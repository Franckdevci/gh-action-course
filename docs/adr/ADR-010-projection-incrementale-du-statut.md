# ADR-010 — Projection incrémentale du statut de parcelle

- **Statut** : Accepté
- **Date** : 2026-08-02
- **Exigences concernées** : EX-F-03, EX-F-02, EX-NF-01, EX-NF-07
- **Complète** : ADR-005 (dénormalisation du dernier relevé)

## Contexte

Le SRS EX-F-03 impose que le statut d'une parcelle soit `EN_ALERTE` si et
seulement si le taux de survie de son **dernier relevé** (au sens de la
`dateObservation`) est strictement inférieur à 60 %, sinon `EN_SUIVI` —
y compris sans aucun relevé.

Le SDD initial §3.3 esquissait cette règle sous forme d'un service de
domaine `RegleStatutAlerte.evaluer(List<Releve>)` qui recalculait le statut
à partir de tout l'historique à chaque enregistrement de relevé. Cette
conception avait le mérite d'exprimer la règle métier en une méthode pure,
testable en isolation.

Elle a cependant été **livrée sans être connectée** au reste du système :
`Parcelle.enregistrerDernierReleve` a été implémentée avec sa propre logique
de projection incrémentale (mise à jour du statut in-place à partir du seul
nouveau relevé, filtrage des antidatés par comparaison à la
`dateDernierReleve` mémorisée). Un audit a mis en évidence que
`RegleStatutAlerte.evaluer` n'était appelée par aucun composant productif —
uniquement par ses propres tests, qui verrouillaient donc un chemin qui ne
s'exécute jamais en production. Illusion de couverture.

Trois options se présentaient :

1. **Câbler `RegleStatutAlerte` dans le flux principal.** Cela imposerait de
   relire tout l'historique des relevés d'une parcelle à chaque écriture,
   pour évaluer la règle sur la liste complète.
2. **Garder la logique dans `Parcelle` et supprimer `RegleStatutAlerte`.**
   Reconnaître que la projection incrémentale est la bonne conception —
   parce qu'elle bénéficie de la dénormalisation portée par ADR-005 — et
   que le service de domaine était une erreur de spec.
3. **Garder les deux, avec un test qui vérifie leur équivalence.** Deux
   sources de vérité maintenues en parallèle, à jour par pure discipline.

## Décision

Option 2 retenue.

Le statut est projeté de manière **incrémentale** sur l'agrégat `Parcelle`,
à chaque écriture d'un relevé, via `Parcelle.enregistrerDernierReleve` :

- Si le nouveau relevé est plus récent que le dernier connu (comparaison
  sur `dateObservation`), l'agrégat met à jour `dernierTaux`,
  `dateDernierReleve` et `statut`.
- Si le nouveau relevé est antidaté (date ≤ dernier connu), l'agrégat
  ne modifie rien et retourne `Optional.empty()`.
- La décision « ce taux est-il critique ? » reste dans le VO
  `TauxDeSurvie.estCritique()` — source unique du seuil 60 %, invariant
  BigDecimal exact.

Le service `RegleStatutAlerte` et ses tests sont supprimés. Les quatre
tests non-négociables du SDD §7.2 (`should_passer_en_alerte_when_taux_est_5995_pourcent`,
`should_rester_en_suivi_when_taux_exactement_60_pourcent`,
`should_ignorer_releve_antidate_when_determine_statut`,
`should_rester_en_suivi_when_aucun_releve`) sont conservés sous leurs noms
canoniques et déplacés sur `ParcelleTest` — c'est-à-dire sur le chemin
réellement exécuté en production.

## Hypothèse d'irrévocabilité (importante)

La projection incrémentale n'est **correcte que sous l'hypothèse suivante**,
vraie en v1 mais jamais écrite jusqu'ici :

> **Un relevé, une fois enregistré, ne peut plus être ni supprimé, ni
> modifié, ni remplacé.**

Cette hypothèse est garantie en v1 par :
- SRS EX-F-02 R3 : « au plus un relevé par parcelle et par date
  d'observation ; un second relevé à la même date est rejeté » — verrouillé
  par la contrainte `UNIQUE(parcelle_id, date_observation)` en base.
- Absence de tout endpoint `PUT /releves/{id}`, `DELETE /releves/{id}` ou
  correction rétroactive — hors périmètre v1.
- Aucun processus batch de correction de données.

Aussi longtemps que ces trois conditions tiennent, la projection
incrémentale et l'évaluation batch d'un `RegleStatutAlerte(List<Releve>)`
produiraient exactement le même statut sur les quatre cas non-négociables
et sur toute séquence d'enregistrements.

## Conséquences

**Positives**

- Une seule source de vérité pour le statut : l'agrégat `Parcelle`, mis à
  jour dans la même transaction que le relevé (cohérence immédiate, cf.
  ADR-005). Pas d'écart entre ce que la règle « déciderait » et ce qui est
  écrit en base.
