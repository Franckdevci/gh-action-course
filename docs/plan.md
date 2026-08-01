# Plan projet — EcoTrack API

**Dernière mise à jour** : 2026-08-01
**Sources de vérité** : `docs/srs.md` v1.3, `docs/sdd.md` v1.2, `docs/adr/`, `CLAUDE.md`

Document opérationnel de suivi. Le SRS reste le contrat métier, le SDD la conception, les ADR les décisions structurantes. Ce plan agrège leur état d'avancement, ne le remplace pas.

---

## 1. Vue d'ensemble

**Phase courante** : socle sécurité stabilisé (Waves 1/2/3), chantier fonctionnel EX-F-02 livré. Prêt à attaquer EX-F-03 (statut d'alerte + event).

**Verrous en place** :
- SRS v1.3 conforme IEEE 830 / ISO 29148, aucune ambiguïté résiduelle.
- SDD v1.2 refactoré §9 en 3 états (SPEC / IMPL / TEST vert).
- Branch protection `main` : PR obligatoire, 3 checks requis (`mvn verify (api)`, `trivy fs scan`, `SonarCloud Code Analysis`).
- 6 règles ArchUnit maison verrouillant les invariants CLAUDE.md.
- Auto-delete des branches actives.

---

## 2. Exigences fonctionnelles (SRS §3)

| Id | Intitulé | Statut | PR | Notes |
|----|----------|--------|----|-------|
| EX-F-01 | Créer une parcelle | ✅ Livré | #10, #13, #14, #15 | Domaine + REST + JPA + Postman |
| EX-F-02 | Enregistrer un relevé | ✅ Livré | #29 | Option B : sans event ni statut recalculé |
| EX-F-03 | Déterminer le statut d'alerte | ⏳ À faire | — | Suivant. Dépend de EX-F-02 (livré), EX-F-07 |
| EX-F-04 | Exporter en CSV | 🔒 Feature flag off | — | Bloqué SEC-B-05 (adapter CSV à écrire au moment de l'implémentation) |
| EX-F-05 | Lister les parcelles paginées | ⏳ À faire | — | Peut être livré avant ou après EX-F-03 |
| EX-F-06 | Fiche parcelle (détail + historique) | ⏳ À faire | — | Dépend de EX-F-02 (livré) |
| EX-F-07 | Journal des alertes | ⏳ À faire | — | Couplé à EX-F-03 (event `StatutParcelleChange`) |

---

## 3. Exigences non fonctionnelles (SRS §4)

| Id | Domaine | Statut | Preuve |
|----|---------|--------|--------|
| EX-NF-01 | Performance P95 < 500 ms | 🟡 Partiel | Dénormalisation en place (ADR-005), test de charge non écrit |
| EX-NF-02 | Zéro coupure (expand/contract) | 🟡 Partiel | Migrations V3/V4 conformes ; sondes readiness/liveness activées (Wave 3), non testées en runtime |
| EX-NF-03 | Aucune perte d'alerte | ⏳ Bloqué EX-F-03 | Spring Modulith event registry (ADR-003) prêt, non exercé |
| EX-NF-04 | Version exposée | 🟡 Partiel | `/actuator/info` en place, header web à ajouter avec front |
| EX-NF-05 | RFC 7807 sans détails internes | ✅ Livré | Waves 1/2 + tests négatifs schema exposure |
| EX-NF-06 | Ergonomie / accessibilité | ⏳ Front absent | Attend module `web/` Next.js |
| EX-NF-07 | Taux et statut calculés par le système | ✅ Livré | `TauxDeSurvie.calculer()` + `fail-on-unknown-properties` empêche l'écriture |

Légende : ✅ complet / 🟡 partiel / ⏳ à faire / 🔒 bloqué

---

## 4. ADR

| Id | Titre | Statut | Notes |
|----|-------|--------|-------|
| ADR-001 | Spring Modulith 3 modules | Accepté | Vérifié par `ApplicationModules.verify()` |
| ADR-002 | Persistance JPA + Flyway | Accepté | `ddl-auto=validate` en place |
| ADR-003 | Events `releves → alertes` | Accepté | Sera exercé par EX-F-03 |
| ADR-004 | Feature flag export CSV | Accepté | Config `ecotrack.features.export-csv=false` |
| ADR-005 | Dénormalisation dernier relevé | Accepté | Livré via EX-F-02 (colonnes `dernier_taux`, `date_dernier_releve`) |
| ADR-006 | Plafonds export CSV | Accepté | 10 000 lignes max, actif à l'impl EX-F-04 |
| ADR-007 | Rétention et purges | ⚠️ Proposé | Bloque sur CLARIF-01 (contradiction EX-F-07 R1) |
| ADR-008 | DLQ event `StatutParcelleChange` | Proposé | À valider avec EX-F-03 en staging |
| ADR-009 | Journalisation applicative | Proposé | À implémenter avec un futur ADR-010 (tracing) |

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
| #29 | **EX-F-02** — enregistrer un relevé (Option B) | 2026-08-01 |

---

## 6. Chantier en cours

| PR | Sujet | Statut |
|----|-------|--------|
| #32 | **Wave 3** — config `application.yml` + validation boot | En review |

---

## 7. Issues ouvertes

| Id | Sujet | Priorité |
|----|-------|----------|
| #19 | Test négatif absence de champs internes dans `ProblemDetail` (SEC-04) | Moyenne — à traiter avant EX-F-07 |
| #30 | Unifier ou départager les 2 `@RestControllerAdvice HIGHEST_PRECEDENCE` | Moyenne — **avant EX-F-03** |
| #31 | Rate-limiting global (arbitrage n°1 auth) | Basse — attend décision auth |

---

## 8. Points ouverts / arbitrages requis

| Sujet | Bloque | Décideur | Notes |
|-------|--------|----------|-------|
| **CLARIF-01** — EX-F-07 R1 « jamais supprimé » vs ADR-007 rétention 24 mois | ADR-007 statut Accepté | Sponsor + analyste-srs | 3 options : lecture stricte / amender SRS / retirer purge |
| **Arbitrage n°1** — Authentification | EX-F-04 exposition, rate-limiting global | Sponsor | Hors périmètre v1 explicite mais à préparer v2 |
| **Seuil N=5** DLQ event (ADR-008) | ADR-008 statut Accepté | Observation staging | Sera validé après EX-F-03 + staging |

---

## 9. Dette technique tracée

| Élément | Origine | Prochaine action |
|---------|---------|------------------|
| Test smoke `/actuator/{env,beans,mappings,heapdump}` → 404 | Wave 3 §9.3 | Ajouter avec EX-F-03 (`@SpringBootTest` complet) |
| Validation regex W3C `traceparent` | SEC-I-04, Wave 3 §9.3 | Reporté à la PR Micrometer Tracing (ADR-009) |
| `@Pattern` sur `@PathVariable code` | Audit PR #29 SEC-FAIB-01 | Trivial, à faire au premier passage sur `RelevesController` |
| Historique relevés paginé (EX-F-06) | Anti-pattern SEC-B-03 (revue v2 §V-06) | À couvrir dans EX-F-06 avec keyset pagination |
| Endpoint `POST /admin/events/{id}/retry` | ADR-008 | À implémenter avec EX-F-03 (flag `ecotrack.admin.enabled` déjà en place) |

---

## 10. Roadmap courte (3 prochaines PR fonctionnelles)

| Ordre | PR | Contenu | Motif |
|-------|----|---------|-------|
| 1 | **Wave 3** (#32) | Config Actuator + validation boot | Ferme le chantier sécurité issu de la revue v2 |
| 2 | **Issue #30** | Unifier les 2 advices dans `shared/` | Nettoie le terrain avant EX-F-03 |
| 3 | **EX-F-03** | Statut d'alerte + event `StatutParcelleChange` + journal (EX-F-07) | Débloque EX-NF-03. Aussi : implémenter ADR-008 DLQ, valider N=5 en staging |
| 4 | **EX-F-05** | Lister les parcelles paginées | Peut être livré en parallèle si un contributeur front commence |
| 5 | **EX-F-06** | Fiche parcelle détail + historique | Débloque le parcours utilisateur complet |

---

## 11. Baseline sécurité prévue

Après merge de la Wave 3, lancer un **audit `security-reviewer` global sur `main`** pour établir la baseline post-Waves. Objectif : verrouiller les acquis avant EX-F-03 (nouveau module `alertes` = nouvelle surface).
