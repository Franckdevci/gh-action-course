# SRS — EcoTrack : Spécification des Exigences Logicielles
**Version 1.2 — statut : VALIDÉ** (arbitrages §7 entérinés le 2026-07-29)
Norme de référence : ISO/IEC/IEEE 29148:2018
Documents liés : `docs/sdd.md` (conception, à produire), `docs/adr/` (décisions), `CLAUDE.md`

> Historique : v1.0 rédaction initiale · **v1.1 corrections issues de l'audit
> 29148** — règle de comparaison au seuil (précision du taux), scénario du
> relevé antidaté, comportement « parcelle sans relevé », perf + pagination de
> la liste, journal des alertes (remplace les « traitements ultérieurs »),
> exigences NF rendues mesurables (déploiement, accessibilité), bornes de
> saisie, regarnissage et clôture de parcelle explicitement arbitrés.
> · **v1.2 VALIDÉ** — les 9 points ouverts sont entérinés avec leurs valeurs
> par défaut ; ils deviennent les règles officielles de la v1.

---

## 1. Introduction

### 1.1 Objet
Ce document spécifie les exigences du système **EcoTrack**, application de suivi
de parcelles de reboisement. Il constitue la source de vérité fonctionnelle du
projet : toute fonctionnalité implémentée doit être tracée vers une exigence de
ce document (référence `EX-x-xx` dans les Pull Requests).

### 1.2 Portée
EcoTrack permet à un organisme de reboisement de : enregistrer ses parcelles,
suivre la croissance des plants par relevés périodiques, détecter automatiquement
les parcelles dont le taux de survie devient critique, consulter l'état du parc
et le journal des alertes via une interface web, et exporter les données pour
analyse externe.

**Hors périmètre (version 1) — choix explicites, pas des oublis** :
- Authentification et gestion des rôles `[À CLARIFIER n°1 — qui accède au système aujourd'hui ?]`
- Notifications externes (email, SMS) lors d'une alerte — le journal des
  alertes (EX-F-07) est le point d'ancrage prévu pour une v2
- Regarnissage (replantation augmentant le nombre de plants) — v1 : le nombre
  de plants initial est immuable `[À CLARIFIER n°7]`
- Clôture / archivage de parcelle — v1 : une parcelle vit indéfiniment `[À CLARIFIER n°8]`
- Application mobile et mode hors-ligne
- Gestion multi-organismes (multi-tenancy)
- Cartographie / géolocalisation des parcelles

### 1.3 Définitions et acronymes
| Terme | Définition |
|---|---|
| **Parcelle** | Zone de terrain plantée, identifiée par un code unique, suivie dans le temps |
| **Code parcelle** | Identifiant métier au format `PRC-<année>-<numéro>` (ex. `PRC-2026-042`) |
| **Relevé de croissance** | Observation datée du nombre de plants vivants sur une parcelle |
| **Taux de survie** | Ratio plants vivants / plants initialement mis en terre (cf. règle de précision, EX-F-02 R4) |
| **Dernier relevé** | Relevé dont la **date d'observation** est la plus récente — indépendamment de l'ordre de saisie |
| **EN_ALERTE** | Statut d'une parcelle dont le dernier relevé a un taux de survie < 60 % |
| **Seuil critique** | 60 % (valeur métier fixée pour la v1) |
| **Gestionnaire** | Utilisateur du système (acteur unique de la v1) |

