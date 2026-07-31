# ADR-008 — Stratégie DLQ et rejeu de l'event `StatutParcelleChange`

- **Statut** : Proposé
- **Date** : 2026-07-31
- **Exigences concernées** : EX-NF-03, EX-F-07
- **Relation** : **Complète ADR-003** (ne le remplace pas). ADR-003 acte la
  communication par event Modulith et le rejeu au redémarrage ; ADR-008
  traite la résilience en cas d'**échec persistant** du consommateur, angle
  mort d'ADR-003.

## Contexte

ADR-003 décide que le module `alertes` consomme l'event
`StatutParcelleChange` via `@ApplicationModuleListener`, et que
l'**event publication registry** de Spring Modulith persiste chaque
publication non traitée pour la rejouer au redémarrage. Cette mécanique
garantit EX-NF-03 (« aucune alerte perdue en cas de crash »).

Elle laisse ouverte une question que la revue SDD (mode 3) a levée : que se
passe-t-il si le consommateur `alertes` **échoue de façon persistante** — bug
logique, contrainte SQL violée par une donnée particulière, colonne ajoutée
en amont mais mapping incomplet côté journal, etc. ?

Le comportement actuel (registry seul, sans stratégie explicite) présente
deux dérives possibles :

- **Boucle de rejeu infinie** : à chaque redémarrage, l'event reste marqué
  non traité et Spring Modulith retente ; le consommateur échoue de nouveau,
  et ainsi de suite. L'incident reste invisible tant que personne ne
  consulte le registry.
- **Dérive silencieuse** : l'event non traité s'accumule au fil des
  publications suivantes ; la table de registry grossit ; la métrique
  `publications_non_traitees` (ADR-007) peut ne monter que légèrement,
  masquée par le bruit — le journal des alertes se remplit partiellement
  mais pas totalement.

Aucune des deux n'est acceptable au regard d'EX-NF-03, dont l'intention
est que **toute alerte accepté finit par être journalisée**. Un event
bloqué en boucle n'est pas plus tenable qu'un event perdu : dans les deux
cas le journal est incomplet.

Trois angles d'action, non exclusifs :

1. **Compteur d'échecs par event** : distinguer un rejeu qui a de bonnes
   chances de succès (bug transitoire) d'un rejeu qui a échoué N fois
   d'affilée (bug persistant).
2. **État terminal `FAILED`** : au-delà de N tentatives, l'event cesse
   d'être rejoué automatiquement, sort de la boucle et devient un incident
   nommé, visible et rejouable **explicitement**.
3. **Rejeu manuel** : endpoint d'exploitation qui remet un event en état
   « à rejouer » une fois la cause corrigée.

## Décision

Nous mettons en place la stratégie suivante en complément d'ADR-003 :

- **Compteur d'échecs** persisté par event dans le registry (colonne
  `attempts` ou équivalent, selon le schéma Spring Modulith). Incrémenté à
  chaque exception non maîtrisée du listener.
- **Seuil de bascule** : **N = 5** tentatives. Au-delà, l'event bascule en
  état terminal **`FAILED`** et **n'est plus rejoué automatiquement** ni au
  démarrage ni sur une nouvelle publication. Justification du seuil : un
  bug transitoire (contention base, timeout réseau, redéploiement en
  cours) se résout typiquement en 1 à 3 tentatives ; 5 laisse une marge
  généreuse sans autoriser une boucle indéfinie. Modifiable par propriété
  `ecotrack.events.max-attempts`.
- **Endpoint d'exploitation** : `POST /admin/events/{id}/retry` — remet un
  event `FAILED` à l'état « à rejouer » et déclenche une nouvelle
  tentative. Endpoint protégé par un mécanisme d'exploitation distinct de
  l'API métier (hors périmètre de l'auth v1 « aucune », cf. arbitrage n°1
  du SRS ; en v1 solo, restriction réseau et non exposé publiquement).
- **Métriques** :
  - `events_failed_total` (Micrometer, `Counter`) — incrémenté à chaque
    bascule en `FAILED`.
  - `events_attempts_histogram` (`DistributionSummary`) — distribution du
    nombre de tentatives par event traité.
  - **Alerte oncall** : `events_failed_total > 0` sur les 5 dernières
    minutes déclenche une notification. Aucun `FAILED` n'est acceptable en
    exploitation normale.
