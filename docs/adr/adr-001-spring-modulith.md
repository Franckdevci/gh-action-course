# ADR-001 — Backend en Spring Modulith

- **Statut** : Acceptée
- **Date** : 2026-07-29
- **Référencée par** : SDD §2.1

## Contexte

EcoTrack v1 doit livrer un backend qui expose trois responsabilités clairement
identifiées dans le SRS : référentiel des parcelles (EX-F-01, EX-F-05, EX-F-06),
enregistrement des relevés et calcul du statut (EX-F-02, EX-F-03), journal des
alertes (EX-F-07). L'équipe est de petite taille, la volumétrie cible v1 est
modeste (≤ 5 000 parcelles, 20 utilisateurs concurrents — cf. H3 du SRS), et le
déploiement doit rester simple (une image, un rolling update — EX-NF-02).

Trois options s'offrent : monolithe en couches classiques (`controller` →
`service` → `repository`), microservices (un service par responsabilité), ou
monolithe modulaire vérifié à la compilation (Spring Modulith).

## Décision

Le backend est implémenté en **Spring Modulith**. L'application est un **seul
livrable** déployable, découpé en modules applicatifs (`parcelles`, `releves`,
`alertes`) plus un module technique partagé (`shared`). La règle
« un module ne connaît d'un autre que son API publique ou ses events » est
**transformée en test** par `ApplicationModules.verify()`, exécuté à chaque PR.

## Conséquences

- **Positives** :
  - Frontières claires du domaine, sans le coût opérationnel des microservices
    (pas de réseau interne à orchestrer, pas de sagas distribuées).
  - Les violations de frontière deviennent une erreur de build, pas une
    convention de revue de code — la règle ne peut pas dériver silencieusement.
  - Ouverture v2 immédiate : un module qui deviendrait critique en autonomie
    peut être extrait en microservice sans réécriture du modèle, ses
    dépendances étant déjà uniquement des API publiques et des events.
  - Une seule transaction JVM, une seule connexion pool, un seul pipeline de
    tests d'intégration.
- **Négatives** :
  - Discipline requise sur la structure interne des modules (§2.3 du SDD) : un
    accès direct à une entité d'un autre module casse la promesse.
  - Effet de mode : Modulith est encore jeune (2023+). On accepte le risque au
    vu du bénéfice de l'API `ApplicationModules.verify()` — irremplaçable dans
    les autres approches.
  - Nécessite Spring Boot ≥ 3.x et Java 21.

## Alternatives considérées

- **Monolithe en couches** : rejeté. Rien n'empêche `AlerteController`
  d'appeler `ParcelleRepository` en direct. La règle de découpage devient une
  convention orale, invérifiable, qui dérive au premier refactor pressé.
- **Microservices** : rejeté pour la v1. Coût opérationnel disproportionné
  pour la volumétrie H3 : trois services, trois pipelines, trois bases (ou une
  base partagée qui recrée un monolithe caché), transactions distribuées pour
  publier au journal des alertes suite à un relevé (EX-F-03 + EX-F-07). Le
  problème fonctionnel ne le justifie pas.
