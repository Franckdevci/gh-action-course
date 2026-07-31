# ADR-007 — Rétention du journal des alertes et du registry d'events

- **Statut** : Proposé (validation sponsor requise sur le volet légal/métier)
- **Date** : 2026-07-31
- **Exigences concernées** : EX-F-07 R1, EX-NF-03, SEC-06 et SEC-07 (revue sécurité SDD §9)

## Contexte

Deux journaux persistants s'accumulent en base au fil du temps sans mécanisme
d'élagage actuellement défini :

1. **Le journal des alertes** (`alerte`), consigné par le module `alertes` à
   chaque changement de statut de parcelle (EX-F-07). EX-F-07 R1 le décrit
   comme **« immuable, jamais modifié ni supprimé »**.
2. **L'event publication registry** de Spring Modulith (table
   `event_publication`), qui persiste chaque publication de
   `StatutParcelleChange` pour la rejouer en cas de crash du consommateur
   (ADR-003, EX-NF-03). Les publications marquées « traitées » restent en
   base par défaut.

Sans purge, les deux tables croissent linéairement dans le temps. La revue
sécurité (SEC-06 rétention non bornée du journal, SEC-07 croissance du
registry non supervisée) a identifié ces croissances comme dettes à traiter
avant staging. Trois conséquences se cumulent :

- **Volumétrie base** : à la volumétrie H3 (5 000 parcelles) avec un taux
  d'alerte modéré, on estime 100 à 500 entrées journal par mois. Sur 5 ans
  sans purge : 6 000 à 30 000 lignes — négligeable en performance mais
  significatif si un incident généralise les basculements.
- **Registry** : chaque relevé changeant le statut produit une publication.
  L'accumulation de publications « traitées » dilue la lecture des
  publications encore en attente, cible principale de la supervision
  (EX-NF-03).
- **Conformité** : conservation de traces d'événements métier sans borne
  temporelle est un signal RGPD, même si aucune donnée personnelle n'est
  aujourd'hui journalisée (les libellés « code parcelle » et « localité » ne
  sont pas des données à caractère personnel).

Deux périmètres distincts, deux logiques différentes :

- Le **registry** est technique : les publications traitées n'ont plus
  aucune valeur d'exploitation passé un délai court.
- Le **journal des alertes** est métier : il porte la traçabilité exigée par
  EX-F-07 pour tout bailleur ou audit sur les changements de statut de
  parcelle.

## Tension avec EX-F-07 R1 (à porter au sponsor)

EX-F-07 R1 énonce que les entrées du journal sont **« immuables, jamais
modifiées ni supprimées »**. Toute purge, même à long terme, **restreint**
cette immuabilité à une fenêtre de rétention et contredit donc la lettre du
SRS.

Trois lectures possibles :

- **Lecture stricte** : « jamais supprimées » interdit toute purge, la table
  croît indéfiniment, la conformité RGPD/coût base est un problème
  d'exploitation à traiter hors périmètre.
- **Lecture pragmatique** : « immuables » signifie « ni éditées ni
  falsifiées », mais une purge automatisée après période documentée est
  admissible tant qu'elle est prévisible et opérée par le système, pas par
  un utilisateur.
- **Lecture rétention légale** : la rétention effective est dictée par
  l'obligation contractuelle du bailleur (2 ans, 5 ans, 10 ans selon les
  programmes), à défaut par la politique interne.

Cette contradiction ne peut être tranchée par l'architecte-sdd : elle relève
de l'analyste-srs et du sponsor. Le présent ADR formalise **la demande de
clarification** et propose un statu quo temporaire.

- **Option A — statu quo « conservation illimitée »** : conforme à la lettre
  de EX-F-07 R1, mais coûte en base et laisse ouverte la question RGPD
  (même sans PII actuellement, la doctrine bailleur peut évoluer).
- **Option B — clarification analyste-srs, rétention explicite** : voie
  recommandée, matérialisée par ce présent ADR en statut « Proposé ». Le SDD
  documente la rétention envisagée (24 mois) sous statut provisoire tant que
  la clarification n'est pas rendue.

Recommandation : voie (b). L'ADR reste **Proposé** jusqu'à validation, et
seule l'entrée « purge quotidienne » du cron sera activée après validation
explicite du sponsor.

## Décision

Sous réserve de validation sponsor (voie b ci-dessus) :

