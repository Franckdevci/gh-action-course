# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Nature du dépôt

Dépôt d'apprentissage GitHub Actions dont le projet fil rouge est **EcoTrack**, une application de suivi de parcelles de reboisement. Le dépôt est aujourd'hui en **phase docs-first** : il ne contient pas encore de code applicatif, seulement des documents de spécification et de conception. Toute automatisation CI/CD introduite dans ce dépôt s'applique à EcoTrack (build Java + Next.js, tests, déploiement).

## Sources de vérité (ordre de priorité)

1. `docs/srs.md` — **SRS v1.2 VALIDÉ** (ISO/IEC/IEEE 29148). Définit le **QUOI** : 7 exigences fonctionnelles (`EX-F-01` … `EX-F-07`), 7 exigences non fonctionnelles (`EX-NF-01` … `EX-NF-07`), 9 arbitrages entérinés (§7).
2. `docs/sdd.md` — **SDD v1.0 BROUILLON** (en attente de revue sécurité §9). Définit le **COMMENT** : architecture C4, découpage Modulith, agrégats DDD, contrat REST.
3. `docs/adr/` — décisions structurantes (référencées par le SDD sous `ADR-001`…`ADR-004`, à produire).

**Règle absolue** : toute fonctionnalité livrée doit être **tracée à une exigence** du SRS. Le message de PR et de commit référence l'identifiant (`EX-F-03`, `EX-NF-01`, …). Aucune exigence n'est orpheline (cf. matrice §8 du SDD).

## Convention de branches et de PR

- Branches : `docs/<sujet>-v<n>` pour les documents, `feat/EX-<id>-<slug>` pour l'implémentation d'une exigence, `fix/EX-<id>-<slug>` pour un correctif.
- La base par défaut est `main`. La branche `main` est **protégée** côté GitHub depuis le 2026-07-29 : PR obligatoire (push direct interdit), checks `mvn verify (api)` et `trivy fs scan` requis verts pour merger, historique linéaire imposé, `force-push` et suppression de branche interdits. `enforce_admins: false` — l'admin garde une échappatoire d'urgence.
- L'auteur d'une PR ne peut pas s'auto-approuver via l'API GitHub — la protection est donc réglée sur `required_approving_review_count: 0` (mono-mainteneur). Les revues internes sont postées en **commentaire**.
- Merge : **squash** + suppression de branche.

## Stack prévue (SDD §1.2 et §2)

- **Backend** : Java 21, Spring Modulith, Maven, PostgreSQL, Flyway (`ddl-auto=validate`).
- **Frontend** : Next.js (App Router), TypeScript, Tailwind, zod pour valider les réponses aux frontières.
- **Routage** : le navigateur ne parle qu'au conteneur `web` ; Next.js relaie `/api/*` vers `http://api:8080` (variable `API_INTERNAL_URL`). Aucune configuration CORS, aucune URL d'API exposée au client.
- **Erreurs API** : format **RFC 7807** (`application/problem+json`), aucun détail interne (EX-NF-05).

## Architecture backend (SDD §2)

Trois modules applicatifs + un module technique, communication par API publique **ou** par event, jamais par accès direct :

```
ci.ecotrack
├── parcelles/    # référentiel des parcelles, unicité du code, statut courant
├── releves/      # relevés, calcul du taux, détermination du statut
├── alertes/      # journal immuable des changements de statut
└── shared/       # types transverses (open module)
```

- `releves` → `parcelles` : dépendance directe assumée (API publique uniquement).
- `releves` → `alertes` : **event `StatutParcelleChange`** exclusivement. `alertes` peut disparaître sans casser `releves`.
- `ApplicationModules.verify()` transforme la règle en test dans le pipeline — ce n'est pas une convention, c'est un check.

Structure interne d'un module (hexagonal allégé) :

```
<module>/
├── <Module>Service.java   # API PUBLIQUE (seul point d'entrée)
├── domaine/               # Java pur, AUCUN import Spring/JPA
├── application/           # use cases + ports de sortie (interfaces)
└── infrastructure/
    ├── rest/              # adapter entrant
    └── jpa/               # adapter sortant : entité JPA + mapper explicite
```

Le domaine est testable sans contexte Spring, en millisecondes.

## Invariants d'implémentation critiques

Ces règles proviennent des cas limites du SRS et du SDD § 3.2. Les enfreindre casse le métier sans que les tests classiques ne le voient.

