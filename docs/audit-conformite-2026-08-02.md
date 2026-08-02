# Audit de conformité EcoTrack — 2026-08-02

**Auteur** : agent `architecte-sdd`
**Périmètre** : `api/src/main` vs SRS v1.3 + SDD v1.2 + ADR-001..009.
**Méthode** : read-only. Aucun fichier modifié.

---

## Tableau 1 — Exigences (EX-F / EX-NF)

| Exigence | Statut | Preuve (`api/src/main`) | Tests (`api/src/test`) | Écart / commentaire |
|---|---|---|---|---|
| **EX-F-01** Créer parcelle | ✅ complet | `parcelles/infrastructure/rest/ParcellesController.java` (POST /api/v1/parcelles), `CreerParcelleRequest` (validation Bean), `parcelles/application/CreerParcelleUseCase.java`, VOs `CodeParcelle`, `Localite`, `Superficie`, `NombrePlants`, `Parcelle.creer` | `parcelles/domaine/*Test.java` (VOs), `application/CreerParcelleUseCaseTest.java`, `infrastructure/jpa/JpaParcelleRepositoryTest.java` (unicité), `infrastructure/rest/ParcellesControllerTest.java`, `ParcellesRestHardeningTest.java` | RAS |
| **EX-F-02** Enregistrer relevé | ✅ complet | `RelevesController.java` (POST /api/v1/parcelles/{code}/releves), `EnregistrerReleveUseCase.java` (contrôle existence parcelle, doublon date, calcul du taux via `Releve.enregistrer`), `Releve` domaine | `releves/domaine/ReleveTest.java`, `NombrePlantsVivantsTest.java`, `application/EnregistrerReleveUseCaseTest.java`, `infrastructure/jpa/JpaRelevesRepositoryTest.java`, `infrastructure/rest/RelevesControllerTest.java` | RAS |
| **EX-F-03** Statut d'alerte + événement | ⚠️ partiel | `RegleStatutAlerte.java` OK (règle pure domaine, seuil strict, tri par date). `Parcelle.enregistrerDernierReleve` retourne `Optional<StatutChange>` et met à jour la ligne parcelle. `EnregistrerReleveUseCase` publie `StatutParcelleChange` via `ApplicationEventPublisher`. | `RegleStatutAlerteTest.java` couvre §7.2 n°1 (5995 %), n°2 (60 % exact), n°3 (antidaté), n°4 (aucun relevé). `EnregistrerReleveUseCaseTest.should_publier_StatutParcelleChange_when_releve_declenche_alerte` + `should_ne_pas_publier_event_when_statut_ne_change_pas`. `StatutParcelleChangeTest.java`. | Écarts : (a) publication via `ApplicationEventPublisher` standard, **aucun consommateur `alertes` n'existe** donc chaque bascule accumule une publication non traitée. (b) test §7.2 n°5 `should_journaliser_alerte_when_crash_apres_enregistrement` **absent** (SEC-I-07 Wave 2). (c) l'invariant *rétablissement journalisé* (EX-F-03 R2) est calculé mais jamais consommé. |
| **EX-F-04** Export CSV | ❌ non implémenté | Aucun endpoint `/parcelles/export.csv`, aucun adapter CSV, `EcotrackProperties.Features.exportCsv` défini mais jamais lu. `ecotrack.export.max-lignes=10000` défini sans usage. | Aucun test | SEC-01 (échappement formule), SEC-03 (streaming) et test §7.2 n°7 tous en dette. Cohérent SDD §9 Wave 2. |
| **EX-F-05** Liste paginée | ❌ non implémenté | Aucun `@GetMapping` sur `/parcelles`. `Pagination.java` (VO) présent, bornes 0..200 / 1..100, **jamais câblé** à un contrôleur. `ParcellesRepository` n'expose aucune méthode `lister(Pagination)`. | `PaginationTest.java` (VO, 17 cas). Aucun test contrôleur/list. | SDD §4 annonce `GET /parcelles?page&size` (200/400/500) et tri « alertes d'abord puis code ». Rien en code. |
| **EX-F-06** Détail + historique | ❌ non implémenté | Aucun `GET /parcelles/{code}`, aucun `GET /parcelles/{code}/releves`. `ParcellesRepository.trouverParCode` existe mais ne remonte qu'une `ParcelleReference` (projection insuffisante pour un détail complet). | Aucun test | SDD §4 : 200/404/500 attendus. Gap complet. |
| **EX-F-07** Journal des alertes | ❌ non implémenté | Module `alertes/` = **1 seul fichier** `package-info.java`. Aucun agrégat `EntreeJournal`, aucun `@ApplicationModuleListener` sur `StatutParcelleChange`, aucun endpoint `GET /alertes`. Aucune migration Flyway pour une table `alerte`. | Aucun test | Gap critique : SDD §2 promet 3 modules applicatifs, seuls 2 sont fonctionnels. `event_publication` (V1) est prête à persister mais aucun listener → chaque bascule accumule une publication non traitée. |
| **EX-NF-01** P95 < 500 ms | ⚠️ conception seule | Dénormalisation en place : colonnes `dernier_taux` + `date_dernier_releve` sur `parcelle` (V4), index `releve(parcelle_id, date_observation DESC)` (V3). Update dénorm dans la même transaction. | Aucun test de charge (post-déploiement, SDD §7.1) | Impossible à vérifier tant qu'EX-F-05/EX-F-06 ne sont pas livrés. Index `parcelle(statut, code)` promis §3.4 pas encore posé. |
| **EX-NF-02** Continuité rolling update | ⚠️ partiel | `application.yml` : `management.endpoint.health.probes.enabled=true` (readiness/liveness). `spring.jpa.hibernate.ddl-auto=validate` + Flyway. Migrations V1-V4 additives. | Aucun test | Test « pendant remise en service » attendu post-déploiement. |
| **EX-NF-03** Fiabilité journal | ⚠️ partiel | Table `event_publication` créée (V1). `EnregistrerReleveUseCase` publie l'event dans la transaction. | Aucun test crash/reprise (§7.2 n°5) | **Non tenu en pratique** : sans consommateur (voir EX-F-07), aucun journal ne peut être rejoué. La garantie est purement documentaire. |
| **EX-NF-04** Traçabilité version | ⚠️ partiel | `management.endpoints.web.exposure.include=health,info` + `management.info.build.enabled=true`. | Aucun test post-déploiement | Pied de page front hors périmètre code. |
| **EX-NF-05** Sécurité entrées | ✅ complet | `SharedApiExceptionHandler` (`@Order(LOWEST_PRECEDENCE)`, RFC 7807, `Exception.class` neutre, `DataIntegrityViolationException` masqué, `NoResourceFoundException`). `ApiExceptionHandler` (parcelles) traite `MethodArgumentNotValidException` sans réfléchir la valeur. `application.yml` : `include-message: never`, `include-stacktrace: never`, `whitelabel.enabled: false`. `Localite` (null byte / RTL / contrôles). `spring.jackson.deserialization.fail-on-unknown-properties=true`. Bornes body 256 KB. | `SharedApiExceptionHandlerTest`, `ParcellesRestHardeningTest`, `LocaliteTest`, `ParcellesControllerTest.should_ne_pas_refleter_input_when_code_contient_script` | RAS |
| **EX-NF-06** Accessibilité / a11y | N/A | Hors périmètre backend (aucun Next.js dans le dépôt). | — | — |
| **EX-NF-07** Statut/taux jamais saisis | ✅ complet | `CreerParcelleRequest` et `EnregistrerReleveRequest` ne portent aucun champ `statut`/`taux`. `TauxDeSurvie.calculer(plantsVivants, plantsInitiaux)` est le seul chemin de calcul. `Parcelle.creer` force `EN_SUIVI`. | Vérifié implicitement par tous les tests contrôleur | RAS |

