# ADR-009 — Politique de journalisation applicative

- **Statut** : Proposé (dette SEC-08 identifiée, à traiter avant staging)
- **Date** : 2026-07-31
- **Exigences concernées** : EX-NF-06 (implicite, observabilité), SEC-08 (revue sécurité SDD §9)

## Contexte

Le SRS ne consigne pas d'exigence chiffrée d'observabilité au niveau
« logs » ; la charte anti-patterns architecturaux exige néanmoins des
**logs structurés avec corrélation** — illisibles autrement en production
distribuée, même sur un déployable unique où plusieurs threads traitent
concurremment des requêtes. La revue sécurité (SEC-08) a classé l'absence
de politique de journalisation comme **dette à lever avant staging** : sans
politique documentée, chaque contributeur invente son format, ses niveaux
et son contenu, avec deux risques concrets :

- **Fuite de PII ou de secrets** dans un log (mot de passe rejeté, token
  d'API partiellement affiché, dump d'un objet contenant un identifiant
  personnel). Un incident de ce type coûte en remédiation (rotation,
  purge des logs archivés) et en conformité.
- **Débogage impossible** en cas d'incident : une trace stack sans
  identifiant de requête, sans identifiant de trace distribuée, sans
  contexte métier (code parcelle, ID relevé) oblige à corréler à la main
  entre plusieurs logs — coût en temps par incident majeur.

Le SDD s'appuiera dès staging sur la journalisation pour :
- diagnostiquer les échecs de traitement d'events (ADR-008) ;
- tracer les purges du journal et du registry (ADR-007) ;
- observer la latence P95 (EX-NF-01) via corrélation logs / métriques.

Sans politique explicite, ces trois usages seront implémentés incohéremment
et coûteront à refactorer plus tard. Un ADR **avant staging** est le bon
moment.

Quatre angles de décision :

1. **Format** : texte plat (lisible en `tail`, illisible en agrégateur)
   ou JSON structuré (verbeux mais exploitable par Elastic/Loki/CloudWatch).
2. **Corrélation** : identifiant propagé entre couches (`traceId`,
   `spanId`) — indispensable dès qu'un event ou un appel asynchrone
   traverse la frontière d'un module.
3. **Niveaux** : convention explicite sur quoi loguer en `INFO`, `WARN`,
   `ERROR`, `DEBUG` — sans quoi la sévérité perçue diverge d'un
   développeur à l'autre.
4. **Contenu interdit** : liste noire des données à ne jamais journaliser.

## Décision

- **Format** : **JSON structuré**, produit par Logback avec
  `logstash-logback-encoder` (encodeur JSON éprouvé, sans dépendance à un
  agrégateur particulier). Chaque ligne de log est un document JSON à plat,
  avec au minimum les champs :
  - `timestamp` (ISO 8601 UTC),
  - `level` (INFO/WARN/ERROR/DEBUG),
  - `logger` (nom de la classe émettrice),
  - `thread`,
  - `message`,
  - `traceId`, `spanId` (voir corrélation ci-dessous),
  - champs métier ad hoc (`parcelleCode`, `releveId`, `eventId`) en
    valeurs typées, pas en concaténation dans `message`.
- **Corrélation** : **Micrometer Tracing** avec propagation **W3C
  Traceparent** (standard `traceparent` HTTP header, format
  `traceId`/`spanId`). Chaque requête entrante génère un `traceId` si
  absent, propagé sur toute la chaîne (contrôleur → use case → repository
  → listener d'event si publié dans la même requête). Un log émis en
  dehors d'une requête (tâche `@Scheduled`, listener de rejeu) génère son
  propre `traceId` de racine.
- **Niveaux** :
  - **INFO** : événements métier normaux dignes d'un audit léger
    (« relevé enregistré », « statut basculé EN_ALERTE », « export
    demandé 4 500 lignes »). Un utilisateur qui lit les INFO reconstitue
    le flux fonctionnel.
  - **WARN** : règle métier violée par une entrée acceptée (par ex.
    tentative de saisir un code déjà existant → 409, à ne pas confondre
    avec un bug), ou incident opérationnel dégradé (event en tentative
    N-1 de N, cf. ADR-008).
  - **ERROR** : exception non prévue (5xx renvoyé), bascule d'un event en
    `FAILED`, échec d'une purge, incohérence détectée par
    `ApplicationModules.verify()` en démarrage.
  - **DEBUG** : détails d'implémentation (requêtes SQL générées, contenu
    d'un DTO, payload complet d'un event). **Désactivé en production**
    par configuration (`logging.level.root=INFO`).
