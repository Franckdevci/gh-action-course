# ADR-003 — Communication `releves` → `alertes` par event uniquement

- **Statut** : Acceptée
- **Date** : 2026-07-29
- **Référencée par** : SDD §2.2

## Contexte

Le module `releves` détermine le statut d'une parcelle à chaque enregistrement
(EX-F-03). Le module `alertes` tient un journal immuable des changements de
statut (EX-F-07). La question : `releves` doit-il appeler `alertes` en direct
pour lui demander d'écrire au journal, ou publier un event auquel `alertes`
s'abonne ?

Contraintes en présence :
- EX-NF-03 : aucun changement de statut accepté ne doit être absent du
  journal, y compris après un crash immédiatement après l'enregistrement du
  relevé. La transactionalité de l'écriture au journal doit être garantie.
- La v2 prévoit des notifications externes (email/SMS) — SRS §1.2 hors
  périmètre v1. Le journal des alertes est explicitement désigné comme
  « point d'ancrage prévu pour une v2 ».

## Décision

`releves` publie un event **`StatutParcelleChange`** à chaque transition de
statut. `alertes` s'abonne à cet event et écrit au journal. **Aucune
dépendance directe** entre `releves` et `alertes` : le graphe de dépendances
des modules ne contient pas d'arête `releves → alertes`, seulement une flèche
en pointillé pour l'event.

L'event est un `record` immuable publié par `releves` dans son API publique :

```java
public record StatutParcelleChange(
    ParcelleId parcelleId,
    CodeParcelle code,
    StatutParcelle ancienStatut,
    StatutParcelle nouveauStatut,
    TauxDeSurvie tauxDeclencheur,
    LocalDate dateReleve,
    Instant survenuLe) {}
```

La publication utilise le **mécanisme Spring Modulith d'event publication
registry**, qui garantit qu'un event publié dans la transaction du relevé
sera livré au listener même en cas de crash entre le commit du relevé et le
traitement du listener (reprise au redémarrage). C'est ce mécanisme qui
réalise EX-NF-03.

## Conséquences

- **Positives** :
  - `alertes` peut disparaître, être remplacé, ou évoluer sans que `releves`
    en ait connaissance — pas de recompilation croisée.
  - La v2 « notification email » s'abonne au **même** event sans toucher au
    module émetteur. On ajoute un module `notifications`, on lui abonne
    `StatutParcelleChange`, `releves` reste inchangé.
  - Le graphe de dépendances reste acyclique. `ApplicationModules.verify()`
    (cf. ADR-001) le vérifie à chaque build.
  - La reprise transactionnelle du registry rend EX-NF-03 réalisable sans
    inventer de queue externe pour trois modules colocalisés.
- **Négatives** :
  - Un développeur qui débug un problème « pourquoi le journal ne s'écrit
    pas » doit connaître le mécanisme d'events (le call graph statique ne le
    dit pas). Le SDD documente ce point et le test
    `should_journaliser_alerte_when_crash_apres_enregistrement` (§7.2) en
    est l'exemple exécutable.
  - Le registry Spring Modulith exige une table dédiée (`event_publication`)
    en base — coût opérationnel mineur.

## Alternatives considérées

- **Appel direct `releves.enregistrer()` → `alertes.journaliser()`** :
  rejeté. Cycle inévitable dès que `alertes` a besoin de relire un champ de
  la parcelle. Couplage fort qui rend impossible l'ajout de la
  v2 notifications sans modifier `releves`. `ApplicationModules.verify()`
  rejetterait la structure.
- **Bus externe (Kafka / RabbitMQ)** : rejeté pour la v1. Coût opérationnel
  disproportionné pour un monolithe modulaire. Reviendra en question si un
  jour un module est extrait en microservice (cf. ADR-001), auquel cas la
  migration se limite à changer le transport de l'event — la forme du
  contrat reste identique.