- Une seule requête d'écriture par relevé enregistré (`INSERT releve` +
  `UPDATE parcelle`), pas de re-lecture de l'historique. Le bénéfice
  d'ADR-005 sur EX-NF-01 est intégralement conservé.
- La décision reste **testable en unité pure** : le seuil est enfermé dans
  `TauxDeSurvie.estCritique()` (tests dédiés dans `TauxDeSurvieTest`), et
  les quatre cas non-négociables sont vérifiés sur l'agrégat
  (`ParcelleTest`).
- Suppression d'environ 100 lignes de code mort et de leurs tests.
  L'audit devient plus honnête : ce qui est vert couvre ce qui tourne.

**Négatives**

- La projection incrémentale **dépend de l'ordre d'insertion** : si un
  antidaté est enregistré avant le vrai dernier relevé (par exemple à la
  reprise d'une importation en désordre), le statut passe transitoirement
  par une valeur incohérente avant d'être corrigé par l'insertion suivante.
  Résultat final identique, chemin intermédiaire non déterministe. Dans la
  fenêtre inter-insertions, un lecteur concurrent peut lire un statut qui
  sera écrasé quelques millisecondes plus tard. Acceptable en v1 (pas
  d'importation batch, saisie interactive uniquement).
- La règle « le statut est le taux du dernier relevé par date » n'est
  plus exprimée dans une seule expression déclarative — elle est diffuse
  entre la vérification `!dateObservation.isAfter(this.dateDernierReleve)`
  et l'affectation qui suit. Un lecteur qui découvre le code doit lire
  ensemble ces sept lignes pour comprendre. Compensé par les quatre tests
  canoniques et cette ADR.
- Toute évolution qui casse l'hypothèse d'irrévocabilité **invalide
  silencieusement** la projection : le champ `parcelle.statut` refléterait
  encore le taux du dernier relevé *enregistré*, plus nécessairement celui
  du dernier relevé *par date d'observation*. Cf. déclencheur de
  reconsidération ci-dessous.
- La logique de statut ne peut plus être invoquée sans avoir un agrégat
  chargé en mémoire, ce qui rend impossible un « recalcul depuis zéro »
  utilitaire (par exemple pour reconstruire les colonnes dénormalisées
  après une migration de données). Il faudrait réintroduire l'équivalent
  d'un `RegleStatutAlerte.evaluer(List<Releve>)` — à ce moment-là,
  volontairement.

## Déclencheur de reconsidération

**Cette ADR doit être rouverte immédiatement dès que l'une des évolutions
suivantes est envisagée :**

1. **Suppression d'un relevé** (endpoint `DELETE /parcelles/{code}/releves/{id}`,
   correction administrative, tâche de purge sélective).
2. **Modification rétroactive d'un relevé** (endpoint `PUT`, correction du
   nombre de plants vivants ou de la date d'observation après enregistrement).
3. **Import batch de relevés antidatés en volume** (migration depuis un
   système existant, reprise de saisies terrain différées).
4. **Besoin d'un utilitaire de recalcul** de la colonne `parcelle.statut`
   (audit interne, migration de données, reconstruction après incident).

Dans chacun de ces quatre cas, la projection incrémentale seule ne suffit
plus. Réintroduire une évaluation batch de la règle (`evaluer(List<Releve>)`)
— soit dans un service utilitaire, soit comme moteur de recalcul appelé
après chaque écriture — redevient nécessaire. Cette ADR sera alors
supersédée par une décision qui expliquera comment concilier les deux.

## Alternatives rejetées

- **Recharger tout l'historique des relevés à chaque écriture pour évaluer
  `RegleStatutAlerte.evaluer(List<Releve>)`.** Fidèle au SDD initial §3.3
  et conceptuellement plus pur (une seule expression déclarative de la
  règle), mais **contredit ADR-005** : le point même de la dénormalisation
  était d'éviter la lecture des relevés à chaque affichage ou à chaque
  écriture. Coût : une requête `SELECT * FROM releve WHERE parcelle_id = ?`
  par écriture, N+1 latent sur les écritures en série.

- **Garder les deux implémentations (projection incrémentale et service
  batch) avec un test croisé qui verrouille leur équivalence.** Deux
  sources de vérité en parallèle imposent la discipline de les modifier de
  concert. L'expérience de cette ADR démontre exactement le contraire :
  quand deux implémentations coexistent, l'une des deux finit orpheline
  et devient un piège de conception. Rejeté.

- **Bouger la règle dans un domaine service `parcelles.domaine.RegleStatutParcelle`
  appelé depuis `Parcelle.enregistrerDernierReleve`.** Reproduirait la
  situation actuelle (un service appelé une seule fois, sans utilité vs.
  code inline) sans le bénéfice de la découplage — puisque l'agrégat
  reste seul dépositaire de l'ordre d'écriture. Rejeté.
