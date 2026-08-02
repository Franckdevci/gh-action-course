---
description: Rédige une ADR au format du projet, avec ses conséquences négatives
argument-hint: <sujet de la décision>
---

Tu vas rédiger une **Architecture Decision Record** sur : **$1**

## Avant de rédiger

1. Liste `docs/adr/` pour déterminer le **prochain numéro** disponible.
2. Lis les ADR existantes : cette décision en **contredit-elle** une ?
   Si oui, la nouvelle ADR doit la **superséder explicitement** — on ne
   réécrit jamais une ADR passée, on l'annote comme supersédée.
3. Lis `docs/srs.md` et `docs/sdd.md` pour rattacher la décision aux exigences
   concernées.

Si le sujet ne relève pas d'une décision structurante (choix réversible sans
coût, détail d'implémentation), **dis-le-moi** au lieu de produire une ADR
inutile. Une ADR par micro-choix dévalue les vraies.

## Format imposé

Fichier `docs/adr/ADR-0NN-<slug>.md` :

```markdown
# ADR-0NN — <titre : la décision, pas le sujet>

- **Statut** : Proposé | Accepté | Supersédé par ADR-0MM
- **Date** : <date du jour>
- **Exigences concernées** : EX-x-xx, …

## Contexte

Le problème réel, les forces en présence, les contraintes. Ce qui rend la
décision nécessaire MAINTENANT. Présente les options envisagées de façon
honnête — y compris celles qu'on va écarter.

## Décision

Ce qui est décidé, formulé à l'affirmative et sans ambiguïté.

## Conséquences

**Positives** — ce que ça nous apporte, concrètement.

**Négatives** — ce que ça nous coûte : dette, contrainte, risque, complexité.
Cette section est OBLIGATOIRE et doit être substantielle.

## Alternatives rejetées

Chaque option écartée, avec la raison précise du rejet.
```

## Exigence de qualité

**Une ADR qui ne liste que des avantages n'est pas une décision, c'est une
justification.** Si tu ne trouves aucune conséquence négative, c'est que tu
n'as pas assez creusé — ou que le choix était évident, auquel cas il ne
méritait pas d'ADR.

Signale aussi, quand c'est pertinent, **le déclencheur qui devrait faire
reconsidérer cette décision** (« si la volumétrie dépasse X », « si un second
consommateur apparaît »). Une décision datée avec son critère de péremption
vaut mieux qu'une décision présentée comme définitive.

À la fin, propose le commit `docs: ADR-0NN <titre>` et rappelle-moi de mettre
à jour `docs/sdd.md` si la conception change.
