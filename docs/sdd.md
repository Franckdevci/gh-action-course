# SDD — EcoTrack : Document de Conception Logicielle
**Version 1.1 — statut : BROUILLON (revue sécurité appliquée sauf SEC-05/SEC-08)**
Source de vérité fonctionnelle : `docs/srs.md` v1.2 · Décisions : `docs/adr/` · Conventions : `CLAUDE.md`

> Ce document dit **COMMENT** on réalise ce que le SRS a défini comme **QUOI**.
> Toute décision structurante y est justifiée par une ADR. Aucune exigence ne
> doit rester sans composant ni sans test (cf. matrice §8).

---

## 1. Vue d'ensemble

### 1.1 Contexte (C4 niveau 1)

```mermaid
C4Context
  Person(gestionnaire, "Gestionnaire de reboisement", "Crée les parcelles, saisit les relevés, surveille les alertes")
  System(ecotrack, "EcoTrack", "Suivi des parcelles de reboisement et détection des alertes")
  Rel(gestionnaire, ecotrack, "Consulte et saisit via navigateur", "HTTPS")
```

Aucun système externe en v1 (cf. SRS §2.1) : pas d'intégration SI, pas de
notification sortante, pas d'authentification (arbitrage n°1).

### 1.2 Conteneurs (C4 niveau 2)

```mermaid
C4Container
  Person(gestionnaire, "Gestionnaire")
  Container_Boundary(ecotrack, "EcoTrack") {
    Container(web, "web", "Next.js / TypeScript / Tailwind", "Interface : liste, détail, journal, export")
    Container(api, "api", "Spring Modulith / Java 21", "Règles métier, API REST, statut d'alerte")
    ContainerDb(db, "db", "PostgreSQL", "Parcelles, relevés, journal des alertes")
  }
  Rel(gestionnaire, web, "Navigue", "HTTPS")
  Rel(web, api, "Appelle côté serveur", "HTTP/JSON, réseau interne")
  Rel(api, db, "Lit/écrit", "JDBC")
```

**Décision de routage** : le navigateur ne parle **qu'au conteneur `web`**.
Next.js relaie `/api/*` vers `http://api:8080` côté serveur (variable
`API_INTERNAL_URL`). Conséquences : aucune configuration CORS, aucune URL d'API
exposée au client, et en Phase 10 l'Ingress reprend ce rôle à l'identique.

---

## 2. Architecture backend : Spring Modulith

### 2.1 Découpage en modules

Trois modules applicatifs, un module technique partagé :

```
ci.ecotrack
├── parcelles/          # module : référentiel des parcelles
├── releves/            # module : relevés + calcul du taux + statut
├── alertes/            # module : journal des changements de statut
└── shared/             # types transverses (open module)
```

**ADR-001** justifie Modulith plutôt que microservices ou monolithe en couches.
Le principe structurant : **un module ne connaît d'un autre que son API publique
ou ses events**. `ApplicationModules.verify()` transforme cette règle en test
(§7.1) — elle n'est pas une convention, c'est un check du pipeline.

| Module | Responsabilité | API publique | Cache |
|---|---|---|---|
| `parcelles` | Cycle de vie et référentiel des parcelles, unicité du code, statut courant | `ParcelleService` (création, consultation, changement de statut), `ParcelleId`, `CodeParcelle` | Entités JPA, repository, contrôleurs |
| `releves` | Enregistrement des relevés, calcul du taux de survie, détermination du statut | `ReleveService` (enregistrer, historique) | Entités JPA, règles de calcul internes |
| `alertes` | Journal immuable des changements de statut, consultation | `JournalAlertesService` (consulter) | Entités JPA, écouteur d'events |
| `shared` | `TauxDeSurvie`, `StatutParcelle` — types de valeur partagés, sans logique de module | tout (open module) | — |

### 2.2 Dépendances et events

