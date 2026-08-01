# Revue de securite adversariale — SDD EcoTrack

**Version** : v2 (audit adversarial)
**Date** : 2026-08-01
**Portee** : docs/sdd.md v1.1 BROUILLON
**Posture** : adversariale — recherche des abus possibles, non validation du document
**Superseeds** : docs/revue-securite-sdd.md v1 (2026-07-29). Les findings SEC-01..SEC-08 v1 encore valides sont repris, marques (v1 confirme) ; ceux clos par une PR/ADR sont marques (v1 clos) sans etre recomptes.

---

## Synthese

- **6 findings BLOQUANT** : SEC-B-01 a SEC-B-06
- **7 findings IMPORTANT** : SEC-I-01 a SEC-I-07
- **6 findings VIGILANCE** : SEC-V-01 a SEC-V-06
- Verdict global : ❌ (le SDD n'est pas defendable en l'etat, six trous exploitables)

Le point structurant reste identique a la v1 : **l'absence d'authentification
(arbitrage SRS n°1) rend inacceptable tout autre relachement**. En posture
adversariale, on constate en outre que la v1.1 a **trace** les corrections
sans les **operationnaliser** : plusieurs affirmations du SDD ne sont pas
tenues par le code deja livre (mismatch SDD ↔ code). C'est l'axe majeur
de cette v2 : **le SDD est trompeur sur ce qui est effectivement en place**,
ce qui reduit d'autant la valeur defensive du document.

Sept surfaces couvertes :
1. Contrat d'API — 4 findings (dont 2 BLOQUANT)
2. Validation des entrees — 3 findings (dont 1 BLOQUANT)
3. Operations non bornees — 3 findings (dont 1 BLOQUANT)
4. Fuite d'information par erreurs et logs — 4 findings (dont 2 BLOQUANT)
5. Exposition Actuator — 2 findings (dont 0 BLOQUANT)
6. Gestion des secrets — 2 findings
7. Retention des donnees — 3 findings

---

## Findings BLOQUANT

### [SEC-B-01] Endpoint `/admin/events/{id}/retry` sans defense demontrable dans le SDD
- **Surface** : contrat d'API + controle d'acces
- **Composant SDD** : le SDD v1.1 §4 (tableau des endpoints) **ne mentionne meme pas** cet endpoint pourtant introduit par ADR-008 (« `POST /admin/events/{id}/retry` — remet un event `FAILED` a l'etat "a rejouer" »).
- **Exigence tracee** : ADR-008, EX-NF-03
- **Description** : ADR-008 impose un endpoint d'administration sensible (relance d'events metier), mais le SDD ne le decrit pas (§4 s'arrete a `/actuator/info`). Aucune contrainte de reseau, de bind, de flag d'activation ni de segregation de port n'est specifiee. Citation ADR-008 : « *Endpoint protege par un mecanisme d'exploitation distinct de l'API metier (hors perimetre de l'auth v1 "aucune", cf. arbitrage n°1 du SRS ; en v1 solo, restriction reseau et non expose publiquement)* ». Cette « restriction reseau » n'est nulle part portee dans le SDD ni dans les manifests decrits.
- **Scenario d'exploitation** : sans authentification (arbitrage n°1) et sans segregation reseau documentee, l'endpoint est joignable via le meme conteneur `api` que les routes publiques ; un attaquant qui atteint l'API peut declencher le rejeu arbitraire d'events `FAILED` — ce qui peut, selon le bug d'origine, entrainer des ecritures multiples au journal, une saturation CPU par boucle de retraitement, ou l'ecrasement d'un etat operationnel.
- **Impact metier** : compromission de l'integrite du journal (EX-F-07) et amplification d'une panne (contradiction avec EX-NF-03).
- **Correction proposee** : (a) ajouter l'endpoint au tableau §4 avec preambule explicite « admin, non expose publiquement » ; (b) le placer sur un **port de management distinct** (`management.server.port`) ; (c) le proteger par flag `ecotrack.admin.enabled=false` par defaut ; (d) verrouiller son bind a `127.0.0.1` en attendant la Phase 10 ; (e) test d'integration verifiant qu'il n'est pas atteignable via le port applicatif.
- **CWE/OWASP** : CWE-306 (Missing Authentication for Critical Function), OWASP API5 (Broken Function Level Authz).