- **`TauxDeSurvie` est un `BigDecimal` d'échelle 4, jamais un `double`, jamais un pourcentage arrondi.** La comparaison au seuil de 60 % se fait sur cette valeur exacte (`valeur < 0.60`). L'arrondi à une décimale n'existe **que** dans la couche d'affichage (REST/UI). Conséquence assumée : une parcelle à 59,95 % passe en `EN_ALERTE` tout en affichant « 60,0 % ».
- **Le taux et le statut ne sont jamais saisis** ; ils sont calculés par le système (EX-NF-07). Aucun champ `statut` ou `taux` n'est acceptable en écriture sur l'API.
- **Le « dernier relevé » est le plus récent par `dateObservation`**, pas par ordre de saisie. Un relevé antidaté ne change le statut que s'il devient le plus récent (ce qu'il n'est pas dans le scénario standard).
- **Passage `EN_SUIVI` → `EN_ALERTE` ET rétablissement** sont tous deux consignés au journal (EX-F-03 R2, EX-F-07).
- **Contraintes en base, pas seulement en Java** : `UNIQUE(code)` sur `parcelle`, `UNIQUE(parcelle_id, date_observation)` sur `releve` — la base est le dernier rempart contre les écritures concurrentes.
- **Piège N+1 anticipé** : le dernier taux et la date du dernier relevé sont **dénormalisés sur la ligne `parcelle`**, mis à jour dans la même transaction que l'enregistrement du relevé. Justifié par EX-NF-01.
- **Migrations Flyway en règle `expand/contract`** obligatoire (rolling update = deux versions coexistantes, EX-NF-02).

## Stratégie de tests (SDD §7)

Pyramide et outils prévus :

| Niveau | Outil | Portée | Quand |
|---|---|---|---|
| Domaine pur | JUnit 5 | VO, `RegleStatutAlerte`, cas limites | PR |
| Architecture | `ApplicationModules.verify()` | frontières et cycles | PR |
| Module | `@ApplicationModuleTest` + `Scenario` | use cases, events | PR |
| Adapters | `@WebMvcTest`, `@DataJpaTest` | contrat REST, mapping, contraintes SQL | PR |
| Front unitaire | vitest + Testing Library | composants, états | PR |
| API e2e | RestAssured | scénarios Gherkin du SRS sur staging | post-déploiement |
| Navigateur | Playwright | parcours complets | post-déploiement |
| Charge | k6 / JMeter | EX-NF-01 (détail **et** liste) | post-déploiement |

**Tests non négociables** (issus des pièges du SRS, cf. SDD §7.2) — à écrire dès l'implémentation :

1. `should_passer_en_alerte_when_taux_est_5995_pourcent` (1199/2000, domaine pur).
2. `should_rester_en_suivi_when_taux_exactement_60_pourcent` (1200/2000).
3. `should_ignorer_releve_antidate_when_determine_statut`.
4. `should_rester_en_suivi_when_aucun_releve`.
5. `should_journaliser_alerte_when_crash_apres_enregistrement` (EX-NF-03, reprise de l'event au redémarrage).
6. `should_afficher_tiret_when_parcelle_sans_releve` (front).

## Commandes

Aucune commande de build/test n'existe encore — le code n'est pas encore introduit. À compléter dans ce fichier dès que Maven et npm arrivent au dépôt.

Commandes utiles côté dépôt aujourd'hui :

```bash
gh pr list --state open
gh pr view <n> --json state,mergedAt,mergedBy,mergeCommit
gh pr comment <n> --body "..."      # revue interne (auteur ne peut pas approuver sa propre PR)
gh pr merge <n> --squash --delete-branch
```

## Points ouverts en attente d'arbitrage

Ces points sont explicitement notés hors périmètre v1 ou en attente. **Ne pas les implémenter sans nouvelle décision** :

- Authentification et rôles (arbitrage n°1, hors périmètre v1).
- Notifications externes (email/SMS) — le journal des alertes est le point d'ancrage prévu pour une v2.
- Regarnissage (plants ajoutés après plantation) — v1 : plants initiaux immuables.
- Clôture/archivage de parcelle — v1 : parcelle indéfinie.
- Mobile natif et hors-ligne, multi-tenant, cartographie.
- **Revue sécurité SDD §9** en attente : absence d'auth, CSV injection sur `localite`, borne max de pagination, fuite de contrainte SQL dans les messages RFC 7807, rétention du journal des alertes.