```mermaid
flowchart LR
  R[releves] -->|API publique| P[parcelles]
  R -.->|event StatutParcelleChange| A[alertes]
  R --> S[shared]
  P --> S
  A --> S
```

- `releves` → `parcelles` : **dépendance directe assumée** (API publique
  uniquement). Un relevé n'a pas de sens sans sa parcelle : il doit vérifier
  son existence et lire son nombre de plants initiaux.
- `releves` → `alertes` : **jamais de dépendance directe**, uniquement l'event
  `StatutParcelleChange`. `alertes` peut disparaître sans casser `releves` —
  et une v2 « notification email » s'abonnera au même event sans toucher au
  module émetteur. **ADR-003**.
- Aucun cycle. `shared` ne dépend de personne.

**L'event** (publié par `releves`, dans son API publique) :

```java
public record StatutParcelleChange(
    ParcelleId parcelleId,
    CodeParcelle code,
    StatutParcelle ancienStatut,
    StatutParcelle nouveauStatut,
    TauxDeSurvie tauxDeclencheur,
    LocalDate dateReleve,
    Instant survenuLe) {}
```

### 2.3 Structure interne d'un module (hexagonal allégé)

```
releves/
├── ReleveService.java              # API PUBLIQUE du module (seul point d'entrée)
├── domaine/                        # Java pur — AUCUN import Spring/JPA
│   ├── Releve.java                 # entité de domaine
│   ├── TauxDeSurvie.java           # VO (dans shared)
│   └── RegleStatutAlerte.java      # règle du seuil (EX-F-03)
├── application/
│   ├── EnregistrerReleveUseCase.java
│   └── port/ReleveRepository.java  # port de sortie (interface)
└── infrastructure/
    ├── rest/ReleveController.java  # adapter entrant
    └── jpa/                        # adapter sortant : entité JPA + mapper
```

Le domaine est testable sans contexte Spring, en millisecondes. Les adapters
sont remplaçables. C'est ce qui rend `EX-F-03` (règle du seuil, cas limites)
vérifiable par des tests unitaires purs.

---

## 3. Modèle de domaine (DDD)

### 3.1 Agrégats

**Agrégat `Parcelle`** (racine, module `parcelles`) — frontière de cohérence :
code unique, caractéristiques immuables, statut courant.

```java
public class Parcelle {
    private final ParcelleId id;
    private final CodeParcelle code;          // VO : format PRC-AAAA-NNN
    private final Localite localite;          // VO : 1..100 caractères
    private final Superficie superficie;      // VO : 0.01..10000, 2 décimales
    private final NombrePlants plantsInitiaux;// VO : entier > 0
    private final LocalDate datePlantation;
    private StatutParcelle statut;            // seul état mutable
}
```

**Agrégat `Releve`** (racine, module `releves`) — référence `Parcelle` **par
identité** (`ParcelleId`), jamais par objet : deux agrégats, deux transactions,
deux modules.

```java
public class Releve {
    private final ReleveId id;
    private final ParcelleId parcelleId;      // référence par ID
    private final LocalDate dateObservation;
    private final NombrePlants plantsVivants;
    private final TauxDeSurvie taux;          // calculé, jamais saisi (EX-NF-07)
}
```

**Agrégat `EntreeJournal`** (racine, module `alertes`) — immuable par
construction (aucun setter, aucune opération de modification exposée), ce qui
réalise EX-F-07 R1 au niveau du modèle et non par convention.

### 3.2 Objets-valeur et invariants

| VO | Invariant | Exigence |
|---|---|---|
| `CodeParcelle` | `^PRC-\d{4}-\d{1,3}$`, rejet à la construction | EX-F-01 R1 |
| `Localite` | non vide, ≤ 100 caractères | EX-F-01 R4 |
| `Superficie` | `BigDecimal` ∈ [0.01, 10000], échelle 2 | EX-F-01 R3 |
| `NombrePlants` | entier > 0 (initial) ou ≥ 0 (vivants) | EX-F-01 R5, EX-F-02 R1 |
| `TauxDeSurvie` | `BigDecimal` **exact**, échelle 4 ; `estCritique()` = `valeur < 0.60` | EX-F-02 R4, EX-F-03 |