### [SEC-B-02] Handler d'exceptions global inexistant — divergence SDD ↔ code
- **Surface** : fuite d'information par les erreurs (EX-NF-05, SEC-04)
- **Composant SDD** : §4.2 « Un gestionnaire d'exceptions global (`@RestControllerAdvice`) est le **seul point** de production de reponses d'erreur ».
- **Exigence tracee** : EX-NF-05, SEC-04 (marque « TRAITE » au tableau §9)
- **Description** : le SDD affirme que SEC-04 est **TRAITE** en v1.1. La lecture du code montre le contraire :
  - `SharedApiExceptionHandler.java` ne mappe QUE `DonneeInvalideException` (400).
  - `ApiExceptionHandler.java` (dans le module `parcelles`) mappe `MethodArgumentNotValidException`, `DonneeParcelleInvalideException`, `HttpMessageNotReadableException`, `CodeParcelleDejaUtiliseException`.
  - **Aucun handler `@ExceptionHandler(Throwable.class)` ni `Exception.class`** n'est present, ni globalement ni sur les advices existants.
  - **Aucun handler pour `DataIntegrityViolationException`** — la contrainte SDD §4.2 « violation de contrainte connue → 409 detail metier » n'est pas cablee. En cas de course de creation entre deux clients avec le meme code, Spring va exposer le message par defaut de `DefaultHandlerExceptionResolver` qui inclut potentiellement le nom de contrainte SQL `parcelle_code_unique` (V2__parcelle.sql).
  - Les advices sont deux, dans deux modules differents : ordre de resolution non specifie, risque de shadowing.
- **Scenario d'exploitation** : un attaquant declenche une violation d'unicite ou une exception non captee (`NullPointerException` dans un mapper, `PSQLException` en cas de contention) et lit dans la reponse le nom de la contrainte SQL ou la classe Java d'origine, ce qui expose la structure interne du schema. Combine a SEC-B-06, cela devient un vecteur de cartographie.
- **Impact metier** : violation directe d'EX-NF-05, information privilegee sur le schema utilisable pour affiner d'autres attaques.
- **Correction proposee** : (a) fusionner les deux advices en un seul, dans `shared`, avec `@Order` explicite ; (b) ajouter un handler `@ExceptionHandler({Exception.class, Throwable.class})` qui repond 500 avec un ProblemDetail neutre ; (c) ajouter un handler `DataIntegrityViolationException` avec traduction sur le nom de **contrainte connue** (whitelist statique, aucun `getMessage()` propage) ; (d) le test §7.2 n°9 doit etre ecrit **avant** de mettre a jour le statut SEC-04 en « TRAITE » — actuellement le test annonce couvrant n'existe pas dans `api/src/test/`.
- **CWE/OWASP** : CWE-209 (Generation of Error Message Containing Sensitive Information), CWE-215 (Insertion of Sensitive Information into Debugging Code), OWASP A05 (Security Misconfiguration).

