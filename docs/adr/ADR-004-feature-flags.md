# ADR-004 — Feature flags par propriétés Spring, exposés au front par l'API

- **Statut** : Accepté
- **Date** : 2026-07-29
- **Exigences concernées** : EX-F-04 R1, contrainte C2 du SRS

## Contexte

Le SRS exige (C2) que certaines fonctionnalités puissent être activées ou
désactivées à l'exécution sans redéploiement, et qu'une fonctionnalité
désactivée soit **absente du point de vue de l'utilisateur** (EX-F-04 R1 :
l'export CSV désactivé répond « ressource inexistante », il ne renvoie pas une
erreur d'autorisation ni un bouton inerte).

Cette contrainte sert un objectif de méthode : permettre le **trunk-based
development**, où du code incomplet peut être mergé dans `main` sans être
visible, plutôt que de maintenir des branches longues.

Deux questions distinctes :
1. Quel mécanisme côté backend ?
2. Comment le frontend sait-il si la fonctionnalité est active ?

## Décision

**Backend** : propriétés Spring (`ecotrack.features.export-csv`, défaut
`false`), lues via `@ConfigurationProperties` et évaluées à l'entrée de
l'adapter REST. Un endpoint dont le flag est désactivé répond **404**.

**Frontend** : aucune configuration propre. Le front interroge
`GET /api/v1/config`, qui expose les flags destinés à l'interface, et
conditionne l'affichage du bouton d'export à cette réponse.

**Une seule source de vérité : le backend.**

## Conséquences

**Positives**
- Trunk-based development effectif : une fonctionnalité incomplète peut vivre
  dans `main`, testée, scannée, déployée, mais invisible.
- Impossible de désynchroniser front et back : le front ne décide de rien, il
  lit. Une divergence de configuration entre les deux conteneurs est
  structurellement exclue.
- Bascule sans redéploiement (variable d'environnement + redémarrage du
  conteneur), ce qui répond à C2.
- Aucune dépendance externe : pas de service de feature management à exploiter,
  à sécuriser ou à payer.
- Le 404 (plutôt qu'un 403) évite de révéler l'existence d'une fonctionnalité
  non livrée.

**Négatives**
- La bascule exige un redémarrage du conteneur : ce n'est pas du *runtime
  toggling* à chaud. Acceptable en staging, à réévaluer pour la production.
- Le flag est **global** : pas de ciblage par utilisateur, pas de déploiement
  progressif ni de test A/B.
- Modifier un flag à la main sur l'environnement crée un **écart entre l'état
  réel et l'état décrit dans Git** — c'est précisément le problème que la
  bascule GitOps corrigera (le flag deviendra une valeur versionnée dans les
  manifests).
- Dette à gérer : un flag ne doit pas survivre indéfiniment à la fonctionnalité
  qu'il protège. Règle retenue : suppression du flag et du code conditionnel
  dès que la fonctionnalité est définitivement acquise.
- Un appel supplémentaire (`/config`) au chargement de l'interface — coût
  négligeable, mitigé par la mise en cache côté rendu serveur.

## Alternatives rejetées

- **Flag côté frontend uniquement** (variable d'environnement Next.js) :
  rejeté — deux sources de vérité, risque de désynchronisation, et l'endpoint
  resterait accessible directement malgré un bouton masqué.
- **Service de feature management externe** (Unleash, LaunchDarkly) : rejeté,
  disproportionné pour un flag unique ; introduit une dépendance externe et un
  point de défaillance.
- **Branche longue au lieu d'un flag** : rejeté, contraire au trunk-based
  development imposé par `CLAUDE.md`.
