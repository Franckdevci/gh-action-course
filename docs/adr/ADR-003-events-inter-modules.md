# ADR-003 — Communication `releves` → `alertes` par event Spring Modulith

- **Statut** : Accepté
- **Date** : 2026-07-29
- **Exigences concernées** : EX-F-03 R2, EX-F-07, EX-NF-03

## Contexte

Lorsqu'un relevé fait basculer le statut d'une parcelle, une entrée doit être
créée au journal des alertes (EX-F-07). Le SRS impose de surcroît qu'**aucun
changement de statut accepté ne soit absent du journal, même en cas d'arrêt
brutal du système** immédiatement après l'enregistrement du relevé (EX-NF-03).

Trois options :

1. **Appel direct** : `releves` appelle l'API publique de `alertes`. Simple,
   transactionnel, mais crée une dépendance structurelle : `alertes` devient
   indispensable à l'enregistrement d'un relevé, et toute future réaction à
   l'alerte (notification email en v2) alourdira le module émetteur.
2. **Event Spring applicatif standard** (`ApplicationEventPublisher` +
   `@EventListener`) : découple les modules, mais l'event ne vit qu'en mémoire.
   Un crash entre le commit du relevé et le traitement de l'event perd
   définitivement l'entrée de journal — violation directe d'EX-NF-03.
3. **Event Spring Modulith** (`@ApplicationModuleListener` + *event publication
   registry*) : l'event est persisté en base dans la même transaction que le
   relevé, marqué comme traité seulement après exécution du listener, et
   **rejoué au redémarrage** s'il ne l'a pas été.

Une quatrième option, un middleware de messagerie (Kafka, RabbitMQ), a été
écartée d'emblée : elle introduit une infrastructure distribuée pour une
communication interne à un déployable unique.

## Décision

`releves` publie l'event `StatutParcelleChange` ; `alertes` le consomme via
`@ApplicationModuleListener` et crée l'entrée de journal. L'**event publication
registry** de Spring Modulith est activé (table de publications en base).

`releves` ne déclare **aucune dépendance** vers `alertes`.

## Conséquences

**Positives**
- EX-NF-03 est satisfaite par un mécanisme éprouvé plutôt que par du code
  maison : l'event survit au crash et est rejoué au démarrage.
- Découplage réel : `alertes` peut être modifié, désactivé ou extrait sans
  toucher à `releves`. Une v2 « notification email » s'abonne au même event —
  extension sans modification de l'émetteur.
- Le listener s'exécute **après le commit** de la transaction du relevé : une
  écriture au journal ne peut donc jamais correspondre à un relevé annulé.
- Testable finement : `@ApplicationModuleTest` avec `Scenario` permet de
  publier l'event et d'observer la réaction du module consommateur isolément.

**Négatives**
- Cohérence **éventuelle** entre le relevé et son entrée de journal : il existe
  une fenêtre (courte) où le relevé est enregistré et le journal pas encore
  écrit. L'interface doit tolérer cet écart — acceptable, le journal étant un
  outil de suivi et non une source de vérité transactionnelle.
- Une table technique de publications s'ajoute au schéma, avec une politique de
  purge à prévoir (les publications traitées s'accumulent).
- Le débogage d'un flux asynchrone est intrinsèquement moins direct qu'un appel
  de méthode ; la traçabilité repose sur les logs et la table de publications.
- En cas d'échec répété du listener, l'event reste non traité : une supervision
  des publications en attente sera nécessaire au-delà du staging.

## Alternatives rejetées

- **Appel direct** : rejeté pour couplage structurel et absence de garantie de
  reprise après crash.
- **Event Spring standard non persisté** : rejeté car il viole EX-NF-03 —
  précisément le point que l'audit du SRS avait rendu explicite.
- **Kafka / RabbitMQ** : rejeté, hors proportion pour une communication interne
  à un déployable unique. Réévaluable si des modules sont extraits en services.