- **Contenu interdit** — liste noire, invariante :
  - Mots de passe, tokens, clés d'API (même partiellement masqués :
    interdit).
  - Données personnelles brutes. En v1, aucune donnée personnelle n'est
    stockée (code parcelle et localité sont métier, pas PII). Cette
    interdiction est **préventive** pour la v2 : si un identifiant
    utilisateur, un email ou un nom sont ajoutés, ils ne passent pas dans
    les logs sans anonymisation.
  - Contenu complet d'un CSV importé (peut contenir des données non
    validées, en volume).
  - Stack traces sur `WARN` ou `INFO` (réservées à `ERROR`).
- **Rétention des logs** : hors périmètre v1, relève de la plateforme
  d'exploitation (ELK/Loki/CloudWatch). Le format JSON garantit la
  compatibilité avec les principaux agrégateurs.
- **Cohérence avec RFC 7807** : les logs `ERROR` peuvent contenir des
  détails internes (SQL, chemins, stack) ; la **réponse HTTP** au client
  ne les expose jamais (EX-NF-05). Cette asymétrie est explicite : logger
  détaillé, répondre sobrement.

## Conséquences

**Positives**
- SEC-08 refermée : politique explicite, testable en revue de PR
  (grep pour `System.out`, `printStackTrace`, `password`, `token` dans
  les logs).
- Débogage traçable : un incident se remonte à partir du `traceId` du
  ticket ou de la métrique jusqu'à l'ensemble des logs qui l'ont produit,
  y compris ceux émis par le listener d'event (asynchrone) — condition
  indispensable pour opérer ADR-008 sereinement.
- Compatible avec tout agrégateur JSON standard (ELK, Loki, CloudWatch
  Logs Insights, Datadog) sans transformation.
- Cadre commun pour ADR-007 (logs de purge) et ADR-008 (logs de rejeu
  d'events) — décisions cohérentes plutôt que trois formats ad hoc.
- Prépare l'ajout d'authentification (arbitrage n°1 du SRS) sans dette
  logs : la liste noire couvre déjà les futures données sensibles.

**Négatives**
- **Verbosité** en développement : JSON structuré est peu lisible en
  `tail -f`. À mitiger par un **profil de log dev** (Logback pattern
  console classique en profil `dev`, JSON en profil `staging`/`prod`).
- Coût CPU marginal de l'encodage JSON par rapport au texte plat ;
  négligeable à la volumétrie H3 (≤ 20 utilisateurs simultanés).
- Discipline requise : ne pas empiler du contexte métier dans `message`
  par facilité (« relevé du parcelle X avec taux Y ») alors que le
  champ typé serait plus exploitable. À faire respecter en revue.
- La liste noire est **déclarative** : elle n'empêche pas techniquement
  un contributeur de logger un mot de passe. Une passe de linter
  (règle SonarQube ou grep en CI sur les mots-clés interdits) est à
  ajouter en dette suivante.

**Neutres**
- Le choix de `logstash-logback-encoder` est spécifique à Java/Logback
  ; il n'engage pas le frontend Next.js, qui suit sa propre politique
  de logs (hors périmètre de cet ADR).

## Alternatives rejetées

- **Logs texte non structurés** (pattern Logback classique) : rejetés.
  Illisibles agrégés en production, impossibles à filtrer par champ
  (`parcelleCode:P-042`), obligent à parser à la volée. Acceptable en
  dev seul, d'où le profil dédié.
- **Corrélation absente** (`traceId` non propagé) : rejetée. Un event
  asynchrone (ADR-003) traité par un thread différent perd tout lien
  avec la requête qui l'a produit. Debugging distribué impossible dès
  qu'un asynchrone entre en jeu.
- **Interception logging côté framework uniquement** (par ex.
  auto-instrumentation Spring Boot Actuator sans log métier explicite) :
  rejetée. Capture les frontières (requêtes HTTP entrantes/sortantes)
  mais **perd le contexte métier** : impossible de retrouver
  « pourquoi cette parcelle est-elle passée en alerte à 10h32 ». Les
  logs métier explicites au niveau du use case restent indispensables.
- **Solution APM propriétaire** (Datadog APM, New Relic) sans logs
  structurés côté application : rejetée pour v1 (coût, dépendance
  externe non nécessaire à la volumétrie H3, engagement fournisseur
  précoce). Le format JSON standard laisse la porte ouverte à
  l'exportation vers un APM sans réécriture.

## Références

- EX-NF-06 (observabilité implicite), EX-NF-05 (format d'erreur RFC 7807)
  du SRS.
- SDD §8 (observabilité), §9 revue sécurité (SEC-08).
- ADR-007 (rétention et purges) — les logs de purge suivent ce format.
- ADR-008 (DLQ et rejeu d'events) — les logs de bascule `FAILED` et de
  rejeu manuel suivent ce format.
- W3C Trace Context (spécification `traceparent`).
- 12-factor app, §XI (Logs).