---

## Tableau 2 — Divergences contrat API (SDD §4 vs code)

| Endpoint SDD §4 | Présent ? | Codes attendus | Codes réels | Divergence |
|---|---|---|---|---|
| `POST /parcelles` | ✅ | 201, 400, 409, 500 | 201 (Location + body), 400 (validation), 409 (`CodeParcelleDejaUtiliseException`), 500 (fallback shared) | Aucune. Annotations Swagger `@ApiResponses` à jour. |
| `GET /parcelles?page&size` | ❌ absent | 200, 400, 500 | — | Endpoint manquant (EX-F-05). Le VO `Pagination` existe mais n'est câblé à aucun `@GetMapping`. |
| `GET /parcelles/{code}` | ❌ absent | 200, 404, 500 | — | Endpoint manquant (EX-F-06). |
| `POST /parcelles/{code}/releves` | ✅ | 201, 400, 404, 409, 500 | 201 (Location + body), 400 (validation + `DonneeReleveInvalideException`), 404 (`ParcelleIntrouvableException`), 409 (`ReleveDoublonException`), 500 (fallback) | Aucune divergence côté codes. |
| `GET /parcelles/{code}/releves` | ❌ absent | 200, 404, 500 | — | Endpoint manquant (EX-F-06). |
| `GET /alertes?page&size` | ❌ absent | 200, 400, 500 | — | Endpoint manquant (EX-F-07). |
| `GET /parcelles/export.csv` | ❌ absent | 200, 404 (flag OFF), 413, 500 | — | Endpoint manquant (EX-F-04). |
| `GET /actuator/health`, `/actuator/info` | ✅ | 200 | 200 (exposition minimale confirmée) | RAS. `show-details: never` conforme SEC-I-02. |
| `POST /admin/events/{id}/retry` | ❌ absent | 202, 400, 404, 409, 500 | — | Endpoint manquant (ADR-008, Wave 2). Flag `ecotrack.admin.enabled=false` déjà provisionné. |

