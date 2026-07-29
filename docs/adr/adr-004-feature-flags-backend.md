# ADR-004 — Feature flags : backend seul dépositaire

- **Statut** : Acceptée
- **Date** : 2026-07-29
- **Référencée par** : SDD §5.3

## Contexte

EX-F-04 exige que l'export CSV soit placé derrière un feature flag :
désactivé, la fonctionnalité doit être **absente du système du point de vue
de l'utilisateur** (aucun bouton, aucun endpoint accessible). Contrainte C2
du SRS : cette activation doit se faire à l'exécution, sans redéploiement.

Deux options : chaque conteneur (`api` et `web`) lit sa propre configuration
de flags, ou le backend est la seule source de vérité et le front interroge
un endpoint dédié pour connaître l'état des flags publics.

## Décision

Le **backend est le seul dépositaire** de la configuration des flags. Il
expose `GET /api/v1/config` qui renvoie l'état des flags publics
(`{ "exportCsv": true }` par exemple). Le front interroge cet endpoint au
démarrage de session et conditionne l'affichage du bouton d'export à sa
réponse.

Le flag est configurable par variable d'environnement
(`ECOTRACK_FEATURES_EXPORT_CSV`), défaut `false`. La bascule ne nécessite
qu'un rolling update de l'`api` — pas de rebuild du front, pas de
redéploiement coordonné.

## Conséquences

- **Positives** :
  - Une seule source de vérité. Impossible que `web` affiche un bouton
    « Export » alors que `api` répond 404 sur `/parcelles/export.csv`, ou
    l'inverse. Le contrat entre les deux passe par HTTP, pas par une
    convention parallèle sur les valeurs de flags.
  - Activation à chaud effective : changer la valeur, redéployer `api`, le
    front la prend en compte à la prochaine session (ou au prochain appel de
    `/config` selon la stratégie de cache).
  - Pas de duplication de la logique de lecture des flags dans les deux
    stacks (une lib côté Java, une côté TypeScript).
  - Un flag *serveur uniquement* (pour restreindre l'accès à un endpoint sans
    changer l'UI) reste possible : il n'est simplement pas exposé par
    `/config`.
- **Négatives** :
  - Un aller-retour HTTP supplémentaire à l'initialisation de session.
    Négligeable (payload de quelques octets, cache HTTP possible).
  - Le front doit gérer un état « configuration pas encore chargée » — c'est
    déjà le cas via les `loading.tsx` de l'App Router (EX-NF-06).

## Alternatives considérées

- **Configuration du front en variables `NEXT_PUBLIC_*`** : rejeté. Un
  changement de flag exigerait un rebuild + redéploiement de `web`,
  contrairement à C2 (activation à l'exécution). Et deux sources de vérité
  peuvent diverger silencieusement.
- **Lecture directe d'une base de configuration (Consul, LaunchDarkly)
  depuis les deux stacks** : rejeté pour la v1. Dépendance externe, coût
  opérationnel, et rien qui ne pourrait pas être fait quand le besoin
  arrivera (rétrocompatible : `/api/v1/config` peut, en v2, être alimenté
  par LaunchDarkly sans changer le contrat vu du front).