> 🔑 **Le point le plus sensible du modèle.** `TauxDeSurvie` est un `BigDecimal`
> d'échelle 4 (1199/2000 = 0.5995), **jamais un `double`, jamais un pourcentage
> arrondi**. La comparaison au seuil se fait sur cette valeur exacte ;
> l'arrondi à une décimale n'existe **que** dans le formatage d'affichage
> (couche REST/UI). C'est ce qui fait qu'une parcelle à 59,95 % passe en alerte
> tout en affichant « 60,0 % » — comportement exigé par le scénario « cas
> limite de l'arrondi » d'EX-F-03. Un `double` ici, et la règle métier devient
> non déterministe.

### 3.3 La règle de statut (cœur métier, EX-F-03)

```java
// domaine pur, testable sans Spring, sans base
public final class RegleStatutAlerte {
    public static StatutParcelle evaluer(List<Releve> releves) {
        return releves.stream()
            .max(comparing(Releve::dateObservation))   // DERNIER PAR DATE, pas par saisie
            .map(r -> r.taux().estCritique() ? EN_ALERTE : EN_SUIVI)
            .orElse(EN_SUIVI);                          // aucun relevé → EN_SUIVI
    }
}
```

Trois exigences réalisées en cinq lignes, et c'est voulu : le tri par
`dateObservation` réalise le scénario du relevé antidaté ; `estCritique()`
(strictement inférieur, valeur exacte) réalise les deux cas limites ;
`orElse(EN_SUIVI)` réalise le cas « parcelle sans relevé » (EX-F-05 R4).

### 3.4 Persistance et mapping

- **Séparation domaine / JPA** : le domaine ignore JPA ; chaque module a ses
  entités `*JpaEntity` et un mapper explicite. Coût : du code de mapping.
  Bénéfice : les invariants ne dépendent pas du cycle de vie d'Hibernate, et
  le domaine reste testable en isolation. **ADR-002**.
- **Schéma géré par Flyway uniquement**, `ddl-auto=validate` sur tous les
  profils. Migrations `V1__creation_schema.sql`, etc. Règle *expand/contract*
  obligatoire (rolling update = deux versions coexistantes, EX-NF-02).
- **Contraintes en base**, pas seulement en Java : `UNIQUE(code)` sur
  `parcelle` (EX-F-01 R2), `UNIQUE(parcelle_id, date_observation)` sur `releve`
  (EX-F-02 R3) — la base est le dernier rempart contre les écritures
  concurrentes que la validation applicative ne voit pas.
- **Index** : `releve(parcelle_id, date_observation DESC)` pour l'historique
  et le dernier relevé ; `parcelle(statut, code)` pour le tri par défaut de la
  liste (EX-F-05 R1, EX-NF-01).