**Ambiguïtés SDD relevées** :

1. SDD §4 exemple `GET /parcelles` retourne `dernierTaux: "59.9"` (chaîne) mais l'exemple `POST /parcelles` renvoie `dernierTaux: null` sans exemple non-null en création. SDD §9.2 (SEC-B-03) note que « l'exemple §4 montre encore le taux comme nombre JSON, à harmoniser en `"60.0"` ». Le code émet bien une chaîne (`ParcelleResponse.tauxFormate` = `toPlainString()`), donc écart purement documentaire.
2. SDD §4 tableau : `GET /parcelles/export.csv` liste `404 (flag OFF)`. Le mécanisme ADR-004 (`@ConditionalOnProperty`) n'est pas encore implémenté ; à la livraison, prévoir que le handler shared renvoie bien 404 via `NoResourceFoundException` (déjà supporté par `SharedApiExceptionHandler`).

---

## Tableau 3 — Conformité aux ADR-001 à ADR-009

| ADR | Sujet | Statut | Preuve / entorse |
|---|---|---|---|
| **ADR-001** Spring Modulith | ⚠️ partiel | Modules `parcelles`, `releves`, `shared` corrects. `alertes` déclaré (`package-info` + `@ApplicationModule`) mais **vide** — le module Modulith existe mais ne remplit aucune responsabilité. `ArchitectureTest.les_modules_verifient_leurs_frontieres` en place. |
| **ADR-002** Domaine séparé JPA + Flyway | ✅ | Aucun `import jakarta.persistence` ni `import org.springframework` dans `parcelles/domaine` ni `releves/domaine`. `ArchitectureConventionsTest.domaine_ne_depend_ni_de_spring_ni_de_jpa` couvre. Migrations V1-V4 sous `db/migration`, `ddl-auto=validate`. |
| **ADR-003** Event Modulith `releves → alertes` | ⚠️ **entorse partielle** | `releves` **ne dépend pas** de `alertes` (aucun import). `EnregistrerReleveUseCase` publie via `ApplicationEventPublisher` standard — API sur laquelle Modulith s'appuie, donc conforme côté émetteur. **Côté consommateur : aucun listener n'existe** — la garantie du rejeu au redémarrage n'a rien à rejouer et l'entrée reste `event_publication.completion_date = NULL` indéfiniment. |
| **ADR-004** Feature flags | ⚠️ partiel | `EcotrackProperties.Features.exportCsv` défini, valeur par défaut `false`. Aucun contrôleur ne le lit. Endpoint `GET /api/v1/config` (SDD §5.3) absent. |
| **ADR-005** Dénormalisation dernier relevé | ✅ | Colonnes `dernier_taux`, `date_dernier_releve` sur `parcelle` (V4). `Parcelle.enregistrerDernierReleve` (domaine) fait l'update ; `MettreAJourDernierReleveUseCase` appelé dans la même transaction que `RelevesRepository.enregistrer`. Vérif grep : `RelevesRepository.enregistrer` n'est appelé que par `EnregistrerReleveUseCase.executer` — un seul chemin d'écriture de relevé, aucun bypass. Correction SEC-V-04 respectée par construction. |
| **ADR-006** Plafonds export CSV | ❌ non implémenté | Configuration `ecotrack.export.max-lignes=10000` et `EcotrackProperties.Export.maxLignes` en place, mais aucun consommateur. |
| **ADR-007** Rétention et purges | ⚠️ partiel | Configuration `ecotrack.retention.journal-alertes-mois=24` + validation `@Min(12)` (fail-fast au boot, SEC-I-06 partiellement traité). Aucune tâche `@Scheduled` de purge (ni journal, ni event_publication). ADR encore Proposé. |
| **ADR-008** DLQ event | ❌ non implémenté | Aucun endpoint `POST /admin/events/{id}/retry`, aucune configuration `management.server.port` sur le profil par défaut. |
| **ADR-009** Journalisation applicative | ❌ non implémenté | Aucune configuration Logback structurée JSON, aucun MDC `traceId`, aucune validation `traceparent` (SDD §6.3). |

