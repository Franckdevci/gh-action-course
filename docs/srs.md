# SRS — EcoTrack : Spécification des Exigences Logicielles
**Version 1.3 — statut : VALIDÉ** (corrective de conformité, 2026-07-31)
Norme de référence : ISO/IEC/IEEE 29148:2018
Documents liés : `docs/sdd.md` (conception, à produire), `docs/adr/` (décisions), `CLAUDE.md`

## Historique des versions

### v1.3 (2026-07-31) — corrective de conformité
- Corrections IEEE 830 / ISO/IEC/IEEE 29148 sans modification du périmètre métier entériné en §7 (arbitrages inchangés).
- Suppression des marqueurs [À CLARIFIER n°1] à [À CLARIFIER n°9] (déjà tranchés en §7).
- Retrait du vocabulaire technique dans les exigences (renvoi au SDD pour les choix de réalisation).
- Ajout Source + Priorité sur les 14 exigences.
- Complétion du glossaire, ajout de règles métier manquantes, précision des scénarios Gherkin.

### v1.2 (2026-07-29) — VALIDÉ
Les 9 points ouverts sont entérinés avec leurs valeurs par défaut ; ils deviennent les règles officielles de la v1.

### v1.1 — corrections issues de l'audit 29148
Règle de comparaison au seuil critique (précision du taux), scénario du relevé antidaté, comportement « parcelle sans relevé », performance et pagination de la liste, journal des alertes (remplace les « traitements ultérieurs »), exigences non fonctionnelles rendues mesurables (remise en service, accessibilité), bornes de saisie, regarnissage et clôture de parcelle explicitement arbitrés.

### v1.0 — rédaction initiale

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
- Authentification et gestion des rôles (cf. §7 arbitrage n°1 : acteur unique en v1).
- Notifications externes (email, SMS) lors d'une alerte — le journal des
  alertes (EX-F-07) est le point d'ancrage prévu pour une v2.
- Regarnissage (replantation augmentant le nombre de plants) — v1 : le nombre
  de plants initial est immuable (cf. §7 arbitrage n°7).
- Clôture / archivage de parcelle — v1 : une parcelle vit indéfiniment (cf. §7 arbitrage n°8).
- Application mobile et mode hors-ligne.
- Gestion multi-organismes.
- Cartographie / géolocalisation des parcelles.