- **Table DLQ dédiée écartée** : l'état `FAILED` sur la table Modulith
  suffit ; ajouter une table redondante compliquerait la reprise sans
  ajouter d'information.

## Conséquences

**Positives**
- EX-NF-03 renforcée : le silence d'un event bloqué devient bruyant
  (métrique + alerte + endpoint de reprise). L'exigence « aucune alerte
  perdue » est **opérationnalisée**, pas seulement inscrite.
- La boucle de rejeu infinie est bornée : au bout de 5 tentatives, la
  situation est nommée (`FAILED`) et sort de la mécanique automatique.
- L'endpoint de reprise permet une correction en trois temps :
  1. corriger la cause (donnée corrompue, bug, migration) ;
  2. appeler `POST /admin/events/{id}/retry` ;
  3. vérifier que la métrique retombe à zéro.
- La supervision existante d'ADR-007
  (`publications_non_traitees`) et celle-ci (`events_failed_total`) sont
  **complémentaires** : la première voit les events en cours de
  traitement ou en attente, la seconde voit les events abandonnés.

**Négatives**
- Un event `FAILED` **ne journalise pas d'alerte** tant qu'il n'est pas
  rejoué avec succès. Fenêtre de non-conformité temporaire à EX-F-07 : le
  changement de statut existe côté `parcelles` mais son entrée journal
  reste en attente. La responsabilité passe à la supervision (l'alerte doit
  se déclencher rapidement) et à l'exploitant (correction et rejeu).
- L'endpoint `/admin/events/{id}/retry` **doit être protégé**. En v1 sans
  auth, la seule protection est réseau (pas d'exposition publique, filtre
  IP, ou activation par flag `ecotrack.admin.enabled=false` par défaut).
  À réévaluer à l'ajout de l'authentification (arbitrage n°1 du SRS).
- Le seuil N=5 est arbitraire. Trop bas : bascule sur un incident
  transitoire, exploitant réveillé pour rien. Trop haut : la boucle infinie
  dure trop longtemps. La valeur est un paramètre, ajustable après
  observation en staging.
- L'implémentation dépend des internes du registry Modulith
  (colonne `attempts`, état `FAILED`). Si le framework ne l'expose pas
  nativement, un adapter maison sur la table de registry est nécessaire —
  fragile à l'évolution de version.

**Neutres**
- La stratégie est indépendante du contenu de l'event : elle s'applique de
  la même façon à `StatutParcelleChange` et à tout futur event
  inter-modules.

## Alternatives rejetées

- **Rejeu infini sans borne** : rejeté. Dérive silencieuse : le registry
  gonfle, la métrique se dilue, l'incident reste invisible. Contraire à
  l'esprit d'EX-NF-03.
- **Table DLQ dédiée** (Spring Modulith registry pour les events actifs +
  table `dead_letter_events` pour les échecs) : rejetée. Redondance
  d'information et de logique de purge ; l'état `FAILED` sur la table
  Modulith porte la même sémantique avec moins de complexité.
- **Abandon silencieux au bout de N échecs** (event supprimé, aucune trace) :
  rejeté. Viole EX-NF-03 : l'alerte est perdue sans qu'un opérateur en
  soit informé.
- **Retry avec backoff exponentiel infini** (1s, 2s, 4s, 8s, …) : rejeté
  seul. Le backoff est une bonne pratique **avant** le seuil terminal,
  mais ne remplace pas la bascule en `FAILED` : au bout de plusieurs
  heures, l'incident reste actif sans intervention humaine possible.
  Peut être combiné en évolution ultérieure (backoff avant le compteur
  d'échecs).
- **Alerte email au consommateur d'origine du relevé** : rejeté, hors
  périmètre v1 (pas d'auth, pas de notifications externes — arbitrages
  SRS §7).

## Références

- EX-NF-03 (aucune alerte perdue), EX-F-07 (journal des alertes),
  EX-NF-06 (observabilité implicite) du SRS.
- SDD §4.3 (contrat évènementiel), §8 (observabilité).
- ADR-003 (event Modulith + rejeu au redémarrage) — cet ADR complète le
  mécanisme.
- ADR-007 (rétention et supervision du registry) — la métrique
  `publications_non_traitees` couvre les events en attente ; la métrique
  `events_failed_total` couvre les events abandonnés.
- ADR-009 proposé (journalisation structurée) — les logs de rejeu et de
  bascule `FAILED` suivent le format JSON structuré défini là.
- Spring Modulith Reference (event publication registry).