- **Rétention journal des alertes** : **24 mois** glissants à partir de la
  date de l'événement (`date_changement`). Une tâche planifiée quotidienne
  supprime les entrées plus anciennes. Le seuil `24 mois` est retenu comme
  compromis entre la valeur métier (recul suffisant pour audit d'une saison
  et de la précédente à des fins comparatives) et le coût base
  (accumulation bornée). Modifiable par propriété
  `ecotrack.alertes.retention-mois`.
- **Rétention event publication registry** : **7 jours** pour les
  publications marquées comme traitées (`completion_date < now() - 7d`),
  purgées quotidiennement. Les publications **non traitées** ne sont
  **jamais purgées** automatiquement : elles constituent le signal
  d'incident. Une **métrique** `publications_non_traitees` est exposée
  (Micrometer) et une **alerte oncall** se déclenche dès que le compteur
  dépasse **0** sur plus de 15 minutes (période supérieure au délai normal
  de rejeu).
- **Mécanisme** : tâches Spring `@Scheduled` (cron quotidien nocturne),
  exécutées dans le module technique `shared` avec des services d'entretien
  dédiés par module (`alertes`, `registry`). Aucune commande manuelle
  n'entre dans le flux courant.
- **Traçabilité** : chaque exécution de purge journalise le nombre de lignes
  supprimées (log JSON structuré, cf. ADR-009 proposé).

## Conséquences

**Positives**
- SEC-06 et SEC-07 refermées : croissance bornée, supervision active,
  incidents de traitement visibles.
- Coût base contenu : sur 5 ans, la table `alerte` reste sous quelques
  milliers de lignes plutôt que dizaines de milliers.
- Le registry retrouve sa vocation opérationnelle : les publications encore
  présentes sont soit récentes (< 7 jours), soit en incident.
- Conformité RGPD anticipée si des données personnelles étaient ajoutées en
  v2 (par ex. identité du gestionnaire ayant saisi un relevé).

**Négatives**
- **Contradiction assumée avec la lettre d'EX-F-07 R1** tant que la
  clarification n'est pas rendue. Ce point est le blocage principal à la
  bascule du statut « Proposé » vers « Accepté ».
- Perte définitive de l'historique au-delà de 24 mois : un audit portant sur
  la saison N-3 devient impossible à partir des données courantes (nécessité
  de restaurer une sauvegarde).
- Un incident de traitement d'event non détecté sous 15 minutes déclenche
  une alerte oncall — ce qui exige que l'oncall soit réellement en place
  (dépasse le périmètre v1 solo, à réévaluer avec le sponsor).
- Une propriété modifiable en configuration (`retention-mois`) crée un
  risque de dérive entre environnements : la valeur retenue doit être
  identique en staging et production, versionnée dans les manifests.

**Neutres**
- Les valeurs 24 mois et 7 jours sont des paramètres, pas des constantes
  gravées. Leur remise en cause dans le même ordre de grandeur ne demande
  pas un nouvel ADR.

## Alternatives rejetées

- **Conservation illimitée** (option A ci-dessus) : rejetée en principe pour
  le coût base, la trajectoire RGPD, et l'absence de motivation métier
  d'auditer au-delà de deux saisons. Conservée uniquement comme état de
  repli tant que la clarification n'est pas rendue.
- **Purge manuelle sur demande** (script exécuté par un exploitant) :
  rejetée, non conforme aux exigences bailleur de traçabilité **automatique**
  et sujette à l'oubli. Une purge non exécutée pendant six mois revient au
  problème initial.
- **Archivage vers stockage froid** (S3, cold storage) avant suppression :
  rejeté en v1, disproportionné et sans exigence bailleur documentée. À
  rouvrir dans un ADR ultérieur si la clarification retient une rétention
  légale longue.
- **Purge du registry lors du redémarrage** : rejetée, dépend d'un événement
  imprévisible et laisse le registry gonfler entre deux redémarrages. Un
  cron quotidien est déterministe.

## Références

- EX-F-07 R1 (journal immuable), EX-NF-03 (aucune alerte perdue), EX-NF-06
  (observabilité, implicite) du SRS.
- SDD §6.4 (transactions), §9 revue sécurité (SEC-06, SEC-07).
- ADR-003 (event publication registry) — donne la source de la table à
  purger.
- ADR-009 proposé (journalisation structurée) — cadre le format des logs
  de purge.
- **CLARIF-01 (à ouvrir à l'analyste-srs)** : « EX-F-07 R1 tolère-t-elle
  une purge automatique après rétention documentée ? Si oui, quelle
  rétention minimale ? »
