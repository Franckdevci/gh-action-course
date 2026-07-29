# ADR-001 — Spring Modulith plutôt que microservices ou monolithe en couches

- **Statut** : Accepté
- **Date** : 2026-07-29
- **Exigences concernées** : toutes (décision structurante) — en particulier EX-NF-02

## Contexte

EcoTrack v1 couvre trois préoccupations métier distinctes (référentiel des
parcelles, relevés et calcul du statut, journal des alertes) pour un acteur
unique et une volumétrie modeste (≤ 5 000 parcelles, ≤ 20 utilisateurs
simultanés — SRS H3). Le projet est développé par une seule personne, avec
un environnement de staging unique.

Trois options étaient sur la table :

1. **Monolithe en couches** (`controller` / `service` / `repository`) : simple
   au départ, mais les frontières métier n'existent qu'en intention. Dans ce
   style, rien n'empêche un service de `releves` d'appeler un repository de
   `parcelles` — la dette de couplage s'accumule sans signal.
2. **Microservices** (un service par préoccupation) : frontières garanties par
   le réseau, mais coût immédiat considérable — trois déployables, cohérence
   distribuée, observabilité distribuée, latence inter-services. Pour trois
   modules et un développeur, ce coût n'achète aucun bénéfice réel : ni
   scaling différencié, ni équipes indépendantes.
3. **Monolithe modulaire vérifié** (Spring Modulith) : un déployable unique,
   des modules aux frontières explicites, et surtout **vérifiables par test**.

## Décision

Nous adoptons **Spring Modulith** : un déployable unique découpé en modules
(`parcelles`, `releves`, `alertes`, plus `shared` en open module), dont les
frontières sont vérifiées par `ApplicationModules.verify()` exécuté dans le
pipeline de PR.

Les modules ne communiquent que par **API publique** (type exposé à la racine
du package du module) ou par **event** (cf. ADR-003). L'accès aux internes
d'un autre module fait échouer le build.

## Conséquences

**Positives**
- Les frontières cessent d'être une convention documentaire : elles deviennent
  un check du pipeline, au même titre que les tests ou le Quality Gate.
- Coût opérationnel d'un monolithe : une image, une base, une transaction
  locale, un déploiement — compatible avec l'exigence de zéro coupure
  (EX-NF-02) sans orchestration distribuée.
- Chemin d'évolution ouvert : un module dont les frontières sont déjà propres
  et qui communique par event peut être extrait en service plus tard, sans
  réécriture du reste. La décision de découper devient une décision de
  déploiement, pas une refonte.
- La documentation d'architecture (`Documenter`) est générée depuis le code
  réel : toute divergence avec le SDD est détectable.

**Négatives**
- Pas de scaling indépendant : un pic sur les relevés fait scaler tout le
  déployable. Acceptable à la volumétrie H3 ; à réévaluer au-delà.
- Une panne applicative affecte toutes les fonctions à la fois.
- Discipline requise : la tentation d'un raccourci entre modules existe à
  chaque instant — c'est précisément ce que `verify()` neutralise.
- Dépendance à Spring Modulith (framework relativement jeune) pour la
  vérification ; en cas d'abandon, la règle redeviendrait déclarative.

## Alternatives rejetées

- **Microservices d'emblée** : rejeté pour coût opérationnel disproportionné à
  l'échelle du projet. Le découpage prématuré fige des frontières qu'on connaît
  mal en début de projet — le plus difficile à corriger.
- **Monolithe en couches** : rejeté car les frontières métier y sont
  invérifiables ; le couplage inter-domaines s'y installe silencieusement.
