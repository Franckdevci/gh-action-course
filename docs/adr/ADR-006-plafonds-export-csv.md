# ADR-006 — Plafonds et streaming de l'export CSV

- **Statut** : Accepté
- **Date** : 2026-07-31
- **Exigences concernées** : EX-F-04, EX-NF-01, SEC-03 (revue sécurité SDD §9)

## Contexte

EX-F-04 impose un export CSV de l'ensemble des parcelles avec leur dernier
taux, leur statut et la date du dernier relevé. À la volumétrie H3 du SRS
(≤ 5 000 parcelles), un export « nominal » représente 5 000 lignes ; rien
n'empêche cependant, en pratique ou en test, une accumulation qui déborde ce
cadre (imports en masse, jeux de données de charge, croissance non
anticipée). Sans borne explicite, l'endpoint devient un vecteur de **DoS
pratique** : une requête peut charger l'intégralité de la table en mémoire,
saturer le heap, bloquer les autres appels et faire chuter le P95 exigé par
EX-NF-01 (< 500 ms sur la liste paginée) pour tous les utilisateurs.

La revue sécurité (SEC-03) a explicitement noté l'absence de borne comme
dette bloquante avant staging. La décision porte sur trois axes indissociables :

1. **Un plafond en nombre de lignes** exportables en une seule requête.
2. **Une stratégie de streaming** évitant de matérialiser l'intégralité du
   jeu de données en mémoire côté API.
3. **Un code d'erreur explicite** au-delà du plafond, aligné sur le format
   RFC 7807 déjà retenu (EX-NF-05).

Quatre options ont été étudiées :

1. **Export bloc unique en mémoire** (`List<Parcelle>` chargée puis sérialisée) :
   simple, mais viole la contrainte d'empreinte mémoire dès quelques milliers
   de lignes ; incompatible avec le streaming et le chunking Tomcat.
2. **Export asynchrone avec notification** (job en arrière-plan, fichier
   déposé, URL envoyée ou pollée) : robuste pour de très grands volumes,
   mais introduit une infrastructure disproportionnée (queue, stockage
   temporaire, cycle de vie du fichier, sécurité de l'URL), sans justification
   à l'échelle H3.
3. **Pagination `?page=` sur l'export** : déplace la responsabilité vers le
   client, qui doit reconstituer le CSV — non conforme à l'esprit d'EX-F-04
   (« un » export unitaire), et fragile (deep pagination `offset` interdite
   par la charte).
4. **Plafond dur + streaming cursor + 413 au-delà** : borne l'appel, protège
   la mémoire, refuse explicitement le dépassement.

## Décision

Nous retenons l'option 4, matérialisée par les paramètres suivants :

- **Plafond de lignes** : `ecotrack.export.max-lignes = 10 000`, soit environ
  deux fois la volumétrie H3 (5 000 parcelles). Marge délibérée pour absorber
  un dépassement transitoire sans déclencher d'erreur en exploitation normale,
  sans pour autant autoriser un ordre de grandeur au-dessus de la cible.
- **Lot de streaming** : lecture cursor JDBC avec `fetchSize = 500`. Chaque
  lot est écrit dans le flux HTTP, aucune matérialisation intermédiaire de la
  totalité du résultat.
- **Transport HTTP** : `StreamingResponseBody` côté Spring MVC, en
  `Content-Type: text/csv; charset=UTF-8` et `Transfer-Encoding: chunked`.
  L'API commence à répondre dès le premier lot.
- **Refus au-delà du plafond** : si le décompte préliminaire (requête
  `SELECT COUNT(*)` sur la même clause) dépasse `max-lignes`, l'endpoint
  répond **`413 Payload Too Large`** au format RFC 7807, avec le décompte
  effectif et le plafond dans le corps `problem+json`, sans exposer aucun
  détail interne (SQL, nom de table, path). Message type :

  ```
  {
    "type": "https://ecotrack.ci/errors/export-trop-volumineux",
    "title": "Export au-dessus du plafond",
    "status": 413,
    "detail": "L'export demande N lignes, plafond 10000.",
    "instance": "/api/v1/parcelles/export"
  }
  ```

Le plafond et le `fetchSize` sont exposés en propriétés Spring, versionnées
dans la configuration, modifiables par redémarrage du conteneur (12-factor,
cohérent avec ADR-004).

## Conséquences

**Positives**
- EX-NF-01 protégée : l'export ne peut plus faire chuter la latence de la
  liste paginée, l'empreinte mémoire est bornée par la taille d'un lot (500
  lignes) et non par la taille totale du résultat.
- SEC-03 refermée : plus d'endpoint sans borne, dette de sécurité levée
  avant staging.
- Le code de retour `413` communique une **cause précise** au client, plutôt
  qu'un timeout ou un `500` opaque. Le message RFC 7807 est actionable
  (« demandez un filtre »).
- Le streaming permet au client de commencer à traiter le fichier avant la
  fin de l'écriture serveur, ce qui améliore le temps perçu.

**Négatives**
- Un export légitime de plus de 10 000 lignes n'est **pas possible** en un
  seul appel. À la volumétrie H3 c'est théorique ; passé cette échelle il
  faudra soit relever le plafond (décision explicite, versionnée), soit
  ouvrir un ADR pour l'export asynchrone.
- Le décompte préliminaire ajoute une requête `COUNT(*)` avant l'export.
  Coût négligeable à cette volumétrie ; à réévaluer si le filtre devient
  complexe.
- Le streaming complique la gestion d'erreur en cours de flux : une
  exception après le début de la réponse ne peut plus renvoyer un statut
  HTTP différent. Convention : dans ce cas, le flux est coupé et l'erreur
  est journalisée, le client détecte un fichier tronqué (dernière ligne
  incomplète).
- L'ouverture d'un cursor JDBC long implique un maintien de connexion sur
  toute la durée de l'export. À cette volumétrie (< 30 s attendu), pas de
  problème ; borne max de connexions du pool à surveiller.

**Neutres**
- La valeur `10 000` est un paramètre de configuration, pas une constante
  gravée dans le code : la remise en cause ne demande pas un nouvel ADR
  tant que l'ordre de grandeur reste cohérent (2× à 4× la volumétrie H3).

## Alternatives rejetées

- **Export bloc unique en mémoire** : rejeté, empreinte mémoire proportionnelle
  au volume, incompatible avec la promesse P95 d'EX-NF-01 et avec la
  contrainte d'exploitation (heap borné en conteneur).
- **Export asynchrone avec notification** : rejeté, disproportionné à la
  volumétrie H3, introduit une infrastructure (queue, stockage temporaire,
  gestion du cycle de vie du fichier, sécurité de l'URL de téléchargement)
  sans bénéfice mesurable en v1.
- **Pagination `?page=` sur l'export** : rejeté, viole l'esprit d'EX-F-04
  (un export unitaire), fragilise la cohérence (mutations entre deux pages
  produisent un CSV incohérent), et repose sur la deep pagination `offset`
  interdite par la charte anti-patterns.

## Références

- EX-F-04 (export CSV), EX-NF-01 (P95 liste et détail), EX-NF-05 (format
  d'erreur RFC 7807) du SRS.
- SDD §4.3 (contrat d'export) et §9 revue sécurité (SEC-03).
- ADR-002 (pool JDBC, PostgreSQL) — cadre le comportement du cursor.
- ADR-005 (dénormalisation) — l'export lit une seule table, ce qui rend le
  streaming cursor efficace.
- RFC 7807 (Problem Details for HTTP APIs).
