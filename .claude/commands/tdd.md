---
description: Implémente une exigence du SRS en TDD strict, du domaine vers les adapters
argument-hint: <identifiant d'exigence, ex. EX-F-05>
---

Tu vas implémenter l'exigence **$1** du projet EcoTrack.

## Étape 1 — Explore (ne code RIEN à cette étape)

Lis dans cet ordre :
- `CLAUDE.md` (conventions et interdits)
- `docs/srs.md`, section **$1** : énoncé, règles métier, scénarios Gherkin
- `docs/sdd.md` : les sections qui concernent cette exigence (module, modèle de
  domaine, contrat d'API, stratégie de tests)
- `docs/adr/` : toute ADR qui contraint cette implémentation
- `docs/revue-securite-sdd.md` : les constats SEC-xx applicables

Puis produis un **plan TDD** :
- la liste des classes à créer ou modifier, par couche
  (`domaine/` → `application/` → `infrastructure/`)
- la liste des tests, dans l'ordre d'écriture, nommés `should_<résultat>_when_<condition>`
- pour chaque scénario Gherkin du SRS, le test correspondant
- les cas limites et cas d'erreur, explicitement
- ce qui est HORS du périmètre de cette exigence

Signale-moi tout point du SRS ou du SDD que tu trouves ambigu **au lieu de
choisir à ma place**.

Attends ma validation du plan avant de continuer.

## Étape 2 — Code (après validation seulement)

TDD strict, un cycle Red-Green-Refactor par comportement :
1. écris le test qui échoue,
2. **montre-le-moi rouge**,
3. écris le code minimal qui le fait passer,
4. refactorise sur du vert.

Ordre imposé — **du centre vers l'extérieur, jamais l'inverse** :
objets-valeur → agrégat → service de domaine → port → use case →
adapter sortant (JPA) → adapter entrant (REST).

Contraintes non négociables :
- aucun import `org.springframework` ni `jakarta.persistence` dans un package
  `..domaine..` (ArchUnit doit rester vert)
- `BigDecimal` pour tout taux, toute superficie, tout seuil — jamais `double` ;
  comparaison par `compareTo`, jamais `equals`
- `Clock` injectée, jamais `LocalDate.now()` en dur
- validation dans les objets de valeur, à la construction — pas un `isValid()`
- pagination bornée (max 100), erreurs RFC 7807 sans fuite de schéma

## Étape 3 — Vérification

```bash
cd api && mvn -B verify
```

Puis résume : quels tests ont été ajoutés, quels scénarios du SRS sont couverts,
et ce qui reste à faire pour compléter **$1**.

Propose le message de commit au format `feat($1): <description>` et le titre de PR.