### [SEC-B-03] `dernierTaux` en flottant JSON viole l'invariant BigDecimal exact
- **Surface** : contrat d'API + integrite metier
- **Composant SDD** : §4 exemple de liste : `"dernierTaux": 59.9` (nombre JSON) et §4 pied de page « `dernierTaux` est un nombre deja arrondi a une decimale pour l'affichage (59.9) ».
- **Exigence tracee** : EX-F-03, EX-NF-07, invariant CLAUDE.md (« `TauxDeSurvie` est un `BigDecimal` d'echelle 4, jamais un `double`, jamais un pourcentage arrondi »).
- **Description** : le SDD serialise le taux affiche comme un **nombre JSON**, qui sera type `Double` cote Java par Jackson par defaut (Number sans annotation). Or l'invariant fondamental est que la comparaison au seuil est exacte sur BigDecimal echelle 4. Deux consequences :
  1. Serialiser `0.5995` en `Double` puis re-formater c'est reintroduire par la fenetre le `double` interdit — un contributeur qui reprend le champ REST pour le back-office (batch de reporting, futur mobile) hydratera un `Double`.
  2. Cote Next.js, le schema zod annonce `dernierTaux: z.number()` (§5.2) — donc **numeric IEEE-754 cote JS**, ce qui rend definitif l'aller-retour en `double`.
- **Scenario d'exploitation** : ce n'est pas une attaque externe classique, c'est un **piege de conception** qui garantit une regression metier a l'implementation. La v2 « notification email » d'ADR-003 s'abonnera a l'event, mais un futur besoin « moyenne des taux du parc » base sur l'API REST re-additionnera des `double` — les alertes seront alors emises sur base d'un taux moyen non deterministe.
- **Impact metier** : perte de determinisme sur la regle du seuil 60 % (EX-F-03) — le pire type de bug metier, silencieux et non falsifiable en test unitaire d'un consommateur.
- **Correction proposee** : serialiser le taux comme **chaine** (`"dernierTaux": "59.9"`) et documenter que le contrat d'affichage est une chaine formatee ; ou publier deux champs distincts (`tauxExact: "0.5995"` + `tauxAffichage: "60,0 %"`). Interdire au SDD toute serialisation numerique du taux au-dela de l'adapter d'affichage strict. Ajouter un test « le champ dernierTaux ne se deserialise pas en Double sans perte ».
- **CWE/OWASP** : CWE-681 (Incorrect Conversion between Numeric Types), OWASP API3 (Broken Object Property Level Authorization — mass assignment inverse : sur-exposition d'un champ interpretable).

### [SEC-B-04] Deep pagination par `offset` non bornee jusqu'a 10 000 pages
- **Surface** : operations non bornees + performance
- **Composant SDD** : §4.1 « `page` : entier `≥ 0` » sans borne haute. Le code `Pagination.java` fixe `PAGE_MAX = 10_000` mais le SDD ne le documente pas.
- **Exigence tracee** : EX-NF-01, EX-NF-02, SEC-02 (partie deep pagination absente).
- **Description** : SEC-02 v1 a ete traite pour `size` (≤ 100). L'axe **`page` non borne** a ete ignore. Avec `size=100` et `page=10 000`, PostgreSQL doit trier et sauter **1 000 000 lignes** avant de commencer a produire la reponse — meme si le parc reel ne compte que 5 000 parcelles. Le code va meme jusqu'a autoriser `PAGE_MAX = 10_000` (soit `offset = 1 000 000` sur le tri `alerte d'abord, code croissant` qui **n'a pas d'index specifique** au-dela de `parcelle(statut, code)` decrit §3.4 : le scan est indexe, mais le decompte `OFFSET` reste couteux). Le SDD n'indique aucune strategie *keyset pagination* pour les endpoints exposes.
- **Scenario d'exploitation** : un attaquant emet quelques requetes concurrentes vers `/parcelles?page=10000&size=100` et `/alertes?page=10000&size=100`. Chaque requete engage une pagination profonde, sature le pool JDBC (§ADR-002 : `pool` non dimensionne dans le SDD), degrade le P95 exige par EX-NF-01. Sans authentification, cout d'attaque quasi nul.
- **Impact metier** : violation d'EX-NF-01 et EX-NF-02 par saturation du pool et de la CPU DB, sans crash direct — plus insidieux qu'un OOM.
- **Correction proposee** : (a) le SDD documente `page ∈ [0, 200]` (≤ 20 000 parcelles = 4× H3) ; (b) ajoute une strategie *keyset pagination* pour tous les endpoints exposant `page > 100` ; (c) ajoute une note « au-dela d'un certain rang, la reponse rappelle le total et propose un lien vers l'export CSV plutot que de continuer a pagıner » (redirection metier de l'offset lointain).
- **CWE/OWASP** : CWE-770 (Allocation of Resources Without Limits), OWASP API4 (Unrestricted Resource Consumption).

### [SEC-B-05] Adapter CSV inexistant mais annonce comme « TRAITE » (SEC-01)
- **Surface** : validation des entrees / sorties + fuite d'information
- **Composant SDD** : §4.3 « L'adapter d'export CSV applique la regle suivante a chaque valeur de cellule avant ecriture, sans exception » + §9 « SEC-01 — TRAITE ».
- **Exigence tracee** : SEC-01 (v1 confirme), EX-F-04
- **Description** : le SDD affirme que SEC-01 est traite « §4.3 + test 7 de §7.2 ». Verification code : aucun package `export`, aucun `ExportController`, aucune classe `CsvEscaper`, aucun test `should_neutraliser_formule_when_localite_commence_par_egal`. Le domaine `Localite` (Localite.java) ne rejette PAS un champ commencant par `=`, `+`, `-`, `@` — ce qui est *correct* (la charte SDD dit « la securite de sortie n'est pas une contrainte d'entree »), mais **aucun adapter de sortie ne remplit ce role**. La contre-mesure existe uniquement dans le document, pas dans le systeme.
- **Scenario d'exploitation** : identique v1 — creation d'une parcelle avec `localite = "=HYPERLINK(...)"`. A l'implementation de l'endpoint d'export sans reprise de la specification §4.3, la vulnerabilite est presente des le premier deploiement.
- **Impact metier** : voir SEC-01 v1 (exfiltration a l'ouverture Excel).
- **Correction proposee** : (a) le tableau §9 doit distinguer « traite dans le SDD » et « traite dans le code » — actuellement l'ambiguite du terme « TRAITE » induit en erreur ; (b) l'implementation de l'export EX-F-04 doit livrer, dans la meme PR, l'adapter d'echappement ET le test 7 §7.2 (contrainte de definition de fini) ; (c) le test 7 doit inclure les 6 caracteres declencheurs et non le seul `=`.
- **CWE/OWASP** : CWE-1236 (Improper Neutralization of Formula Elements in a CSV File).

### [SEC-B-06] Absence de contrainte de longueur/charset a la frontiere REST
- **Surface** : validation des entrees + operations non bornees
- **Composant SDD** : §3.2 tableau des VO : `Localite` ≤ 100 caracteres. §4 aucun `Content-Length` maximum, aucun encodage impose.
- **Exigence tracee** : EX-NF-05, EX-F-01 R4, SEC-05 (annonce en dette mais son perimetre est plus large que ce que le SDD dit)
- **Description** : la contrainte 100 caracteres est verifiee par `@Size(max = 100)` sur `CreerParcelleRequest.localite`, mais :
  1. **`Localite` du domaine fait `.trim()` avant validation** (`Localite.java` L11) — un attaquant peut envoyer 100 caracteres + espaces autour et passer la validation Bean mais aussi ecrire 100 caracteres normalises. Comportement plus permissif qu'annonce.
  2. Aucune protection contre les **caracteres de controle** (` ..` sauf `\t` `\n` `\r`), les **null bytes**, les **caracteres directionnels Unicode** (U+202E RTL Override utilise en attaque phishing sur nom de fichier). Un attaquant peut injecter du null byte dans `localite` puis un log naif le tronquerait (log injection).
  3. Aucun `spring.servlet.multipart.max-request-size`, `server.max-http-request-header-size`, ni `spring.mvc.async.request-timeout` documente ni configure. Un attaquant envoie un body JSON gigantesque (200 MB de chaines), Jackson deserialise en mémoire avant que la validation Bean s'applique.
- **Scenario d'exploitation** : (a) log injection par null byte (compromet l'auditabilite SEC-08) ; (b) DoS par corps enorme sur `POST /parcelles` sans authentification.
- **Impact metier** : SEC-08 (integrite des logs) et EX-NF-02 (dispo).
- **Correction proposee** : (a) le SDD §3.2 doit expliciter la classe de caracteres autorisee (`\p{L}\p{N}\p{P}\p{Z}` sauf controle) ; (b) documenter en §6 `server.tomcat.max-http-form-post-size` et `spring.codec.max-in-memory-size` ; (c) rendre `Localite` `.trim()` conforme a la borne : le trim doit se faire AVANT validation de longueur, pas apres construction. Le code actuel valide APRES trim, ce qui expose les 100 caracteres apres nettoyage — a documenter au SDD, ou a corriger.
- **CWE/OWASP** : CWE-20 (Improper Input Validation), CWE-117 (Improper Output Neutralization for Logs), CWE-770 (Allocation of Resources Without Limits).

---

## Findings IMPORTANT

### [SEC-I-01] Endpoint `GET /api/v1/config` expose sans specification de contenu
- **Surface** : contrat d'API + fuite d'information
- **Composant SDD** : §5.3 « le front interroge `GET /api/v1/config` qui expose les flags publics ».
- **Exigence tracee** : ADR-004, C2 SRS
- **Description** : le SDD ne definit pas le corps de cette reponse. Un contributeur qui expose « les flags publics » peut par erreur inclure d'autres proprietes `ecotrack.*` (`retention.journal-alertes-mois`, `export.max-lignes`, `admin.enabled`) via un mapping paresseux sur `@ConfigurationProperties`. L'attaquant lit alors la configuration operationnelle.
- **Scenario d'exploitation** : deserialisation JSON de la reponse `/config`, extraction de champs revelateurs (activation d'un flag admin, seuils operationnels).
- **Impact metier** : cartographie de la configuration operationnelle, aide a l'exploitation d'autres findings.
- **Correction proposee** : le SDD §5.3 doit specifier une **whitelist explicite** des cles renvoyees (nom + type) et interdire le renvoi par introspection d'un objet ConfigurationProperties complet.
- **CWE/OWASP** : CWE-200 (Exposure of Sensitive Information), OWASP A05.

### [SEC-I-02] Actuator `/health` expose sans distinction readiness/liveness/details
- **Surface** : exposition Actuator
- **Composant SDD** : §6 « Sondes readiness/liveness » et §4 endpoint `/actuator/health` sans qualificatif.
- **Exigence tracee** : EX-NF-02, EX-NF-04
- **Description** : la configuration `application.yml` expose `health,info` sans `management.endpoint.health.show-details` explicite. Par defaut Spring Boot expose `always` en profil `dev` et `when-authorized` en presence de securite — mais **il n'y a pas de securite** (arbitrage n°1). Consequence probable : `show-details: never` par defaut suffit, mais aucun test ni configuration ne le garantit ; un contributeur peut passer a `always` pour debug et laisser en place. En outre `/actuator/health/readiness` et `/actuator/health/liveness` ne sont pas explicitement exposes ; les sondes Kubernetes doivent viser ces sous-endpoints (EX-NF-02).
- **Scenario d'exploitation** : reponse `/actuator/health` avec details = enumeration des composants (dataSource, diskSpace, ping, db.url masque mais parfois version SGBD lisible via `db.database`).
- **Impact metier** : fuite d'info sur infra (EX-NF-05).
- **Correction proposee** : SDD §6 ajoute une ligne `management.endpoint.health.show-details: never` + `probes.enabled: true` + `web.exposure.include: health,info,prometheus` (Prometheus si utilise par ADR-007/008 metriques) — et rien d'autre. Tests d'integration verifiant que `/actuator/env`, `/beans`, `/mappings`, `/heapdump`, `/threaddump`, `/loggers` repondent 404.
- **CWE/OWASP** : CWE-16 (Configuration), OWASP A05.

### [SEC-I-03] Metriques Micrometer sans expose ni protection
- **Surface** : exposition Actuator + information
- **Composant SDD** : §3.5 « une metrique `ecotrack.events.publications_non_traitees` est exposee via `/actuator` » ; ADR-007 idem ; ADR-008 `events_failed_total`, `events_attempts_histogram`.
- **Exigence tracee** : ADR-007, ADR-008
- **Description** : le SDD annonce une metrique exposee mais `application.yml` n'inclut PAS `prometheus` ou `metrics` dans `web.exposure.include`. Contradiction. En corollaire, si un contributeur active `metrics` pour repondre a la specification, il expose alors publiquement l'ensemble des metriques Spring : `jvm.memory.used`, `http.server.requests` (avec noms de routes), taux d'erreur — ce qui est un vecteur classique de reconnaissance.
- **Scenario d'exploitation** : moisson de metriques → cartographie des endpoints reels, detection des pics d'usage, deduction d'evenements internes (ex. augmentation soudaine de `events_failed_total`).
- **Impact metier** : information tactique pour un attaquant (recon).
- **Correction proposee** : SDD §6 explicite que les metriques sont **exposees sur `management.server.port` distinct**, bindees `127.0.0.1`, scrappees par un sidecar Prometheus (ou equivalent) en reseau interne. Aucune exposition publique. Pas de `/actuator/prometheus` sur le port applicatif.
- **CWE/OWASP** : CWE-200, OWASP A05.

### [SEC-I-04] Reception non specifiee des headers de tracing (traceparent)
- **Surface** : validation des entrees + integrite des logs
- **Composant SDD** : ADR-009 « Micrometer Tracing avec propagation W3C Traceparent. Chaque requete entrante genere un `traceId` si absent, propage sur toute la chaine ».
- **Exigence tracee** : ADR-009, EX-NF-06
- **Description** : accepter un `traceparent` fourni par le client sans validation ni filtrage permet a un attaquant de **fabriquer un traceId** pour se fondre dans une trace existante ou pour polluer l'observabilite. En corollaire, aucune limite de longueur sur le header `traceparent` n'est specifiee — log injection potentielle sur agregation Loki/ELK.
- **Scenario d'exploitation** : un attaquant emet des requetes avec `traceparent` reproduisant celui d'un incident interne connu, brouillant l'investigation ; ou en envoie de tres long, provoquant un log oversize.
- **Impact metier** : perte d'auditabilite (SEC-08), obstruction de l'investigation d'incident.
- **Correction proposee** : SDD ADR-009 doit expliciter (a) validation du format `traceparent` (regex W3C stricte) avant adoption ; (b) si header absent OU invalide, **le serveur genere un traceId** et ignore le header ; (c) trace-context propage vers l'aval **seulement si l'appel est sortant interne** — en v1 il n'y a pas d'appel sortant, donc le header n'est jamais propage vers l'exterieur.
- **CWE/OWASP** : CWE-117 (Log Injection), CWE-20.

### [SEC-I-05] Corps de reponse d'erreur inclut `champs[]` pouvant refleter l'entree
- **Surface** : fuite d'information par erreur
- **Composant SDD** : §4 exemple « `champs: [ { champ: "code", message: "format invalide" } ]` » + code `ApiExceptionHandler.traiterValidation` qui propage `fe.getField()` et `fe.getDefaultMessage()`.
- **Exigence tracee** : EX-NF-05
- **Description** : renvoyer le nom du champ invalide est acceptable ; renvoyer `fe.getDefaultMessage()` sans whitelist peut inclure la valeur rejetee (Bean Validation `{value}`). Actuellement les messages sont statiques dans `CreerParcelleRequest.java` — mais rien dans le SDD n'interdit d'utiliser des templates Bean Validation par defaut (`javax.validation.constraints.Size.message = must be between {min} and {max}`) qui reveleraient au client les bornes internes. Pire : un contributeur peut inclure `${validatedValue}` dans un message custom, ce qui echo la valeur envoyee dans la reponse — vecteur de reflexion.
- **Scenario d'exploitation** : XSS-en-reflexion si la reponse est un jour consommee par un client HTML naif (aucun en v1, mais le contrat est public — ADR-004).
- **Impact metier** : fuite conditionnelle + risque XSS reflete futur.
- **Correction proposee** : SDD §4.2 doit expliciter (a) messages metier **statiques** whitelistes (pas de `{value}`, pas de `${validatedValue}`) ; (b) test qui envoie `<script>` dans `code`, `localite` et verifie qu'il n'apparait NULLE PART dans la reponse.
- **CWE/OWASP** : CWE-79 (XSS - reflected), CWE-209.

### [SEC-I-06] Purge automatique du journal contredit `EN` sur la fenetre d'evasion
- **Surface** : retention des donnees + integrite audit
- **Composant SDD** : §3.5 « purge automatique quotidienne » ; ADR-007 « rejet EX-F-07 R1 lecture stricte » toujours **statut Propose**.
- **Exigence tracee** : EX-F-07 R1 (v1 confirme SEC-06 marque « TRAITE » — mais la contradiction subsiste)
- **Description** : contradiction connue de v1 (SEC-06). Le SDD v1.1 la marque « TRAITE » alors qu'ADR-007 est **Propose, en attente de validation sponsor**. Marquage trompeur. Pire : la propriete `ecotrack.retention.journal-alertes-mois` peut etre positionnee a une valeur negative ou nulle (aucune borne dans le SDD), auquel cas la purge devient une suppression immediate — vecteur destructif si un attaquant obtient le controle d'une variable d'env.
- **Scenario d'exploitation** : compromission d'une variable d'env (Kubernetes ConfigMap) fixant `ecotrack.retention.journal-alertes-mois=0` → suppression complete du journal a la premiere execution scheduled → destruction de la piste d'audit avant enquete.
- **Impact metier** : perte definitive de la piste d'audit metier (EX-F-07).
- **Correction proposee** : (a) SDD §9 doit repasser SEC-06 en « EN DETTE » tant qu'ADR-007 est Propose ; (b) borne inferieure sur `retention-mois` (≥ 12 par exemple) verifiee au demarrage, echec de boot si hors bornes ; (c) log INFO au demarrage rappelant la valeur effective.
- **CWE/OWASP** : CWE-1284 (Improper Validation of Specified Quantity in Input), OWASP A08 (Software and Data Integrity Failures).

### [SEC-I-07] Absence de test/verification pour SEC-04 (test §7.2 n°9) — inventaire declaratif
- **Surface** : fuite d'information + gouvernance
- **Composant SDD** : §7.2 « 9. `should_ne_pas_exposer_schema_when_violation_contrainte` ».
- **Exigence tracee** : SEC-04 (v1 confirme)
- **Description** : le test declare dans le SDD est **absent du code** (`api/src/test/` ne le contient pas). Pattern general : le SDD annonce des tests non negociables qui ne sont pas ecrits, tout en marquant les items « TRAITE ». Meme observation pour tests n°1, 2, 3, 4, 5, 6, 7, 8 — seul le n°2 (« 60% exact ») et le n°1 (« 59.95% ») semblent partiellement couverts par `TauxDeSurvieTest.java` (a verifier ; le fichier existe mais SDD requiert un scenario complet, pas un test de VO).
- **Scenario d'exploitation** : ce n'est pas une exploitation directe. C'est le **defaut de defense en profondeur** : sans test, les regles §4.2 ne sont pas verrouillees, un refactor peut casser silencieusement le contrat.
- **Impact metier** : SEC-04 reste ouvert de fait.
- **Correction proposee** : refuser de marquer un SEC « TRAITE » dans le SDD §9 tant que le test correspondant n'est pas dans `api/src/test/` et vert en CI. Introduire une ligne « Testee dans PR#... » dans le tableau §9.
- **CWE/OWASP** : CWE-1053 (Missing Documentation for Design), gouvernance.

---

## Findings VIGILANCE

### [SEC-V-01] Chaine de confiance proxy Next.js `web` → `api` (v1 confirme SEC-09)
- **Surface** : contrat d'API + reseau
- **Composant SDD** : §1.2 « le navigateur ne parle qu'au conteneur `web` ».
- **Description** : rappel v1 — l'API ne doit **jamais** etre directement joignable en production hors reseau interne. Le SDD ne mentionne PAS de NetworkPolicy Kubernetes ni de garantie ingress. La note SEC-09 v1 (`SDD staging expose le port 8080 pour tests RestAssured`) doit rester un rappel explicite dans la v1.1 sous forme de check-list Phase 10, ce qu'elle n'est plus.
- **Scenario d'exploitation** : oubli de manifest Phase 10 → `api` joignable directement, tous les findings ci-dessus deviennent exploitables.
- **Correction proposee** : SDD §6 doit contenir une check-list « Phase 10 : NetworkPolicy `api` interdit ingress externe ».
- **CWE/OWASP** : CWE-668 (Exposure of Resource to Wrong Sphere).

### [SEC-V-02] React `dangerouslySetInnerHTML` (v1 confirme SEC-10)
- **Surface** : rendu frontend
- **Description** : rappel v1 — pas d'insertion HTML brute a partir de donnees saisies. Aucune indication dans le SDD v1.1 que la garde est ecrite (eslint rule `react/no-danger`).
- **Correction proposee** : SDD §5 ajoute « eslint `react/no-danger` en erreur, verifie en CI web ».
- **CWE/OWASP** : CWE-79.

### [SEC-V-03] Images conteneur non privilegiees (v1 confirme SEC-11)
- **Surface** : chaine d'approvisionnement
- **Description** : rappel v1 — utilisateur non-root, images minimales, scan Trivy bloquant. Confirme pour `api` (Trivy fs scan branche protection). Pas mentionne pour `web` dans le SDD v1.1. A ne pas oublier a la construction de l'image Next.js.
- **Correction proposee** : SDD §6 mentionne explicitement les deux images.

### [SEC-V-04] Denormalisation `dernierTaux` sur `parcelle` : integrite transactionnelle
- **Surface** : integrite metier
- **Composant SDD** : §3.4 « denormalisees sur la ligne parcelle, mis a jour par le meme use case ».
- **Description** : la denormalisation est faite dans la meme transaction que le relevé — bien. Mais aucune specification n'evite l'incoherence si un contributeur ajoute plus tard un endpoint `PATCH /parcelles/{code}` (nom, localite) qui touche a la ligne mais oublie le champ `dernier_taux`. Aucune contrainte declarative n'empeche la desync (ex. trigger, ou vue materialisee).
- **Scenario d'exploitation** : indirect — un futur PATCH qui reecrit toute la ligne peut annuler la valeur denormalisee, faisant reapparaitre l'affichage `—` sur une parcelle qui a des releves.
- **Correction proposee** : SDD §3.4 documente une **regle de code review** ou un test d'invariant : « toute PR touchant la ligne parcelle doit preserver `dernier_taux` et `date_dernier_releve` OU les recalculer ». Envisager une vue materialisee lecture seule comme alternative testable.
- **CWE/OWASP** : CWE-353 (Missing Support for Integrity Check).

### [SEC-V-05] Deserialisation Jackson : polymorphisme non desactive explicitement
- **Surface** : validation des entrees
- **Composant SDD** : aucune ligne sur la configuration Jackson.
- **Description** : Spring Boot 3 desactive par defaut le typing polymorphique (Jackson 2.10+), mais aucune ligne du SDD ne l'interdit. Un contributeur qui ajoute `@JsonTypeInfo` sur un DTO sans reflexion ouvre une classe entiere de deserialisation gadget.
- **Scenario d'exploitation** : hypothetique tant qu'aucun `@JsonTypeInfo` n'est utilise ; a surveiller.
- **Correction proposee** : SDD ajoute une regle « aucun `@JsonTypeInfo`, aucun `ObjectMapper.enableDefaultTyping` », verifiee en revue.
- **CWE/OWASP** : CWE-502 (Deserialization of Untrusted Data).

### [SEC-V-06] Registry event : rejeu au demarrage sans limite
- **Surface** : disponibilite + retention
- **Composant SDD** : §3.5, ADR-007 « les publications traitees sont purgees apres 7 jours ». ADR-008 « seuil N=5 tentatives ».
- **Description** : au demarrage, Spring Modulith rejoue **tous** les events non traites accumules pendant l'indisponibilite. Si un incident dure 6 jours et genere 100 events par heure (extreme), le redemarrage tente de rejouer 14 400 events en batch — risque de saturation au boot, echec readiness, redemarrages en boucle. ADR-008 borne les tentatives PAR event, pas le volume total au boot.
- **Scenario d'exploitation** : un incident volumetrique combine a un redemarrage rend l'API incapable de passer readiness.
- **Correction proposee** : SDD documente une strategie de **rejeu par lots** au demarrage (batch de 50 events, entre chaque batch verifier la sante) OU une limite dure « rejeu asynchrone en tache scheduled apres boot, readiness independante ».
- **CWE/OWASP** : CWE-770.

---

## Findings v1 clos ou deplaces

- **SEC-01** — Injection formule CSV — **specification SDD §4.3 en place**, mais **implementation absente** (voir SEC-B-05). Statut effectif : ouvert.
- **SEC-02** — `size` non borne — **partiellement clos** (borne `size ≤ 100` cablée dans `Pagination.java`, decrite §4.1). Le sous-cas `page` non borne remonte en SEC-B-04.
- **SEC-03** — Export CSV non pagıne — specification en place §4.3, implementation absente (comme SEC-B-05).
- **SEC-04** — Fuite information erreurs — specification en place §4.2, **implementation deficiente** (voir SEC-B-02).
- **SEC-05** — Rate limiting et taille de corps — statut « EN DETTE » du SDD confirme, mais le SDD ignore le volet `max-http-post-size` (voir SEC-B-06 pour la partie corps enorme).
- **SEC-06** — Retention journal — **contradiction ADR-007 encore Propose** (voir SEC-I-06). Statut effectif : en attente sponsor.
- **SEC-07** — Purge registry — statut « TRAITE » recevable au niveau specification (§3.5) et ADR-007. Reste a implementer et superviser.
- **SEC-08** — Journalisation applicative — statut « EN DETTE » explicite dans le SDD, ADR-009 Propose, ce qui est coherent.

---

## Verdict

❌ **Le SDD v1.1 n'est pas defendable en l'etat.**

Motifs :
1. **Trois affirmations trompeuses** (SEC-B-02, SEC-B-05, SEC-I-07) : le tableau §9 marque « TRAITE » des items dont l'implementation n'existe pas ou est deficiente. Un lecteur du SDD croit le systeme protege alors qu'il ne l'est pas. C'est le pire type de dette : dette invisible.
2. **Un piege de conception** (SEC-B-03) : la serialisation numerique du taux garantit une regression future de l'invariant metier central, en depit de l'insistance repetee de CLAUDE.md et du SDD sur BigDecimal.
3. **Un endpoint admin non decrit** (SEC-B-01) alors qu'ADR-008 l'introduit — le SDD est incomplet.
4. **Une surface d'attaque non bornee** (SEC-B-04, SEC-B-06) sur pagination et taille de requete, dans un contexte sans authentification.

Correction attendue avant Checkpoint 2 :
- SEC-B-01 a SEC-B-06 fixes dans une revision v1.2 du SDD, avec pour SEC-B-02, SEC-B-05 et SEC-I-07 une preuve **code** (tests presents et verts) et non seulement documentaire.
- Le tableau §9 doit distinguer trois etats : « SPEC en place », « IMPL en place », « TEST vert ». Un item n'est « TRAITE » qu'aux trois etats atteints.
- ADR-007 doit basculer « Accepte » ou le SDD doit repasser SEC-06 en dette.

Les findings IMPORTANT sont a corriger dans la v2 du SDD ou par PR de suivi tracees en issue.
Les findings VIGILANCE sont a surveiller pendant l'implementation.

---

*Cette revue est un document de projet ; elle est versionnee et toute modification passe par une Pull Request. Les constats non traites doivent etre traces en dette explicite, jamais silencieusement abandonnes.*
