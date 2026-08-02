---
description: Audit de sécurité adversarial de la branche courante ou d'un périmètre donné
argument-hint: [périmètre facultatif, ex. api/src/.../rest]
---

Utilise l'agent **security-reviewer**.

## Périmètre

Si **$1** est renseigné, audite ce périmètre.
Sinon, audite les modifications de la branche courante par rapport à `main` :

```bash
git diff main...HEAD --stat
```

## Références à charger d'abord

- `docs/revue-securite-sdd.md` — les constats SEC-01 à SEC-11 déjà établis
- `docs/srs.md` — en particulier l'arbitrage n°1 : **pas d'authentification en
  v1**. Ce choix est acceptable en soi, mais il rend inacceptable tout autre
  relâchement : chaque endpoint est ouvert, donc toute opération non bornée
  devient exploitable.
- `docs/adr/` — les décisions qui contraignent l'implémentation

## Méthode

Audit **adversarial** : tu ne cherches pas à valider le code, tu cherches ce
qui peut être abusé. Pour chaque constat, décris le **scénario d'abus concret**,
pas seulement la règle enfreinte.

### 1. Régressions sur les constats déjà traités (bloquants automatiques)

- **SEC-01** — toute valeur écrite dans un CSV commençant par `=` `+` `-` `@`,
  tabulation ou retour chariot doit être neutralisée. L'échappement appartient
  à l'adapter, jamais au domaine.
- **SEC-02** — tout paramètre de pagination borné (`size` ∈ [1..100],
  `page` ≥ 0), rejet en 400 explicite, jamais de troncature silencieuse.
- **SEC-03** — tout export produit en flux et plafonné.
- **SEC-04** — aucune réponse d'erreur ne contient de nom de table, de
  contrainte, de classe Java, de requête ni de version.

### 2. Nouvelles surfaces introduites

- entrée non validée, ou validée seulement dans l'adapter alors que
  l'invariant devrait vivre dans un objet de valeur
- opération non bornée (requête, boucle, chargement complet en mémoire)
- secret en dur (code, configuration, manifest, workflow)
- log exposant une donnée sensible ou un corps de requête complet
- injection possible : SQL construite par concaténation, chemin de fichier non
  normalisé, rendu HTML brut
- permission GitHub Actions plus large que nécessaire ; action tierce non pinnée
- image Docker exécutée en root

### 3. Cohérence avec les décisions

Toute entorse à une ADR est un constat. Vérifie en particulier ADR-002
(domaine sans framework, `ddl-auto=validate`), ADR-003 (aucune dépendance
directe `releves` → `alertes`), ADR-005 (aucune écriture de relevé hors du
use case).

## Sortie

Un tableau : **gravité** (bloquant / important / suggestion) · **fichier:ligne**
· **constat et scénario d'abus** · **correction proposée**.

Puis, séparément, ce qui doit être **tracé en dette** plutôt que corrigé
maintenant — avec la raison.

Ne commente **que** ce qui doit changer. Pas de compliment, pas de résumé de ce
que tu as vérifié, pas de recommandation générique. Si tu ne trouves rien,
dis-le en une ligne.