### 1.3 Définitions et acronymes
| Terme | Définition |
|---|---|
| **Parcelle** | Zone de terrain plantée, identifiée par un code unique, suivie dans le temps |
| **Code parcelle** | Identifiant métier au format `PRC-<année>-<numéro>` (ex. `PRC-2026-042`) |
| **Relevé de croissance** | Observation datée du nombre de plants vivants sur une parcelle |
| **Taux de survie** | Ratio plants vivants / plants initialement mis en terre (cf. règle de précision, EX-F-02 R4) |
| **Dernier relevé** | Relevé dont la **date d'observation** est la plus récente — indépendamment de l'ordre de saisie |
| **EN_ALERTE** | Statut d'une parcelle dont le dernier relevé a un taux de survie strictement inférieur au seuil critique (60 %) |
| **Seuil critique** | Valeur métier fixée à 60 % pour la v1 ; toute parcelle dont le dernier relevé se situe strictement en dessous bascule en EN_ALERTE |
| **Gestionnaire** | Utilisateur du système (acteur unique de la v1) |
| **Journal des alertes** | Registre horodaté, en écriture seule, listant tous les changements de statut d'alerte (passages en alerte et rétablissements) d'une parcelle |
| **Rétablissement** | Retour d'une parcelle du statut EN_ALERTE au statut EN_SUIVI, à la suite d'un relevé plus récent dont le taux de survie atteint ou dépasse le seuil critique |
| **Pagination** | Découpage de la liste des parcelles en pages successives de taille fixée, afin d'en permettre la consultation lorsque le nombre total dépasse ce qui peut être présenté en une seule vue |
| **État vide** | Message métier explicite affiché lorsqu'un écran n'a aucune donnée à présenter (par opposition à une page blanche ou à un message d'erreur) |
| **Antichronologique** | Ordre de présentation du plus récent au plus ancien, par date d'observation |
| **Marqueur visuel d'alerte** | Indication graphique explicite (non fondée sur la seule couleur) signalant qu'une parcelle est en statut EN_ALERTE |

### 1.4 Références
- TP « EcoTrack — cycle de vie complet » v3.2 (source du besoin)
- ISO/IEC/IEEE 29148:2018 — Requirements engineering
- WCAG 2.1 niveau AA (référentiel d'accessibilité, cf. EX-NF-06)

---

## 2. Description générale

### 2.1 Contexte produit
EcoTrack est un système autonome (aucune intégration à un système d'information
existant en v1) offrant au gestionnaire une interface d'usage et conservant
durablement les données saisies. Les choix de réalisation relèvent du document
de conception.

### 2.2 Acteurs et parties prenantes
- **Gestionnaire de reboisement** (acteur unique v1) : crée les parcelles,
  saisit les relevés, consulte l'état du parc et le journal des alertes,
  exporte les données.
- La distinction de rôles supplémentaires (agent de terrain en saisie seule,
  superviseur en lecture seule) est renvoyée à une évolution ultérieure
  (cf. §7 arbitrage n°1).

### 2.3 Hypothèses et dépendances
- H1 : le nombre de plants initial d'une parcelle est connu et fiable à la création.
- H2 : les relevés sont saisis a posteriori par le gestionnaire (pas de capteur).
- H3 : volumétrie v1 : au plus 5 000 parcelles, au plus 100 relevés par
  parcelle, jusqu'à 20 utilisateurs simultanés. **Toute exigence de performance
  (§4) est exprimée et vérifiée sur cette volumétrie.**

### 2.4 Contraintes
- C1 : les données de suivi ne doivent jamais être perdues une fois acceptées
  par le système (cf. EX-NF-03).
- C2 : certaines fonctionnalités doivent pouvoir être activées ou désactivées
  en cours d'exploitation sans nouvelle mise en service (cf. EX-F-04).
- C3 : le système expose sa version en service à des fins d'exploitation (cf. EX-NF-04).

---

## 3. Exigences fonctionnelles

> Convention : chaque exigence est singulière, identifiée, et vérifiable par
> ses critères d'acceptation Gherkin. Les scénarios d'erreur font partie de
> l'exigence.

### EX-F-01 — Créer une parcelle
Le système doit permettre au gestionnaire de créer une parcelle avec : code
parcelle, localité, superficie en hectares, nombre de plants mis en terre,
date de plantation.

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST

**Règles métier** :
- R1 : le code respecte le format `PRC-<année à 4 chiffres>-<numéro, 1 à 3 chiffres>`.
- R2 : le code est unique dans le système.
- R3 : la superficie est comprise entre 0,01 et 10 000 ha, avec au plus
  2 décimales (cf. §7 arbitrage n°3).
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

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST
**Dépend de** : EX-F-01

**Règles métier** :
- R1 : le nombre de plants vivants est un entier ≥ 0 et ≤ nombre de plants
  initial (le regarnissage est hors périmètre v1, cf. §1.2).
- R2 : la date d'observation du relevé n'est ni future, ni antérieure à la
  date de plantation.
- R3 : au plus un relevé par parcelle et par date d'observation ; un second
  relevé à la même date est rejeté (cf. §7 arbitrage n°4).
- R4 : le taux de survie du relevé = plants vivants / plants initiaux, calculé
  par le système (jamais saisi). Il est conservé et comparé **en valeur
  exacte** ; il est **affiché** arrondi à une décimale. En particulier,
  59,95 % est inférieur au seuil critique (60 %) (cf. EX-F-03).

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
  Alors le système rejette la demande avec un message métier explicite indiquant
        que la parcelle référencée n'existe pas
```

### EX-F-03 — Déterminer le statut d'alerte
Le système doit maintenir le statut de chaque parcelle : `EN_ALERTE` si et
seulement si le taux de survie de son **dernier relevé** (au sens de la date
d'observation, cf. §1.3) est **strictement inférieur au seuil critique
(60 %)**, comparé en valeur exacte (cf. EX-F-02 R4) ; `EN_SUIVI` sinon (y
compris sans aucun relevé).

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST
**Dépend de** : EX-F-02, EX-F-07

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

Scénario : cas limite de l'arrondi — décision de statut sur valeur exacte
  Étant donné la parcelle "PRC-2026-043" avec 2000 plants initiaux
  Quand le gestionnaire enregistre un relevé avec 1199 plants vivants (59,95 %)
  Alors la parcelle passe au statut EN_ALERTE (comparaison en valeur exacte au seuil critique)

Scénario : cas limite de l'arrondi — affichage arrondi à une décimale
  Étant donné la parcelle "PRC-2026-043" dont le dernier relevé donne 59,95 %
  Quand le gestionnaire consulte cette parcelle
  Alors le taux affiché est "60,0 %"
  Et la parcelle porte le marqueur visuel d'alerte

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

### EX-F-04 — Exporter la liste des parcelles pour analyse externe
Le système doit permettre au gestionnaire d'exporter la liste des parcelles
sous forme de fichier tabulaire (une ligne par parcelle : code, localité,
superficie, plants initiaux, dernier taux de survie, statut, date du dernier
relevé), destiné à être ouvert dans un tableur de bureautique.

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST

**Règles métier** :
- R1 : l'export peut être activé ou désactivé en cours d'exploitation ;
  désactivé, il est invisible et inaccessible pour l'utilisateur (cf. C2).
- R2 : le fichier produit est un format tabulaire compatible avec les tableurs
  de bureautique en langue française, séparateur point-virgule (cf. §7
  arbitrage n°5).
- R3 : pour une parcelle sans relevé, les colonnes « dernier taux de survie »
  et « date du dernier relevé » sont vides.

```gherkin
Scénario : export activé
  Étant donné l'export activé et 3 parcelles enregistrées dont une sans relevé
  Quand le gestionnaire demande l'export de la liste des parcelles
  Alors il reçoit un fichier tabulaire de 3 lignes de données plus une ligne d'en-tête
  Et la ligne de la parcelle sans relevé a ses colonnes de taux et de date vides

Scénario : export désactivé
  Étant donné l'export désactivé
  Quand le gestionnaire consulte l'interface
  Alors la fonction d'export n'est ni visible ni accessible
```

### EX-F-05 — Consulter la liste des parcelles
Le système doit présenter au gestionnaire la liste des parcelles avec, pour
chacune : code, localité, dernier taux de survie, statut.

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST

**Règles métier** :
- R1 : tri par défaut : parcelles `EN_ALERTE` d'abord, puis par code croissant
  (cf. §7 arbitrage n°6).
- R2 : la liste est paginée par pages de 50 parcelles (cf. H3 : jusqu'à 5 000).
- R3 : les parcelles `EN_ALERTE` sont signalées visuellement par un marqueur
  visuel d'alerte distinctif.
- R4 : pour une parcelle sans relevé, le taux est affiché « — » (absence de donnée).

```gherkin
Scénario : liste triée avec alerte visible
  Étant donné 2 parcelles EN_SUIVI et 1 parcelle EN_ALERTE
  Quand le gestionnaire ouvre la liste des parcelles
  Alors les 3 parcelles sont affichées, la parcelle EN_ALERTE en premier
  Et celle-ci porte un marqueur visuel d'alerte

Scénario : pagination — première page
  Étant donné 120 parcelles enregistrées
  Quand le gestionnaire ouvre la liste des parcelles
  Alors la première page affiche 50 parcelles
  Et une navigation vers les pages suivantes est proposée

Scénario : pagination — dernière page partielle
  Étant donné 120 parcelles enregistrées (50 + 50 + 20)
  Quand le gestionnaire consulte la troisième page
  Alors la page affiche les 20 dernières parcelles
  Et aucune navigation vers une page suivante n'est proposée

Scénario : pagination — au-delà de la dernière page
  Étant donné 120 parcelles enregistrées
  Quand le gestionnaire demande une page au-delà de la dernière (par exemple la quatrième)
  Alors le système affiche un état vide explicite pour cette page
  Et propose de revenir à la première page

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

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST
**Dépend de** : EX-F-01, EX-F-02

**Règles métier** :
- R1 : l'historique d'une parcelle est présenté intégralement, en ordre
  antichronologique, sans limitation de volume tant qu'il n'excède pas
  100 relevés (borne H3).
- R2 : le statut courant est visible sur la vue de détail ; en cas de statut
  EN_ALERTE, il est accompagné du marqueur visuel d'alerte.

```gherkin
Scénario : détail avec historique
  Étant donné la parcelle "PRC-2026-042" avec 3 relevés dont un antidaté
  Quand le gestionnaire ouvre le détail de la parcelle
  Alors ses caractéristiques et ses 3 relevés sont affichés, triés par date
        d'observation décroissante
  Et le statut courant est visible (marqueur visuel d'alerte si EN_ALERTE)

Scénario : historique volumineux dans la borne métier
  Étant donné la parcelle "PRC-2026-042" comportant 100 relevés
  Quand le gestionnaire ouvre le détail de la parcelle
  Alors l'ensemble des 100 relevés est présenté en ordre antichronologique
  Et aucun relevé n'est masqué ni tronqué

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

**Source** : TP « EcoTrack — cycle de vie complet » v3.2
**Priorité** : MUST
**Dépend de** : EX-F-03

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
Sur la volumétrie H3, sous une sollicitation soutenue de 10 consultations par
seconde pendant 60 secondes : le temps de restitution de la fiche d'une
parcelle (historique inclus) doit être **P95 < 500 ms** ; celui d'une page de
la liste des parcelles **P95 < 500 ms**. Mesure effectuée du point de vue du
gestionnaire.

**Source** : sponsor projet (attente de réactivité perçue par le gestionnaire)
**Priorité** : MUST

*Vérification : test de charge automatisé sur l'environnement de pré-production.*

### EX-NF-02 — Continuité de service lors des remises en service
Lors de toute opération de remise en service programmée (déploiement d'une
nouvelle version, redémarrage), le gestionnaire ne doit subir aucune
interruption perceptible : un flux de 5 sollicitations valides par seconde
doit obtenir **100 % de réponses correctes** (aucune erreur, aucune
indisponibilité imputable à l'opération).

**Source** : sponsor projet (continuité opérationnelle)
**Priorité** : MUST

*Vérification : tests automatisés exécutés pendant une remise en service réelle en pré-production.*

### EX-NF-03 — Fiabilité du journal des alertes
Aucun changement de statut d'alerte accepté par le système ne doit être absent
du journal (EX-F-07), y compris en cas d'arrêt brutal du système immédiatement
après l'enregistrement du relevé déclencheur : l'entrée correspondante doit
exister au plus tard au redémarrage.

**Source** : sponsor projet (exigence de redevabilité vis-à-vis du bailleur)
**Priorité** : MUST

*Vérification : test d'intégration simulant un arrêt brutal entre l'enregistrement du relevé et l'inscription au journal.*

### EX-NF-04 — Traçabilité de la version en service
Le système doit publier de manière visible pour l'exploitant, en deux endroits
cohérents (interface d'usage et point d'interrogation dédié à l'exploitation),
l'identifiant de la version en service, sa date de mise en service et une
référence unique traçable au code livré.

**Source** : sponsor projet (exploitabilité)
**Priorité** : SHOULD

*Vérification : contrôle automatisé, après chaque remise en service, comparant la cohérence des deux sources de publication.*

### EX-NF-05 — Sécurité des entrées
Toute donnée entrante doit être validée avant traitement (format, bornes,
cohérence) ; une entrée invalide est rejetée avec un message explicite ne
révélant aucun détail technique interne (trace d'erreur, requêtes internes,
numéros de version).

**Source** : bonnes pratiques de sécurité applicative + attente bailleur
**Priorité** : MUST

*Vérification : tests automatisés des cas d'erreur de chaque exigence fonctionnelle.*

### EX-NF-06 — Utilisabilité et accessibilité
L'interface d'usage doit présenter des états explicites (chargement, vide,
erreur) sur chaque écran, et satisfaire les critères WCAG 2.1 niveau AA
suivants : libellés de champs de formulaire, rôles sémantiques des éléments
interactifs, contraste minimal 4,5:1 pour le texte, navigation au clavier sur
les actions principales. Le marqueur visuel d'alerte ne doit pas reposer sur
la seule couleur.

**Source** : référentiel WCAG 2.1 AA + attente sponsor
**Priorité** : SHOULD

*Vérification : tests de composants + audit outillé d'accessibilité sur les écrans livrés.*

### EX-NF-07 — Intégrité des données
Le calcul du taux de survie et la détermination du statut sont exclusivement
effectués par le système ; aucune interface ne permet de les saisir ou de les
modifier directement.

**Source** : TP « EcoTrack — cycle de vie complet » v3.2 (règle métier fondamentale)
**Priorité** : SHOULD

*Vérification : revue du contrat d'interopérabilité (aucun champ de statut ou de taux en écriture) + tests.*

---

## 5. Exigences d'interface

- **EI-01 — Interface d'usage** : le système est utilisé au travers d'une
  interface web adaptable aux principaux formats d'écran (poste de travail
  prioritaire, tablette). L'usage mobile n'est pas une cible v1 (cf. §7
  arbitrage n°9).
- **EI-02 — Interface d'interopérabilité** : les fonctions EX-F-01 à EX-F-04
  et EX-F-07 doivent être accessibles à un consommateur programmatique
  distinct de l'interface d'usage, selon un contrat documenté faisant foi
  entre l'interface d'usage et le système. Les modalités techniques relèvent
  du document de conception.
- **EI-03 — Persistance des données** : les données saisies survivent à toute
  remise en service du système. Le choix des moyens relève du document de conception.

---

## 6. Matrice de traçabilité

| Besoin métier | Source amont | Exigence(s) | Vérification prévue |
|---|---|---|---|
| Constituer le référentiel des parcelles | TP EcoTrack v3.2 | EX-F-01 | Tests unitaires domaine + tests d'interopérabilité + tests bout-en-bout |
| Suivre la croissance dans le temps | TP EcoTrack v3.2 | EX-F-02, EX-F-06 | Tests domaine (précision, relevé antidaté) + interopérabilité + tests bout-en-bout navigateur |
| Réagir vite aux parcelles en difficulté | TP EcoTrack v3.2 + attente bailleur | EX-F-03, EX-F-05, EX-F-07, EX-NF-03 | Tests domaine (seuil critique, arrondi, antidaté) + tests d'intégration en cas d'arrêt brutal + tests bout-en-bout |
| Analyser les données hors du système | Entretien sponsor (usage tableur) | EX-F-04 | Tests d'interopérabilité (fonction activée/désactivée, parcelle sans relevé) + tests bout-en-bout |
| Confiance dans les chiffres | Règle métier fondamentale (TP EcoTrack) | EX-NF-07, EX-NF-05, EX-F-02 R4 | Revue du contrat d'interopérabilité + tests cas d'erreur et cas limites |
| Service utilisable au quotidien | Attente sponsor + WCAG 2.1 AA | EX-NF-01, EX-NF-02, EX-NF-06, EX-F-05 R2 | Test de charge en pré-production + tests pendant remise en service + audit d'accessibilité |
| Exploitabilité | Attente sponsor (exploitation) | EX-NF-04 | Contrôle automatisé après chaque remise en service |

---

## 7. Arbitrages (points ouverts entérinés en v1.2)

| # | Question | Décision v1 |
|---|---|---|
| 1 | Rôles distincts (terrain / superviseur) ? | Acteur unique, pas d'authentification v1 |
| 2 | Volumétrie cible réelle | ≤ 5 000 parcelles, ≤ 100 relevés/parcelle, 20 utilisateurs |
| 3 | Bornes et précision de la superficie | 0,01 à 10 000 ha, 2 décimales |
| 4 | Deux relevés le même jour | Rejet du second |
| 5 | Séparateur du fichier tabulaire d'export | Point-virgule (compatibilité tableur de bureautique en langue française) |
| 6 | Tri par défaut de la liste | Alertes d'abord, puis code croissant |
| 7 | Regarnissage (plants ajoutés après plantation) | Hors périmètre v1 — plants initiaux immuables |
| 8 | Clôture / archivage d'une parcelle | Hors périmètre v1 — aucune fin de vie |
| 9 | Usage mobile de l'interface | Poste de travail prioritaire, adaptation aux principaux formats d'écran (tablette incluse) au mieux |

---

*Toute évolution de ce document passe par une Pull Request référencée.*
