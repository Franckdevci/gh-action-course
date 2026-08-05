# Plan projet — EcoTrack API

**Dernière mise à jour** : 2026-08-05 (post EX-F-06)
**Sources de vérité** : `docs/srs.md` v1.3, `docs/sdd.md` v1.2, `docs/adr/`, `CLAUDE.md`

Document opérationnel de suivi. Le SRS reste le contrat métier, le SDD la conception, les ADR les décisions structurantes. Ce plan agrège leur état d'avancement, ne le remplace pas.

---

## 1. Vue d'ensemble

**Phase courante** : socle sécurité stabilisé (Waves 1/2/3), **EX-F-01, EX-F-02, EX-F-03, EX-F-05, EX-F-06 et EX-F-07 livrés**. EX-NF-03 tenue en pratique (event consommé + registry `event_publication` complété). Reste EX-F-04 (export CSV).

**Verrous en place** :
- SRS v1.3 conforme IEEE 830 / ISO 29148, aucune ambiguïté résiduelle.
- SDD v1.2 avec §9 refactoré en 3 états (SPEC / IMPL / TEST vert), sync post-Wave 2 en place.
- Branch protection `main` : PR obligatoire, checks requis (`mvn verify (api)`, `trivy fs scan`, `SonarCloud Code Analysis`).
- 6 règles ArchUnit maison verrouillant les invariants CLAUDE.md.
- Auto-delete des branches actives.
- **238 tests verts** sur la branche EX-F-06, ArchitectureTest + ArchitectureConventionsTest inclus.
- Événement `StatutParcelleChange` émis + consommé par `AlertesService.surStatutParcelleChange` (`@ApplicationModuleListener`), durabilité assurée par Spring Modulith `event_publication` registry.

---

## 2. Exigences fonctionnelles (SRS §3)