**Vérifications transverses effectuées** :

- Aucun `@JsonTypeInfo` ni `enableDefaultTyping` dans `api/src/main` (grep vide) → SEC-V-05 respecté.
- Aucun `@Autowired` sur champ ; injection constructeur partout.
- Aucun usage de `java.util.Date`, `java.sql.Timestamp`, `java.util.Calendar` (règle ArchUnit).

---

## Ambiguïtés / signalements

1. **SRS §1.3 « Dernier relevé »** : dit « par date d'observation, indépendamment de l'ordre de saisie ». Le domaine `Parcelle.enregistrerDernierReleve` implémente correctement `dateObservation.isAfter(this.dateDernierReleve)` (rejet des antidatés pour l'update dénorm), mais **ne recalcule pas** le statut à partir de l'historique complet lors d'un antidaté récent. Comportement correct pour EX-F-03 R1 dans le scénario nominal, mais pas robuste si un antidaté remplaçait le vrai dernier relevé (cas non couvert par le SRS). À clarifier : jamais suppression/modification de relevé prévue en v1 → OK, à documenter.
2. **SDD §4 tableau** annonce `POST /admin/events/{id}/retry` sur la même ligne que le contrat public. §4.4 clarifie que l'endpoint est sur le port management. Le tableau §4 gagnerait à distinguer les colonnes.
3. **SDD §4.2 « Codes 409 »** cite `DataIntegrityViolationException` comme trigger générique. `ApiExceptionHandler` (parcelles) traite spécifiquement `CodeParcelleDejaUtiliseException` en 409 ; le fallback `SharedApiExceptionHandler.traiterViolationContrainte` en 409 est correct mais le message « Contrainte de base de données violée » ne distingue pas les deux contraintes uniques (`parcelle_code_unique` vs `releve_parcelle_date_unique`). Un test de contournement (crash concurrent) tomberait sur le message générique.
4. **SDD §3.5** exige `ecotrack.retention.journal-alertes-mois ≥ 12` **et** un log INFO au démarrage rappelant la valeur retenue. Premier point : `@Min(12)` OK. Second point : log INFO absent. Mineur.

---

## Verdict global

- **Fondations solides** : EX-F-01, EX-F-02, EX-F-03 (règle métier + événement domain-side), EX-NF-05, EX-NF-07 sont livrés avec des tests non triviaux, incluant les 4 premiers tests §7.2 non-négociables (5995 %, 60 %, antidaté, aucun relevé).
- **Écart le plus significatif** : le module `alertes` est un squelette (`package-info` seul). Trois exigences en découlent en cascade : EX-F-07 (0 %), EX-NF-03 (garantie documentaire non tenue en pratique), test §7.2 n°5 (crash/reprise) impossible à écrire. Chaque bascule de statut aujourd'hui produit une publication non traitée en base sans consommateur — dette qui grossit à chaque livraison des cas EX-F-03.
- **Lectures manquantes** : EX-F-05, EX-F-06, EX-F-04 → aucun `GET /…` sur les parcelles, aucun endpoint d'export. Le VO `Pagination` est prêt mais orphelin.
- **Cohérence avec SDD §9** : les gaps constatés sont **tous** annoncés Wave 2 (SEC-B-05, SEC-I-07, SEC-I-01, SEC-I-03/04/06, EX-F-04/05/06/07). Aucune surprise ; le SDD est honnête sur l'avancement.
- **Pas d'entorse silencieuse détectée** : les invariants critiques du `CLAUDE.md` (`TauxDeSurvie` BigDecimal exact, dénormalisation dans la même transaction, tri par `dateObservation`, aucun champ statut/taux en écriture, unicité BDD) sont tous respectés dans le code livré.

**Prochaine étape logique** : **EX-F-07** (module `alertes` + `@ApplicationModuleListener` sur `StatutParcelleChange`). Débloque EX-F-03 (côté lecture), EX-NF-03 (rejeu au redémarrage effectivement testable), et test §7.2 n°5.
