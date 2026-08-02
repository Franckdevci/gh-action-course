# Plan projet — EcoTrack API

**Dernière mise à jour** : 2026-08-02
**Sources de vérité** : `docs/srs.md` v1.3, `docs/sdd.md` v1.2, `docs/adr/`, `CLAUDE.md`

Document opérationnel de suivi. Le SRS reste le contrat métier, le SDD la conception, les ADR les décisions structurantes. Ce plan agrège leur état d'avancement, ne le remplace pas.

---

## 1. Vue d'ensemble

**Phase courante** : socle sécurité stabilisé (Waves 1/2/3), **EX-F-01, EX-F-02 et EX-F-03 livrés**. Prêt à attaquer **EX-F-07 (module `alertes`)** — chemin critique pour tenir EX-NF-03.

**Verrous en place** :
- SRS v1.3 conforme IEEE 830 / ISO 29148, aucune ambiguïté résiduelle.
- SDD v1.2 avec §9 refactoré en 3 états (SPEC / IMPL / TEST vert), sync post-Wave 2 en place.
- Branch protection `main` : PR obligatoire, checks requis (`mvn verify (api)`, `trivy fs scan`, `SonarCloud Code Analysis`).
- 6 règles ArchUnit maison verrouillant les invariants CLAUDE.md.
- Auto-delete des branches actives.
- **190 tests verts** sur `main`, ArchitectureTest + ArchitectureConventionsTest inclus.
- Événement de domaine `StatutParcelleChange` émis à chaque bascule de statut (jamais consommé pour l'instant — voir EX-F-07).

---

## 2. Exigences fonctionnelles (SRS §3)

| Id | Intitulé | Statut | PR | Notes |
|----|----------|--------|----|-------|
| EX-F-01 | Créer une parcelle | ✅ Livré | #10, #13, #14, #15 | Domaine + REST + JPA + Postman |
| EX-F-02 | Enregistrer un relevé | ✅ Livré | #29 | Option B initiale, complétée par EX-F-03 |
| EX-F-03 | Déterminer le statut d'alerte | ✅ Livré | #45, #48 | Projection incrémentale sur `Parcelle` (ADR-005/010), event `StatutParcelleChange` publié via `ApplicationEventPublisher`. 4 tests non-négociables §7.2 verts. |
| EX-F-04 | Exporter en CSV | 🔒 Feature flag off | — | Bloqué SEC-B-05 (adapter CSV à écrire au moment de l'implémentation) |
| EX-F-05 | Lister les parcelles paginées | ⏳ À faire | — | VO `Pagination` prêt (`PAGE_MAX=200`, `SIZE_MAX=100`) mais orphelin ; à câbler sur `GET /parcelles` |
| EX-F-06 | Fiche parcelle (détail + historique) | ⏳ À faire | — | Dépend de EX-F-02 (livré). Deux endpoints `GET /parcelles/{code}` + `GET /parcelles/{code}/releves` |
| EX-F-07 | Journal des alertes | ⏳ **Prochain chantier** | — | Module `alertes/` = squelette `package-info` seul. À faire : agrégat `EntreeJournal`, `@ApplicationModuleListener` sur `StatutParcelleChange`, endpoint `GET /alertes`, migration Flyway. Débloque EX-NF-03. |

---

## 3. Exigences non fonctionnelles (SRS §4)

| Id | Domaine | Statut | Preuve |
|----|---------|--------|--------|
| EX-NF-01 | Performance P95 < 500 ms | 🟡 Partiel | Dénormalisation en place (ADR-005), index `releve(parcelle_id, date_observation DESC)`. Test de charge k6 non écrit. Non testable tant qu'EX-F-05/06 absents. |
| EX-NF-02 | Zéro coupure (expand/contract) | 🟡 Partiel | Migrations V1-V4 additives ; sondes readiness/liveness activées (Wave 3). Test rolling update non écrit. |
| EX-NF-03 | Aucune perte d'alerte | 🟡 Partiel — **non tenu en pratique** | Event émis dans la transaction (PR #45), table `event_publication` créée (V1). **Aucun consommateur** ⇒ garantie purement documentaire jusqu'à EX-F-07. |
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
| ADR-003 | Events `releves → alertes` | Accepté | Émetteur en place (PR #45), consommateur attendu EX-F-07 |
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

---

## 6. Chantier en cours

| PR | Sujet | Statut |
|----|-------|--------|
| #48 | refactor: statut projeté incrémentalement, tests sur le chemin réel (supprime `RegleStatutAlerte` mort + ajoute ADR-010) | En review |

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
| Historique relevés paginé (EX-F-06) | Anti-pattern SEC-B-03 (revue v2 §V-06) | À couvrir dans EX-F-06 avec keyset pagination |
| Endpoint `POST /admin/events/{id}/retry` | ADR-008 | À implémenter avec EX-F-07 (flag `ecotrack.admin.enabled` déjà en place) |
| Publications non traitées dans `event_publication` | EX-F-03 livré sans consommateur | Résolu par EX-F-07 dès qu'`@ApplicationModuleListener` est en place |
| Log INFO au boot rappelant `retention.journal-alertes-mois` | SDD §3.5 | Mineur, à ajouter avec EX-F-07 |
| SDD §4 exemple `dernierTaux` en number → à harmoniser en string | SEC-B-03 note résiduelle | Mineur, PR docs |
| `SharedApiExceptionHandler.traiterViolationContrainte` message générique | SDD §4.2 attend `detail` distincts par contrainte | À faire au premier test de crash concurrent |

---

## 10. Roadmap courte (3 prochaines PR)

| Ordre | PR | Contenu | Motif |
|-------|----|---------|-------|
| 1 | **EX-F-07** module `alertes` | Agrégat `EntreeJournal`, `@ApplicationModuleListener` sur `StatutParcelleChange`, endpoint `GET /alertes?page&size`, migration Flyway V5 `alerte`. Consomme et clôt l'event publication registry. | Débloque EX-NF-03 (rejeu au crash), rend testable §7.2 n°5 (SEC-I-07), utilise le `Pagination` VO orphelin. |
| 2 | **EX-F-05** liste parcelles paginée | `GET /parcelles?page&size`, tri « alertes d'abord puis code », adapter Pagination sur Repository. | Débloque le parcours utilisateur de consultation, valide en runtime le VO `Pagination`. |
| 3 | **EX-F-06** fiche parcelle + historique | `GET /parcelles/{code}` + `GET /parcelles/{code}/releves` (paginé). | Complète le parcours consultation. |
| 4 (optionnel) | **EX-F-04** export CSV | Endpoint `GET /parcelles/export.csv`, adapter d'échappement, activation `ecotrack.features.export-csv=true`. | Ferme SEC-B-05 + SEC-01. |

---

## 11. Baseline sécurité

- **Wave 3** stabilisée (PR #32) : rate limiting body OK, actuator restreint OK, `ddl-auto=validate` OK, RFC 7807 sans fuite OK.
- **Audit conformité 2026-08-02** (`docs/audit-conformite-2026-08-02.md`) : aucune entorse silencieuse détectée sur les invariants critiques. Gaps identifiés sont **tous** annoncés Wave 2 dans le SDD §9 (cohérence docs/code).
- Prochain audit `security-reviewer` à lancer **après merge d'EX-F-07** — nouvelle surface (module `alertes` + endpoint admin `POST /admin/events/{id}/retry`) et rejeu d'events → surface d'attaque à qualifier.