> ⚠️ **Piège de performance à traiter dès la conception** : afficher la liste
> paginée avec le dernier taux de chaque parcelle est un N+1 classique
> (1 requête + 50 requêtes de relevés). Solution retenue : le **dernier taux
> et la date du dernier relevé sont dénormalisés sur la ligne `parcelle`**,
> mis à jour par le même use case qui enregistre le relevé. Justifié par
> EX-NF-01 (P95 < 500 ms sur la liste, jusqu'à 5 000 parcelles).
> Contrepartie assumée : redondance contrôlée, dans une seule transaction.

### 3.5 Rétention et purges (SEC-06, SEC-07)

- **Journal des alertes (EX-F-07)** : rétention de **24 mois** à compter de la
  date d'entrée. Au-delà, une purge automatique (tâche planifiée quotidienne)
  supprime les entrées expirées. La rétention est documentée et son échéance
  contrôlable en configuration (`ecotrack.retention.journal-alertes-mois`,
  défaut : `24`). L'immuabilité (EX-F-07 R1) porte sur la période de rétention :
  aucune modification, seule la purge d'échéance peut retirer une entrée.
- **Event publication registry (ADR-003)** : les publications **traitées**
  sont purgées après 7 jours (tâche planifiée) — leur accumulation n'a pas de
  valeur métier et dégrade les performances. Les publications **non traitées**
  sont conservées sans limite de durée et **supervisées** : une métrique
  `ecotrack.events.publications_non_traitees` est exposée via `/actuator`, et
  toute valeur > 0 déclenche une alerte d'exploitation. Justification : une
  publication non traitée = une entrée manquante au journal des alertes, donc
  une violation silencieuse d'EX-NF-03.

---

## 4. Contrat d'API REST

Contrat faisant foi entre `web` et `api` (EI-02). Base : `/api/v1`.
Erreurs au format **RFC 7807** (`application/problem+json`), sans détail
interne (EX-NF-05).

| Méthode | Ressource | Codes | Exigence |
|---|---|---|---|
| `POST` | `/parcelles` | 201, 400, 409, 500 | EX-F-01 |
| `GET` | `/parcelles?page=0&size=50` | 200, 400, 500 | EX-F-05 |
| `GET` | `/parcelles/{code}` | 200, 404, 500 | EX-F-06 |
| `POST` | `/parcelles/{code}/releves` | 201, 400, 404, 409, 500 | EX-F-02 |
| `GET` | `/parcelles/{code}/releves` | 200, 404, 500 | EX-F-06 |
| `GET` | `/alertes?page=0&size=50` | 200, 400, 500 | EX-F-07 |
| `GET` | `/parcelles/export.csv` | 200, 404 (flag OFF), 413, 500 | EX-F-04 |
| `GET` | `/actuator/health`, `/actuator/info` | 200 | EX-NF-04 |

### 4.1 Pagination : bornes strictes (SEC-02)

Les endpoints paginés (`GET /parcelles`, `GET /alertes`) appliquent les mêmes
bornes, documentées dans le contrat :

| Paramètre | Type | Bornes | Défaut | Hors bornes |
|---|---|---|---|---|
| `page` | entier | `≥ 0` | `0` | `400 Bad Request` |
| `size` | entier | `1..100` | `50` | `400 Bad Request` |

Règles :
- `size > 100` : **rejet explicite en 400**, **jamais** de troncature
  silencieuse à 100 (le contrat serait alors ambigu et le client ne saurait
  pas qu'il a été bridé).
- `page < 0` ou `page` non entier : `400 Bad Request`.
- `size ≤ 0` ou `size` non entier : `400 Bad Request`.
- Justification : sans authentification (arbitrage n°1), tout paramètre non
  borné devient une arme (déni de service par saturation mémoire) — cf.
  SEC-02 de la revue sécurité et impact direct sur EX-NF-02.

**Création d'une parcelle** :

```http
POST /api/v1/parcelles
{ "code": "PRC-2026-042", "localite": "Bingerville",
  "superficie": 12.50, "plantsInitiaux": 2000, "datePlantation": "2026-06-15" }

201 Created
{ "code": "PRC-2026-042", "localite": "Bingerville", "superficie": 12.50,
  "plantsInitiaux": 2000, "datePlantation": "2026-06-15",
  "statut": "EN_SUIVI", "dernierTaux": null, "dateDernierReleve": null }
```

**Liste paginée** (EX-F-05 : tri alertes d'abord puis code croissant) :

```http
GET /api/v1/parcelles?page=0&size=50

200 OK
{ "contenu": [
    { "code": "PRC-2026-043", "localite": "Adzopé", "statut": "EN_ALERTE",
      "dernierTaux": 59.9, "dateDernierReleve": "2026-07-22" },
    { "code": "PRC-2026-042", "localite": "Bingerville", "statut": "EN_SUIVI",
      "dernierTaux": null, "dateDernierReleve": null } ],
  "page": 0, "taille": 50, "total": 2, "totalPages": 1 }
```

> `dernierTaux` est un **nombre déjà arrondi à une décimale pour l'affichage**
> (59.9), tandis que le statut provient de la valeur exacte (0.5995) — c'est
> exactement le cas limite d'EX-F-03 : le contrat assume que taux affiché et
> statut peuvent sembler incohérents. `null` signifie « aucun relevé », jamais 0.

**Erreur de validation** :

```http
400 Bad Request
{ "type": "about:blank", "title": "Requête invalide", "status": 400,
  "detail": "Le code doit respecter le format PRC-AAAA-NNN",
  "champs": [ { "champ": "code", "message": "format invalide" } ] }
```

**Codes 409** : conflit d'unicité — code de parcelle déjà utilisé (EX-F-01 R2),
relevé déjà existant à cette date (EX-F-02 R3).

**Aucun champ `statut` ni `taux` n'est acceptable en écriture** sur les
ressources — vérifié par revue de contrat et par test (EX-NF-07).

### 4.2 Gestion des erreurs (SEC-04)

Un **gestionnaire d'exceptions global** (`@RestControllerAdvice`) est le
**seul point** de production de réponses d'erreur. Il traduit chaque famille
d'exception en une réponse RFC 7807 neutre du point de vue métier.

**Règles de traduction** :

| Origine | Réponse | Contenu |
|---|---|---|
| Violation de contrainte connue (`DataIntegrityViolationException` sur `UNIQUE(code)`) | `409 Conflict` | `detail`: « une parcelle avec ce code existe déjà » |
| Violation de contrainte connue (`UNIQUE(parcelle_id, date_observation)`) | `409 Conflict` | `detail`: « un relevé existe déjà à cette date » |
| Validation d'entrée (VO, `@Valid`, bornes de pagination) | `400 Bad Request` | `detail` métier + `champs[]` (nom du champ + message métier) |
| Ressource inexistante | `404 Not Found` | `detail`: « ressource introuvable » |
| Export refusé au-delà de la limite absolue | `413 Payload Too Large` | `detail`: « export refusé, volume au-delà de la limite » |
| **Toute autre exception non prévue** | `500 Internal Server Error` | `detail`: « erreur interne » — **aucun détail** technique |

**Interdits absolus** dans le corps d'une réponse d'erreur :
- nom de table, de colonne, de contrainte SQL ;
- nom de classe Java, package, méthode ;
- version du SGBD, du framework, du JDK ;
- trace d'exécution, extrait de requête SQL, message brut d'`SQLException`.

**Configuration Spring imposée** (fichier `application.yml`, tous profils) :

```yaml
server:
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
    include-exception: never
    whitelabel:
      enabled: false
```

**Test associé** : `should_ne_pas_exposer_schema_when_violation_contrainte` —
provoque chaque violation de contrainte connue et vérifie qu'aucun nom de
table, de contrainte, de classe ni de version n'apparaît dans la réponse.

### 4.3 Export CSV : échappement et production en flux (SEC-01, SEC-03)

**Échappement anti-injection de formule (SEC-01, adapter d'export uniquement)** :

L'adapter d'export CSV applique la règle suivante à **chaque valeur de cellule**
avant écriture, sans exception :

- Si la valeur (une fois convertie en chaîne) commence par l'un des caractères
  `=`, `+`, `-`, `@`, une tabulation (`\t`) ou un retour chariot (`\r`), elle
  est **préfixée d'une apostrophe** (`'`).
- Tous les guillemets internes (`"`) sont **doublés** (`""`).
- Tous les champs sont **systématiquement encadrés** de guillemets, quel que
  soit leur contenu (pas d'encadrement conditionnel).

Cette règle est une responsabilité **exclusive de l'adapter d'export** :
elle n'existe **jamais** dans le domaine (`Localite` reste un texte libre de
100 caractères, ses invariants ne changent pas — la sécurité de sortie n'est
pas une contrainte d'entrée). Justification : la même valeur peut être
affichée sans échappement dans le HTML (React échappe déjà) mais doit être
échappée à l'export CSV — la règle appartient au canal de sortie concerné.

Test associé : `should_neutraliser_formule_when_localite_commence_par_egal`.

**Production en flux (SEC-03)** :

L'export CSV est produit en **flux** — jamais construit intégralement en
mémoire. Contraintes de conception :

- **Écriture progressive** dans la réponse HTTP (`StreamingResponseBody`
  Spring) : chaque ligne est sérialisée puis flushée avant lecture de la
  suivante.
- **Lecture par lots côté base** : itération sur un curseur JDBC ou un
  `Stream<Parcelle>` JPA borné (taille de lot : 500 lignes). Aucun
  `findAll()` ni chargement complet.
- **Limite absolue** : `ecotrack.export.max-lignes` (défaut : `10 000`,
  cohérent avec H3 = 5 000 parcelles + marge). Au-delà, l'export est
  **refusé explicitement** en `413 Payload Too Large` — pas de troncature
  silencieuse.
- L'export est **inclus au périmètre du test de charge EX-NF-01** : les
  scénarios de charge exécutent aussi `/parcelles/export.csv` en parallèle
  du détail et de la liste, avec vérification de l'absence d'accumulation
  mémoire côté API.

---

## 5. Conception frontend (Next.js)

### 5.1 Écrans et traçabilité

| Écran | Route | Endpoints | Exigences | Rendu |
|---|---|---|---|---|
| Liste des parcelles | `/` | `GET /parcelles` | EX-F-05, EX-NF-01 | Server Component |
| Détail + historique | `/parcelles/[code]` | `GET /parcelles/{code}`, `.../releves` | EX-F-06 | Server Component |
| Création de parcelle | `/parcelles/nouvelle` | `POST /parcelles` | EX-F-01 | Client Component (formulaire) |
| Saisie d'un relevé | `/parcelles/[code]/releves/nouveau` | `POST .../releves` | EX-F-02 | Client Component |
| Journal des alertes | `/alertes` | `GET /alertes` | EX-F-07 | Server Component |
| Export CSV | action sur `/` | `GET /parcelles/export.csv` | EX-F-04 | bouton conditionné au flag |

### 5.2 Couche d'accès à l'API

`src/lib/api.ts` : **unique** point d'appel au backend, schémas **zod** dérivés
du contrat §4. Aucun `fetch` dans un composant, aucun `any` (CLAUDE.md).

```typescript
const ParcelleResume = z.object({
  code: z.string().regex(/^PRC-\d{4}-\d{1,3}$/),
  localite: z.string(),
  statut: z.enum(["EN_SUIVI", "EN_ALERTE"]),
  dernierTaux: z.number().nullable(),        // null = aucun relevé (EX-F-05 R4)
  dateDernierReleve: z.string().nullable(),
});
```

**Rôle de zod** : si l'API dévie du contrat, l'erreur est explicite et testable,
au lieu d'un `undefined` silencieux dans l'UI. Le contrat du SDD est ainsi
vérifié **des deux côtés**.

### 5.3 Feature flag côté interface

Le bouton d'export ne doit apparaître que si le flag est actif (EX-F-04 R1).
Le front n'a pas de configuration propre : il interroge `GET /api/v1/config`
qui expose les flags publics. **Une seule source de vérité** — le backend.
**ADR-004**.

### 5.4 Accessibilité (EX-NF-06)

Le badge d'alerte combine **couleur + texte + icône** (jamais la couleur seule),
contraste ≥ 4,5:1, `aria-label` explicite. États `loading` / `empty` / `error`
implémentés sur chaque écran via `loading.tsx` et `error.tsx` (App Router).

---

## 6. Configuration, profils et déploiement

| Profil | Base | Usage |
|---|---|---|
| `dev` | H2 en mode PostgreSQL | poste de développement |
| `test` | H2 en mode PostgreSQL, schéma Flyway | tests automatisés |
| `staging` | PostgreSQL | environnement de vérification |

Toute configuration sensible par variable d'environnement (jamais dans le code
ni dans une image). Flags : `ecotrack.features.export-csv` (défaut : `false`).

**Sondes** (EX-NF-02) : `readiness` conditionne la réception du trafic,
`liveness` le redémarrage — distinction indispensable au rolling update.
**Version** (EX-NF-04) : `build-info` Maven → `/actuator/info` ;
`NEXT_PUBLIC_VERSION` → pied de page web ; cohérence vérifiée par test.

---

## 7. Stratégie de tests

### 7.1 Pyramide

| Niveau | Outil | Portée | Où |
|---|---|---|---|
| Domaine pur | JUnit 5 | VO, `RegleStatutAlerte`, invariants, **cas limites** | PR |
| Architecture | `ApplicationModules.verify()` | frontières et cycles entre modules | PR |
| Module | `@ApplicationModuleTest` + `Scenario` | use cases, publication et réception d'events | PR |
| Adapters | `@WebMvcTest`, `@DataJpaTest` | contrat REST, mapping, contraintes SQL | PR |
| Front unitaire | vitest + Testing Library | composants, états, badge | PR |
| API bout-en-bout | RestAssured | scénarios Gherkin du SRS sur staging réel | post-déploiement |
| Navigateur | Playwright | parcours complets navigateur → API → BDD | post-déploiement |
| Charge | k6 / JMeter | EX-NF-01 (détail **et** liste) | post-déploiement |

### 7.2 Tests non négociables (issus des pièges du SRS)

1. `should_passer_en_alerte_when_taux_est_5995_pourcent` — 1199/2000, domaine pur.
2. `should_rester_en_suivi_when_taux_exactement_60_pourcent` — 1200/2000.
3. `should_ignorer_releve_antidate_when_determine_statut`.
4. `should_rester_en_suivi_when_aucun_releve`.
5. `should_journaliser_alerte_when_crash_apres_enregistrement` — EX-NF-03,
   vérifie la reprise de l'event au redémarrage.
6. `should_afficher_tiret_when_parcelle_sans_releve` (front).
7. `should_neutraliser_formule_when_localite_commence_par_egal` — adapter
   d'export CSV, SEC-01 : vérifie qu'une localité `=HYPERLINK(...)` sort
   préfixée d'une apostrophe et encadrée de guillemets.
8. `should_rejeter_when_size_superieur_a_100` — contrat REST, SEC-02 :
   vérifie que `GET /parcelles?size=101` et `GET /alertes?size=101` répondent
   `400 Bad Request` sans troncature silencieuse.
9. `should_ne_pas_exposer_schema_when_violation_contrainte` — gestionnaire
   d'erreurs global, SEC-04 : provoque chaque violation de contrainte connue
   et vérifie qu'aucun nom de table, contrainte, classe Java ni version ne
   figure dans la réponse d'erreur.

---

## 8. Matrice de traçabilité exigence → composant → test

| Exigence | Composant | Test |
|---|---|---|
| EX-F-01 | `parcelles` : `CodeParcelle`, `Superficie`, `Localite`, `ParcelleService`, `POST /parcelles` | unitaires VO, `@WebMvcTest`, `@DataJpaTest` (unicité), e2e |
| EX-F-02 | `releves` : `Releve`, `TauxDeSurvie`, `EnregistrerReleveUseCase` | unitaires, module, `@DataJpaTest` (doublon date), e2e |
| EX-F-03 | `releves` : `RegleStatutAlerte` + event `StatutParcelleChange` | unitaires **cas limites**, `@ApplicationModuleTest` |
| EX-F-04 | `parcelles` : export CSV sous flag ; bouton conditionné front | `@WebMvcTest` flag ON/OFF, vitest, e2e |
| EX-F-05 | `parcelles` : liste paginée triée + colonnes dénormalisées ; écran `/` | `@DataJpaTest` (tri/pagination), vitest, Playwright |
| EX-F-06 | `releves` : historique ; écran `/parcelles/[code]` | `@WebMvcTest`, vitest, Playwright |
| EX-F-07 | `alertes` : `EntreeJournal`, écouteur d'event, écran `/alertes` | `@ApplicationModuleTest`, Playwright |
| EX-NF-01 | index BDD + dénormalisation + pagination | test de charge staging (détail **et** liste) |
| EX-NF-02 | sondes readiness/liveness, migrations expand/contract | tests pendant déploiement réel |
| EX-NF-03 | event publication registry (Modulith) | test de crash/reprise |
| EX-NF-04 | `build-info` + `NEXT_PUBLIC_VERSION` | test post-déploiement de cohérence |
| EX-NF-05 | validation VO + `@Valid` + RFC 7807 | tests des cas d'erreur de chaque EX-F |
| EX-NF-06 | badge couleur+texte+icône, états UI | vitest + audit accessibilité |
| EX-NF-07 | aucun champ statut/taux en écriture | revue de contrat + `@WebMvcTest` |

**Aucune exigence du SRS v1.2 n'est orpheline.**

---

## 9. Points issus de la revue sécurité — état d'avancement

La revue sécurité (`docs/revue-securite-sdd.md`, 2026-07-29) a produit 8
constats (4 bloquants, 4 importants). Cette section fait le point sur ce qui
est **TRAITÉ dans la v1.1** et ce qui reste **EN DETTE**.

| Constat | Gravité | Statut v1.1 | Traité dans |
|---|---|---|---|
| SEC-01 — Injection de formule CSV | Bloquant | **TRAITÉ** | §4.3 (échappement adapter) + test 7 de §7.2 |
| SEC-02 — `size` non borné | Bloquant | **TRAITÉ** | §4.1 (bornes strictes) + test 8 de §7.2 |
| SEC-03 — Export CSV non paginé | Bloquant | **TRAITÉ** | §4.3 (flux + limite absolue) + §7.1 (charge inclut export) |
| SEC-04 — Fuite d'information par erreurs | Bloquant | **TRAITÉ** | §4.2 (gestionnaire global) + test 9 de §7.2 |
| SEC-05 — Pas de rate limiting ni de limite de corps | Important | **EN DETTE** | À traiter en Phase 10 (limitation au niveau ingress) — tracé ici, pas silencieux |
| SEC-06 — Rétention du journal des alertes | Important | **TRAITÉ** | §3.5 (24 mois, purge automatique) |
| SEC-07 — Purge des publications d'events | Important | **TRAITÉ** | §3.5 (purge des traitées + supervision des non traitées) |
| SEC-08 — Journalisation applicative | Important | **EN DETTE** | À concevoir avant Checkpoint 3 (opérations d'écriture, sans secret) — tracé ici, pas silencieux |

**Points de vigilance (SEC-09 à SEC-11)** : à surveiller pendant
l'implémentation, non repris ici (voir §4 de la revue).

**Rappel** : SEC-05 et SEC-08 restent des dettes **explicites** et
**tracées** — elles ne sont pas silencieusement abandonnées, elles sont
planifiées pour des jalons ultérieurs (Phase 10 pour SEC-05, Checkpoint 3
pour SEC-08).

---

*Historique :*
- *v1.0 — conception initiale tracée sur SRS v1.2.*
- *v1.1 — application des 6 corrections de la revue sécurité (SEC-01
  échappement CSV, SEC-02 bornes de pagination, SEC-03 export CSV en flux +
  inclusion dans EX-NF-01, SEC-04 gestion d'erreurs sans fuite, SEC-06
  rétention 24 mois du journal, SEC-07 purge/supervision des publications
  d'events, tests non négociables 7 à 9). Restent en dette : SEC-05
  (rate limiting), SEC-08 (journalisation applicative).*

*Toute évolution passe par une Pull Request référencée.*