| Id | Intitulé | Statut | PR | Notes |
|----|----------|--------|----|-------|
| EX-F-01 | Créer une parcelle | ✅ Livré | #10, #13, #14, #15 | Domaine + REST + JPA + Postman |
| EX-F-02 | Enregistrer un relevé | ✅ Livré | #29 | Option B initiale, complétée par EX-F-03 |
| EX-F-03 | Déterminer le statut d'alerte | ✅ Livré | #45, #48 | Projection incrémentale sur `Parcelle` (ADR-005/010), event `StatutParcelleChange` publié via `ApplicationEventPublisher`. 4 tests non-négociables §7.2 verts. |
| EX-F-04 | Exporter en CSV | 🔒 Feature flag off | — | Bloqué SEC-B-05 (adapter CSV à écrire au moment de l'implémentation) |
| EX-F-05 | Lister les parcelles paginées | ✅ Livré | (cette PR) | `GET /api/v1/parcelles?page&size`, tri EN_ALERTE puis code croissant via `@Query` explicite (pas d'ordre alphabétique fragile), migration V6 index `(statut, code)`, 10 tests couvrant les 5 scénarios Gherkin (parc vide, page au-delà, R4 taux null, tri, pagination). |
| EX-F-06 | Fiche parcelle (détail + historique) | ✅ Livré | (cette PR) | `GET /parcelles/{code}` (fiche + statut projeté) + `GET /parcelles/{code}/releves?page&size` antichronologique par `dateObservation`. Réutilise index V3 `releve(parcelle_id, date_observation DESC)`. 12 tests (4 REST fiche, 4 REST historique, 4 JPA historique dont antidaté + isolation par parcelle). |
| EX-F-07 | Journal des alertes | ✅ Livré | (cette PR) | Agrégat `EntreeJournal` immuable, `AlertesService` avec `@ApplicationModuleListener` sur `StatutParcelleChange`, endpoint `GET /alertes?page&size`, migration V5, 22 tests. Débloque EX-NF-03. |

---

## 3. Exigences non fonctionnelles (SRS §4)

| Id | Domaine | Statut | Preuve |
|----|---------|--------|--------|
| EX-NF-01 | Performance P95 < 500 ms | 🟡 Partiel | Dénormalisation en place (ADR-005), index `releve(parcelle_id, date_observation DESC)` + `parcelle(statut, code)` (V6, EX-F-05). EX-F-06 livré → parcours consultation complet, testable en k6. Test de charge non écrit. |
| EX-NF-02 | Zéro coupure (expand/contract) | 🟡 Partiel | Migrations V1-V4 additives ; sondes readiness/liveness activées (Wave 3). Test rolling update non écrit. |
| EX-NF-03 | Aucune perte d'alerte | ✅ Livré | Event émis dans la transaction (PR #45), consommé par `AlertesService.surStatutParcelleChange` (`@ApplicationModuleListener` → `@Async` + `@Transactional(REQUIRES_NEW)`). Registry `event_publication` complété à chaque bascule, vérifié par `JournalisationEndToEndTest`. |
| EX-NF-04 | Version exposée | 🟡 Partiel | `/actuator/info` avec `management.info.build.enabled=true`. Header web à ajouter avec front. |
| EX-NF-05 | RFC 7807 sans détails internes | ✅ Livré | Waves 1/2 + `SharedApiExceptionHandler` (`Exception.class`, `DataIntegrityViolationException`, `NoResourceFoundException`), `ParcellesRestHardeningTest` (4 cas non-reflection) |
| EX-NF-06 | Ergonomie / accessibilité | ⏳ Front absent | Attend module `web/` Next.js |
| EX-NF-07 | Taux et statut calculés par le système | ✅ Livré | `TauxDeSurvie.calculer()` + `fail-on-unknown-properties`. Aucun champ `statut`/`taux` dans les requests. |

Légende : ✅ complet / 🟡 partiel / ⏳ à faire / 🔒 bloqué

---

## 4. ADR

| Id | Titre | Statut | Notes |
|----|-------|--------|-------|
| ADR-001 | Spring Modulith 3 modules | Accepté | Vérifié par `ApplicationModules.verify()` ; module `alertes` déclaré mais vide |
| ADR-002 | Persistance JPA + Flyway | Accepté | `ddl-auto=validate` en place, 4 migrations additives |
| ADR-003 | Events `releves → alertes` | Accepté | Émetteur + consommateur en place (EX-F-07). `event_publication` registry actif. |
| ADR-004 | Feature flag export CSV | Accepté | Config `ecotrack.features.export-csv=false` — orpheline tant qu'EX-F-04 absent |
| ADR-005 | Dénormalisation dernier relevé | Accepté | Livré, complété par ADR-010 |
| ADR-006 | Plafonds export CSV | Accepté | 10 000 lignes max, actif à l'impl EX-F-04 |
| ADR-007 | Rétention et purges | ⚠️ Proposé | Bloque sur CLARIF-01 ; `@Min(12)` en place mais aucun `@Scheduled` |
| ADR-008 | DLQ event `StatutParcelleChange` | Proposé | À valider avec EX-F-07 (endpoint `POST /admin/events/{id}/retry` absent) |
| ADR-009 | Journalisation applicative | Proposé | Logback JSON structuré, MDC `traceId` — à implémenter |
| **ADR-010** | **Projection incrémentale du statut** | **Accepté** | **Complète ADR-005. Formalise l'hypothèse d'irrévocabilité des relevés en v1. Déclencheurs de reconsidération listés.** |

---

## 5. Historique des livraisons

| PR | Sujet | Merge |
|----|-------|-------|
| #1 | SRS v1.2 | — |
| #2 | SDD v1.0 | — |
| #3 | CLAUDE.md | — |
| #4 | ADR-001…005 | — |
| #5 | Revue sécurité SDD v1 | — |
| #6 | Bootstrap Spring Boot + Modulith | — |
| #7…#9 | Pipeline CI + hardening permissions | — |
| #10, #13, #14 | EX-F-01 domaine + application + REST | — |
| #11 | Trivy fs scan | — |
| #12 | Branch protection main | — |
| #15 | Postman collection EX-F-01 | — |
| #16 | Socle `Pagination` + RFC 7807 JSON malformé | — |
| #17 | Handler global RFC 7807 `DonneeInvalideException` | — |
| #18 | Activation JaCoCo | — |
| #20 | SRS v1.3 corrective IEEE 830 | — |
| #21 | ADR-006…009 | — |
| #22 | 6 règles ArchUnit maison | — |
| #23 | 4 sous-agents Claude + refonte `.gitignore` | — |
| #24 | Cache Trivy quotidien + artefact JaCoCo | — |
| #25 | **Wave 1** — SDD v1.2 + revue sécurité v2 | — |
| #26 | **Wave 2** — corrections code (handler global, PAGE_MAX, Localite charset) | — |
| #27 | Intégration SonarCloud | — |
| #28 | Fix Sonar caractères Unicode invisibles | — |
| #29 | **EX-F-02** enregistrer un relevé (Option B) | 2026-08-01 |
| #32 | **Wave 3** config `application.yml` + validation boot | 2026-08-01 |
| #34 | Plan projet + Docker image API + workflow Claude Review | 2026-08-01 |
| #35 | Publication image API sur GHCR | 2026-08-01 |
| #36 | Swagger UI dev-only + fix 500-on-404 handler global | 2026-08-01 |
| #37 | Swagger @Operation/@ApiResponses/@Tag | 2026-08-01 |
| #38 | Swagger schema JSON 201 (Parcelle + Releve) | 2026-08-01 |
| #39 | Docs Swagger + fix workflow health-check | 2026-08-01 |
| #40 | fix(SEC-B-03) formatage BigDecimal pur | 2026-08-01 |
| #41 | chore(ci) pin claude-review sur claude-sonnet-4-6 | 2026-08-01 |
| #42 | test(SEC-B-06) 4 tests REST hardening non-reflection | 2026-08-01 |
| #43 | docs(sdd) sync §9 SEC-B-04 et SEC-B-06 | 2026-08-01 |
| #44 | docs(sdd) sync complet du tableau §9 (7 lignes) | 2026-08-02 |
| #45 | **EX-F-03** règle de statut + event `StatutParcelleChange` | 2026-08-02 |
| #46 | chore(ci) debug claude-review (`show_full_output`) | 2026-08-02 |
| #47 | chore check-in slash commands `.claude/commands/*.md` | 2026-08-02 |
| #48 | refactor statut projeté incrémentalement (ADR-010) | 2026-08-02 |
| #50 | **EX-F-07** module `alertes` + 3 correctifs sécu ÉLEVÉS | 2026-08-02 |
| #51 | **EX-F-05** liste parcelles paginée + fix SEC-FAIB-02 | 2026-08-02 |
| (cette PR) | **EX-F-06** fiche parcelle + historique antichronologique paginé | — |

---

## 6. Chantier en cours

| PR | Sujet | Statut |
|----|-------|--------|
| (à ouvrir) | **EX-F-06** fiche parcelle + historique paginé antichronologique | En review |

---

## 7. Issues ouvertes

| Id | Sujet | Priorité | Notes |
|----|-------|----------|-------|
| #19 | Test négatif absence de champs internes dans `ProblemDetail` (SEC-04) | Basse — couvert par `SharedApiExceptionHandlerTest` (PR #26) | À fermer après revue |
| #30 | Unifier ou départager les 2 `@RestControllerAdvice HIGHEST_PRECEDENCE` | ❌ Wontfix (voir ADR-010 + verdict audit 2026-08-02 : fusion écartée pour préserver les frontières Modulith) | À fermer |
| #31 | Rate-limiting global (arbitrage n°1 auth) | Basse | Reporté Phase 10 Ingress (SEC-05) |

---

## 8. Points ouverts / arbitrages requis

| Sujet | Bloque | Décideur | Notes |
|-------|--------|----------|-------|
| **Recharger `ANTHROPIC_API_KEY`** (opérationnel) | claude-review + weekly-health-check | Toi (Console Anthropic Billing) | Cause identifiée le 2026-08-02 : `Credit balance is too low` (billing_error 400). Solution alternative : migrer vers `claude_code_oauth_token` |
| **CLARIF-01** — EX-F-07 R1 « jamais supprimé » vs ADR-007 rétention 24 mois | ADR-007 statut Accepté | Sponsor + analyste-srs | 3 options : lecture stricte / amender SRS / retirer purge |
| **Arbitrage n°1** — Authentification | EX-F-04 exposition, rate-limiting global | Sponsor | Hors périmètre v1 explicite mais à préparer v2 |
| **Seuil N=5** DLQ event (ADR-008) | ADR-008 statut Accepté | Observation staging | Sera validé après EX-F-07 + staging |

---

## 9. Dette technique tracée

| Élément | Origine | Prochaine action |
|---------|---------|------------------|
| Test smoke `/actuator/{env,beans,mappings,heapdump}` → 404 | Wave 3 §9.3 | Ajouter avec EX-F-07 (`@SpringBootTest` complet) |
| Validation regex W3C `traceparent` | SEC-I-04, Wave 3 §9.3 | Reporté à la PR Micrometer Tracing (ADR-009) |
| `@Pattern` sur `@PathVariable code` | Audit PR #29 SEC-FAIB-01 | Trivial, à faire au premier passage sur `RelevesController` |
| Historique relevés — passage en keyset pagination | Anti-pattern SEC-B-03 (revue v2 §V-06) — EX-F-06 livré avec offset paginé standard, borné à `size ≤ 100` | À basculer en keyset (`?before=<dateObservation>`) au premier signal de volumétrie ou à la prochaine passe sécu |
| Endpoint `POST /admin/events/{id}/retry` | ADR-008 | À implémenter avec EX-F-07 (flag `ecotrack.admin.enabled` déjà en place) |
| Publications non traitées dans `event_publication` | EX-F-03 livré sans consommateur | ✅ Résolu par EX-F-07 (listener en place, publications complétées à chaque bascule) |
| Log INFO au boot rappelant `retention.journal-alertes-mois` | SDD §3.5 | Mineur, à ajouter avec le job de purge (bloqué par CLARIF-01) |
| **SEC-ELEV-01 hardening prod** — REVOKE UPDATE/DELETE sur `alerte` + rôle `ecotrack_janitor` distinct pour la purge | Audit sécu EX-F-07 (2026-08-02) | Défendu en-app par `@Immutable` + `updatable=false`. Trigger PG à installer Phase 10 Ingress (dépend rôles PG documentés dans ADR-007) |
| SEC-MOY-01 — `PageAlertesResponse.totalPages` en `int`, borner à `PAGE_MAX+1` ou passer en `long` + champ `hasMore` | Audit sécu EX-F-07 | Non exploitable v1 (5 000 parcelles), mais divergence contrat/réalité si le volume monte |
| SEC-FAIB-01 — décider exposition `id` UUID interne dans `AlerteResponse` | Audit sécu EX-F-07 | Aucun consommateur v1. Soit supprimer, soit documenter `@Schema(description="opaque, sans usage client v1")` |
| ~~SEC-FAIB-02~~ handler `MethodArgumentTypeMismatchException` | ✅ Résolu dans PR EX-F-05 (`SharedApiExceptionHandler.traiterParametreMalType`) + 2 tests |
| SEC-INFO-01 — test `should_200_when_page_egale_200` (borne haute inclusive) sur `GET /alertes` | Audit sécu EX-F-07 | Trivial, à ajouter au premier passage |
| SEC-INFO-02 — index sur `alerte(parcelle_id)` si un futur `GET /parcelles/{code}/alertes` est demandé | Audit sécu EX-F-07 | À déclencher avec le besoin métier |
| SEC-INFO-03 — `@Schema` doc du champ `tauxDeclencheur` (unité pourcent, échelle 1 décimale) | Audit sécu EX-F-07 | Cohérence avec `ParcelleResponse.dernierTaux` |
| Purge scheduled `alerte` (rétention 24 mois) + purge event_publication 7 jours | SDD §3.5 | Bloqué par **CLARIF-01** (« immuable » vs rétention 24 mois) — pas d'implémentation tant que sponsor n'a pas tranché |
| Endpoint admin `POST /admin/events/{id}/retry` | ADR-008 | À implémenter quand un cas concret de DLQ apparaît sur staging (seuil N=5 à valider) |
| Rejeu par lots au boot (`batch-size=50`) | SDD §3.5 correction SEC-V-06 | À implémenter avant premier déploiement en prod (rejeu synchrone actuel = risque de saturation CPU) |
| SDD §4 exemple `dernierTaux` en number → à harmoniser en string | SEC-B-03 note résiduelle | Mineur, PR docs |
| `SharedApiExceptionHandler.traiterViolationContrainte` message générique | SDD §4.2 attend `detail` distincts par contrainte | À faire au premier test de crash concurrent |

---

## 10. Roadmap courte (3 prochaines PR)

| Ordre | PR | Contenu | Motif |
|-------|----|---------|-------|
| 1 | **EX-F-04** export CSV | Endpoint `GET /parcelles/export.csv`, adapter d'échappement, activation `ecotrack.features.export-csv=true`. | Ferme SEC-B-05 + SEC-01, dernière exigence fonctionnelle v1. |
| 2 | **k6 sur `/parcelles` + `/parcelles/{code}/releves`** | Preuve chiffrée EX-NF-01 P95 < 500 ms sur les 2 endpoints de consultation. | Débloqué par EX-F-06 livré. |
| 3 (dette hors sécu) | **Regex `PRC-\d{4}-\d{1,3}` extraite en `shared.CodeParcelleFormat`** | Aujourd'hui dupliquée 3× (`CodeParcelle`, `EntreeJournal`, `CreerParcelleRequest`) — risque divergence future | Priorité HAUTE mais coût court, à faire au prochain passage sur les validations |

---

## 11. Baseline sécurité

- **Wave 3** stabilisée (PR #32) : rate limiting body OK, actuator restreint OK, `ddl-auto=validate` OK, RFC 7807 sans fuite OK.
- **Audit conformité 2026-08-02** (`docs/audit-conformite-2026-08-02.md`) : aucune entorse silencieuse détectée sur les invariants critiques. Gaps identifiés sont **tous** annoncés Wave 2 dans le SDD §9 (cohérence docs/code).
- **Prochain audit `security-reviewer` à lancer sur la PR EX-F-07** avant merge : nouvelle surface `GET /alertes` (pagination bornée réutilisée du VO existant), listener `@ApplicationModuleListener` (durabilité via `event_publication` mais pas encore de rejeu par lots au boot), migration V5 (FK vers `parcelle`, index `survenu_le DESC`).
