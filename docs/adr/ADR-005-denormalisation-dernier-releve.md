# ADR-005 — Dénormalisation du dernier relevé sur la ligne `parcelle`

- **Statut** : Accepté
- **Date** : 2026-07-29
- **Exigences concernées** : EX-NF-01, EX-F-05, EX-F-04

## Contexte

Trois fonctions du SRS affichent, pour chaque parcelle, son **dernier taux de
survie** et la date de ce relevé : la liste paginée (EX-F-05), l'export CSV
(EX-F-04) et, indirectement, le tri par statut.

Le modèle de domaine sépare deux agrégats : `Parcelle` et `Releve` (référencé
par identité). En lecture naïve, afficher une page de 50 parcelles exige donc
une requête pour les parcelles, puis une requête de recherche du dernier relevé
**par parcelle** : le problème N+1 classique, soit 51 allers-retours en base
pour un seul écran — et le même schéma sur l'export, qui porte sur l'intégralité
du parc (jusqu'à 5 000 parcelles, SRS H3).

EX-NF-01 exige P95 < 500 ms sur la liste à cette volumétrie. Trois options :

1. **Jointure latérale / sous-requête corrélée** à la lecture : modèle intact,
   mais une requête non triviale, coûteuse à trier (le tri d'EX-F-05 R1 porte
   sur le statut, lui-même dérivé du dernier relevé) et difficile à paginer
   efficacement.
2. **Vue matérialisée** rafraîchie périodiquement : données potentiellement
   périmées, ce qui contredit l'affichage immédiat attendu après saisie.
3. **Dénormalisation** : porter `dernier_taux`, `date_dernier_releve` et
   `statut` sur la ligne `parcelle`, mis à jour à l'enregistrement d'un relevé.

## Décision

Nous dénormalisons : la table `parcelle` porte `statut`, `dernier_taux` et
`date_dernier_releve`. Ces colonnes sont mises à jour par le **use case
d'enregistrement d'un relevé**, dans la **même transaction** que l'insertion
du relevé, à partir de la règle de domaine `RegleStatutAlerte`.

Un index `parcelle(statut, code)` sert le tri par défaut d'EX-F-05 R1.

La lecture (liste, export) ne consulte **jamais** la table des relevés.

## Conséquences

**Positives**
- La liste paginée et l'export deviennent une requête unique et indexée :
  EX-NF-01 est atteignable sans optimisation ultérieure.
- Le tri « alertes d'abord, puis code croissant » (EX-F-05 R1) s'exécute
  directement sur un index, ce qui rend la pagination cohérente et stable.
- La cohérence est **immédiate**, pas éventuelle : même transaction que le
  relevé. Un rollback annule les deux écritures.
- La logique de calcul n'est pas dupliquée : les colonnes sont alimentées par
  la même règle de domaine que celle qui est testée unitairement.

**Négatives**
- **Redondance assumée** : la même information existe à deux endroits (le
  dernier relevé et sa projection sur la parcelle). Toute écriture de relevé
  qui contournerait le use case produirait une incohérence — d'où la règle
  stricte : aucune écriture de relevé hors du use case.
- Le cas du **relevé antidaté** exige une vigilance particulière : après
  insertion, le statut se recalcule à partir du relevé le plus récent **par
  date d'observation**, qui n'est pas nécessairement celui qu'on vient de
  saisir. Un test dédié couvre ce cas (SDD §7.2).
- Une correction manuelle en base ne suffirait pas : reconstruire ces colonnes
  exigerait un traitement de recalcul. Prévoir ce traitement si une migration
  de données devient nécessaire.
- Entorse à la normalisation stricte, à documenter pour tout nouvel arrivant —
  c'est l'objet de cette ADR.

## Alternatives rejetées

- **Sous-requête corrélée à la lecture** : rejetée pour le coût du tri et de la
  pagination sur une valeur dérivée, et le risque de dégradation avec le volume.
- **Vue matérialisée** : rejetée car un décalage d'affichage après saisie serait
  perçu comme un défaut par le gestionnaire.
- **Cache applicatif** : rejeté — déplace le problème (invalidation) sans
  garantir la cohérence immédiate, et fragilise le rolling update (deux
  instances, deux caches).