### 1.4 Références
- TP « EcoTrack — cycle de vie complet » v3.2 (source du besoin)
- ISO/IEC/IEEE 29148:2018 — Requirements engineering
- WCAG 2.1 niveau AA (référentiel d'accessibilité, cf. EX-NF-06)

---

## 2. Description générale

### 2.1 Contexte produit
EcoTrack est un système autonome (aucune intégration à un SI existant en v1)
composé d'une interface web et d'une API, avec persistance des données. Les
choix techniques (architecture, technologies, découpage) relèvent du SDD.

### 2.2 Acteurs et parties prenantes
- **Gestionnaire de reboisement** (acteur unique v1) : crée les parcelles,
  saisit les relevés, consulte l'état du parc et le journal des alertes,
  exporte les données.
- `[À CLARIFIER n°1]` Rôles distincts (agent de terrain : saisie seule ;
  superviseur : lecture seule) ? Impact : v2, exigences d'autorisation.

### 2.3 Hypothèses et dépendances
- H1 : le nombre de plants initial d'une parcelle est connu et fiable à la création.
- H2 : les relevés sont saisis a posteriori par le gestionnaire (pas de capteur).
- H3 : volumétrie v1 `[À CLARIFIER n°2 — défaut retenu : ≤ 5 000 parcelles,
  ≤ 100 relevés par parcelle, ≤ 20 utilisateurs simultanés]`. **Toute exigence
  de performance (§4) est exprimée et vérifiée sur cette volumétrie.**

### 2.4 Contraintes
- C1 : les données de suivi ne doivent jamais être perdues une fois acceptées
  par le système (cf. EX-NF-03).
- C2 : certaines fonctionnalités doivent pouvoir être activées/désactivées à
  l'exécution sans redéploiement (mécanisme de feature flag — cf. EX-F-04).
- C3 : le système expose sa version déployée à des fins d'exploitation (cf. EX-NF-04).

---

## 3. Exigences fonctionnelles

> Convention : chaque exigence est singulière, identifiée, et vérifiable par
> ses critères d'acceptation Gherkin. Les scénarios d'erreur font partie de
> l'exigence.

### EX-F-01 — Créer une parcelle
Le système doit permettre au gestionnaire de créer une parcelle avec : code
parcelle, localité, superficie en hectares, nombre de plants mis en terre,
date de plantation.

**Règles métier** :
- R1 : le code respecte le format `PRC-<année à 4 chiffres>-<numéro, 1 à 3 chiffres>`.
- R2 : le code est unique dans le système.
- R3 : la superficie est comprise entre 0,01 et 10 000 ha, avec au plus
  2 décimales `[À CLARIFIER n°3 — bornes et précision par défaut]`.
- R4 : la localité est un texte non vide de 100 caractères au plus.
- R5 : le nombre de plants initial est un entier strictement positif.
- R6 : la date de plantation n'est pas dans le futur.
- R7 : à la création, la parcelle a le statut `EN_SUIVI` et aucun relevé.

```gherkin
Scénario : création nominale
  Étant donné aucune parcelle de code "PRC-2026-042"
  Quand le gestionnaire crée la parcelle "PRC-2026-042", localité "Bingerville",
        superficie 12.50 ha, 2000 plants, plantée le 2026-06-15
  Alors la parcelle est enregistrée avec le statut EN_SUIVI
  Et elle apparaît dans la liste des parcelles

Scénario : code invalide
  Quand le gestionnaire crée une parcelle avec le code "PARCELLE-42"
  Alors le système rejette la demande avec une erreur de validation explicite
  Et aucune parcelle n'est créée

Scénario : code déjà utilisé
  Étant donné une parcelle existante de code "PRC-2026-042"
  Quand le gestionnaire crée une parcelle avec le même code
  Alors le système rejette la demande en signalant le conflit d'unicité

Scénario : date future
  Quand le gestionnaire crée une parcelle plantée à une date postérieure à aujourd'hui
  Alors le système rejette la demande avec une erreur de validation explicite

Scénario : bornes de saisie
  Quand le gestionnaire crée une parcelle de superficie 0 ha, ou de localité vide,
        ou de localité de 101 caractères
  Alors le système rejette chaque demande avec une erreur de validation explicite
```

### EX-F-02 — Enregistrer un relevé de croissance
Le système doit permettre au gestionnaire d'enregistrer, pour une parcelle
existante, un relevé daté indiquant le nombre de plants vivants observés.

**Règles métier** :
- R1 : le nombre de plants vivants est un entier ≥ 0 et ≤ nombre de plants
  initial (le regarnissage est hors périmètre v1, cf. §1.2).
- R2 : la date d'observation du relevé n'est ni future, ni antérieure à la
  date de plantation.
- R3 : au plus un relevé par parcelle et par date d'observation ; un second
  relevé à la même date est rejeté `[À CLARIFIER n°4]`.
- R4 : le taux de survie du relevé = plants vivants / plants initiaux, calculé
  par le système (jamais saisi). Il est conservé et comparé **en valeur
  exacte** ; il est **affiché** arrondi à une décimale. En particulier,
  59,95 % est inférieur au seuil de 60 % (cf. EX-F-03).

```gherkin
Scénario : relevé nominal
  Étant donné la parcelle "PRC-2026-042" avec 2000 plants initiaux
  Quand le gestionnaire enregistre un relevé du 2026-07-20 avec 1700 plants vivants
  Alors le relevé est enregistré avec un taux de survie de 85 %
  Et il apparaît dans l'historique de la parcelle

Scénario : plants vivants supérieurs aux plants initiaux
  Étant donné la parcelle "PRC-2026-042" avec 2000 plants initiaux
  Quand le gestionnaire enregistre un relevé avec 2100 plants vivants
  Alors le système rejette la demande avec une erreur de validation explicite

Scénario : relevé antérieur à la plantation
  Étant donné la parcelle "PRC-2026-042" plantée le 2026-06-15
  Quand le gestionnaire enregistre un relevé daté du 2026-06-01
  Alors le système rejette la demande avec une erreur de validation explicite

Scénario : doublon de date
  Étant donné un relevé du 2026-07-20 sur la parcelle "PRC-2026-042"
  Quand le gestionnaire enregistre un second relevé daté du 2026-07-20
  Alors le système rejette la demande en signalant le doublon

Scénario : parcelle inexistante
  Quand le gestionnaire enregistre un relevé pour la parcelle "PRC-2026-999" inconnue
  Alors le système répond que la parcelle n'existe pas
```

### EX-F-03 — Déterminer le statut d'alerte
Le système doit maintenir le statut de chaque parcelle : `EN_ALERTE` si et
seulement si le taux de survie de son **dernier relevé** (au sens de la date
d'observation, cf. §1.3) est **strictement inférieur à 60 %**, comparé en
valeur exacte (cf. EX-F-02 R4) ; `EN_SUIVI` sinon (y compris sans aucun relevé).

**Règles métier** :
- R1 : le statut est recalculé à chaque enregistrement de relevé, à partir du
  relevé le plus récent **par date d'observation** — un relevé antidaté ne
  change le statut que s'il devient le plus récent (ce qu'il n'est pas).
- R2 : chaque **passage** de `EN_SUIVI` à `EN_ALERTE` est enregistré au journal
  des alertes (cf. EX-F-07) — le rétablissement y est également consigné.

```gherkin
Scénario : passage en alerte
  Étant donné la parcelle "PRC-2026-042" avec 2000 plants initiaux, au statut EN_SUIVI
  Quand le gestionnaire enregistre un relevé avec 1100 plants vivants (55 %)
  Alors la parcelle passe au statut EN_ALERTE
  Et une entrée de passage en alerte est créée au journal des alertes

Scénario : cas limite au seuil exact
  Quand le dernier relevé donne un taux de survie d'exactement 60 % (1200/2000)
  Alors la parcelle est au statut EN_SUIVI

Scénario : cas limite de l'arrondi
  Étant donné la parcelle "PRC-2026-043" avec 2000 plants initiaux
  Quand le gestionnaire enregistre un relevé avec 1199 plants vivants (59,95 %)
  Alors la parcelle passe au statut EN_ALERTE
  Et le taux affiché est "60,0 %" accompagné du badge d'alerte

Scénario : relevé antidaté sans effet sur le statut
  Étant donné la parcelle "PRC-2026-042" dont le dernier relevé (2026-07-20) donne 80 %
  Quand le gestionnaire enregistre un relevé daté du 2026-06-20 avec un taux de 45 %
  Alors la parcelle reste au statut EN_SUIVI
  Et le relevé du 2026-06-20 apparaît dans l'historique à sa place chronologique

Scénario : rétablissement
  Étant donné la parcelle "PRC-2026-042" au statut EN_ALERTE
  Quand un relevé plus récent donne un taux de survie de 72 %
  Alors la parcelle repasse au statut EN_SUIVI
  Et le rétablissement est consigné au journal des alertes
```

### EX-F-04 — Exporter les parcelles en CSV
Le système doit permettre au gestionnaire d'exporter la liste des parcelles au
format CSV (une ligne par parcelle : code, localité, superficie, plants initiaux,
dernier taux de survie, statut, date du dernier relevé).

**Règles métier** :
- R1 : cette fonctionnalité est placée derrière un feature flag ; désactivée,
  elle est absente du système du point de vue de l'utilisateur (cf. C2).
- R2 : encodage UTF-8, séparateur `;` `[À CLARIFIER n°5 — public cible Excel FR ?]`.
- R3 : pour une parcelle sans relevé, les colonnes « dernier taux de survie »
  et « date du dernier relevé » sont vides.

```gherkin
Scénario : export activé
  Étant donné le feature flag export-csv ACTIVÉ et 3 parcelles enregistrées
        dont une sans relevé
  Quand le gestionnaire demande l'export CSV
  Alors il reçoit un fichier CSV de 3 lignes de données plus une ligne d'en-tête
  Et la ligne de la parcelle sans relevé a ses colonnes de taux et de date vides

Scénario : export désactivé
  Étant donné le feature flag export-csv DÉSACTIVÉ
  Quand le gestionnaire demande l'export CSV
  Alors le système répond que la ressource n'existe pas
```

### EX-F-05 — Consulter la liste des parcelles
Le système doit présenter au gestionnaire la liste des parcelles avec, pour
chacune : code, localité, dernier taux de survie, statut.

**Règles métier** :
- R1 : tri par défaut : parcelles `EN_ALERTE` d'abord, puis par code croissant
  `[À CLARIFIER n°6]`.
- R2 : la liste est paginée par pages de 50 parcelles (cf. H3 : jusqu'à 5 000).
- R3 : les parcelles `EN_ALERTE` sont signalées visuellement (badge distinctif).
- R4 : pour une parcelle sans relevé, le taux est affiché « — » (absence de donnée).

```gherkin
Scénario : liste triée avec alerte visible
  Étant donné 2 parcelles EN_SUIVI et 1 parcelle EN_ALERTE
  Quand le gestionnaire ouvre la liste des parcelles
  Alors les 3 parcelles sont affichées, la parcelle EN_ALERTE en premier
  Et celle-ci porte un badge d'alerte visible

Scénario : pagination
  Étant donné 120 parcelles enregistrées
  Quand le gestionnaire ouvre la liste des parcelles
  Alors la première page affiche 50 parcelles et la navigation vers les pages suivantes

Scénario : parcelle sans relevé
  Étant donné une parcelle sans aucun relevé
  Quand le gestionnaire ouvre la liste
  Alors cette parcelle affiche « — » comme taux et le statut EN_SUIVI

Scénario : parc vide
  Étant donné aucune parcelle enregistrée
  Quand le gestionnaire ouvre la liste
  Alors un état vide explicite est affiché (pas une page blanche ni une erreur)
```

### EX-F-06 — Consulter le détail d'une parcelle et son historique
Le système doit présenter, pour une parcelle donnée, ses caractéristiques et
l'historique complet de ses relevés (date d'observation, plants vivants, taux
de survie), du plus récent au plus ancien **par date d'observation**.

```gherkin
Scénario : détail avec historique
  Étant donné la parcelle "PRC-2026-042" avec 3 relevés dont un antidaté
  Quand le gestionnaire ouvre le détail de la parcelle
  Alors ses caractéristiques et ses 3 relevés sont affichés, triés par date
        d'observation décroissante
  Et le statut courant est visible (badge si EN_ALERTE)

Scénario : parcelle sans relevé
  Étant donné une parcelle sans aucun relevé
  Quand le gestionnaire ouvre son détail
  Alors un état « aucun relevé » explicite est affiché à la place de l'historique

Scénario : parcelle inconnue
  Quand le gestionnaire ouvre le détail d'une parcelle inexistante
  Alors une page "parcelle introuvable" explicite est affichée
```

### EX-F-07 — Consulter le journal des alertes
Le système doit tenir un journal horodaté des changements de statut d'alerte
(passages en alerte et rétablissements), consultable par le gestionnaire :
date, parcelle, sens du changement, taux déclencheur.

**Règles métier** :
- R1 : les entrées du journal sont immuables (jamais modifiées ni supprimées).
- R2 : le journal est trié antichronologiquement.

```gherkin
Scénario : consultation du journal
  Étant donné un passage en alerte de "PRC-2026-042" puis son rétablissement
  Quand le gestionnaire consulte le journal des alertes
  Alors il voit les deux entrées, la plus récente en premier, avec date,
        parcelle, sens du changement et taux déclencheur
```

---

## 4. Exigences non fonctionnelles

### EX-NF-01 — Performance
Sur la volumétrie H3, mesuré côté API sous une charge de 10 requêtes/s pendant
60 s : le temps de réponse de la consultation du détail d'une parcelle
(historique inclus) doit être **P95 < 500 ms**, et celui d'une page de la liste
des parcelles **P95 < 500 ms**.
*Vérification : test de charge automatisé sur l'environnement de staging.*

### EX-NF-02 — Disponibilité en déploiement
Pendant toute la fenêtre d'une mise à jour du système, un flux continu de
requêtes valides émis à 5 requêtes/s doit obtenir **100 % de réponses
correctes** (aucune erreur, aucune connexion refusée imputable au déploiement).
*Vérification : tests automatisés exécutés pendant un déploiement réel en staging.*

### EX-NF-03 — Fiabilité du journal des alertes
Aucun changement de statut d'alerte accepté par le système ne doit être absent
du journal (EX-F-07), y compris en cas d'arrêt brutal du système immédiatement
après l'enregistrement du relevé déclencheur : l'entrée correspondante doit
exister au plus tard au redémarrage.
*Vérification : test d'intégration simulant un crash entre l'enregistrement et l'écriture au journal.*

### EX-NF-04 — Traçabilité de la version
Le système doit exposer sa version déployée (version, commit, date de build) :
via un point de consultation technique côté API, et visiblement dans le pied de
page de l'interface web. Les deux valeurs doivent être cohérentes.
*Vérification : test automatisé post-déploiement comparant les deux sources.*

### EX-NF-05 — Sécurité des entrées
Toute donnée entrante doit être validée avant traitement (format, bornes,
cohérence) ; une entrée invalide est rejetée avec un message explicite ne
révélant aucun détail interne (pile d'exécution, requêtes, versions).
*Vérification : tests automatisés des cas d'erreur de chaque exigence fonctionnelle.*

### EX-NF-06 — Utilisabilité et accessibilité
L'interface web doit présenter des états explicites (chargement, vide, erreur)
sur chaque écran, et satisfaire les critères WCAG 2.1 niveau AA suivants :
libellés de champs de formulaire, rôles sémantiques des éléments interactifs,
contraste minimal 4,5:1 pour le texte, navigation clavier sur les actions
principales. Le badge d'alerte ne doit pas reposer sur la seule couleur.
*Vérification : tests de composants + audit outillé d'accessibilité sur les écrans livrés.*

### EX-NF-07 — Intégrité des données
Le calcul du taux de survie et la détermination du statut sont exclusivement
effectués par le système ; aucune interface ne permet de les saisir ou de les
modifier directement.
*Vérification : revue du contrat d'API (aucun champ de statut/taux en écriture) + tests.*

---

## 5. Exigences d'interface

- **EI-01 — Interface utilisateur** : application web (navigateurs récents,
  desktop prioritaire ; responsive `[À CLARIFIER n°9 — usage mobile réel ?]`).
- **EI-02 — Interface de programmation** : les fonctions EX-F-01 à EX-F-04 et
  EX-F-07 sont exposées via une API HTTP dont le contrat détaillé (ressources,
  codes de réponse, schémas, pagination) est défini au SDD et fait foi entre
  interface web et système.
- **EI-03 — Persistance** : les données survivent au redémarrage du système
  (le choix du mécanisme relève du SDD).

---

## 6. Matrice de traçabilité

| Besoin métier | Exigence(s) | Vérification prévue |
|---|---|---|
| Constituer le référentiel des parcelles | EX-F-01 | Tests unitaires domaine + tests API + e2e |
| Suivre la croissance dans le temps | EX-F-02, EX-F-06 | Tests domaine (précision, antidaté) + API + e2e navigateur |
| Réagir vite aux parcelles en difficulté | EX-F-03, EX-F-05, EX-F-07, EX-NF-03 | Tests domaine (seuil, arrondi, antidaté) + intégration crash + e2e |
| Analyser les données hors du système | EX-F-04 | Tests API (flag ON/OFF, parcelle sans relevé) + e2e |
| Confiance dans les chiffres | EX-NF-07, EX-NF-05, EX-F-02 R4 | Revue contrat + tests cas d'erreur et cas limites |
| Service utilisable au quotidien | EX-NF-01, EX-NF-02, EX-NF-06, EX-F-05 R2 | Charge staging + tests pendant déploiement + audit accessibilité |
| Exploitabilité | EX-NF-04 | Test post-déploiement |

---

## 7. Arbitrages (points ouverts entérinés en v1.2)

| # | Question | Décision v1 |
|---|---|---|
| 1 | Rôles distincts (terrain / superviseur) ? | Acteur unique, pas d'authentification v1 |
| 2 | Volumétrie cible réelle | ≤ 5 000 parcelles, ≤ 100 relevés/parcelle, 20 utilisateurs |
| 3 | Bornes et précision de la superficie | 0,01 à 10 000 ha, 2 décimales |
| 4 | Deux relevés le même jour | Rejet du second |
| 5 | Séparateur CSV | `;` (usage Excel francophone) |
| 6 | Tri par défaut de la liste | Alertes d'abord, puis code croissant |
| 7 | Regarnissage (plants ajoutés après plantation) | Hors périmètre v1 — plants initiaux immuables |
| 8 | Clôture / archivage d'une parcelle | Hors périmètre v1 — aucune fin de vie |
| 9 | Usage mobile de l'interface | Desktop prioritaire, responsive best-effort |

---

*Toute évolution de ce document passe par une Pull Request référencée.*
