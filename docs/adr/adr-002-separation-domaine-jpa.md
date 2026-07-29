# ADR-002 — Séparation stricte domaine / JPA avec mapper explicite

- **Statut** : Acceptée
- **Date** : 2026-07-29
- **Référencée par** : SDD §3.4

## Contexte

Le cœur métier d'EcoTrack contient des invariants sensibles :
- `TauxDeSurvie` doit être un `BigDecimal` d'échelle 4 comparé strictement au
  seuil de 60 % (EX-F-03 et son scénario limite 59,95 % / 1199 sur 2000).
- Les objets-valeur (`CodeParcelle`, `Superficie`, `Localite`, `NombrePlants`)
  doivent rejeter à la construction toute valeur qui viole leur invariant
  (EX-F-01 R1 à R5, EX-F-02 R1 à R4).
- Le calcul du statut (`RegleStatutAlerte`) doit tourner en millisecondes,
  sans démarrer de contexte Spring, pour permettre des tests exhaustifs des
  cas limites listés dans SDD §7.2.

Deux options : partager les mêmes classes entre domaine et persistance
(annotations JPA sur les entités du domaine — approche « Active Record »
répandue en Spring), ou séparer les deux mondes avec un mapper explicite.

## Décision

Le domaine ignore JPA. Chaque module a :
- Ses classes de domaine (`Releve`, `Parcelle`, `EntreeJournal`) — Java pur,
  aucun `import jakarta.persistence.*`, aucun `import org.springframework.*`.
- Ses entités de persistance `*JpaEntity` — annotées JPA, dans
  `infrastructure/jpa/`.
- Un mapper explicite bidirectionnel entre les deux.

Le schéma est **géré par Flyway uniquement** ; `spring.jpa.hibernate.ddl-auto`
est fixé à `validate` sur tous les profils. Hibernate n'a jamais l'autorisation
de modifier le schéma.

## Conséquences

- **Positives** :
  - Les invariants du domaine ne dépendent pas du cycle de vie d'Hibernate
    (constructeur privé + factory versus proxy Hibernate, lazy loading,
    session détachée) — ces surprises n'existent plus.
  - Le domaine est testable en isolation totale : `RegleStatutAlerte` et les
    VO tournent sans base, sans Spring, en millisecondes. Les 6 tests non
    négociables du SDD §7.2 restent des tests unitaires purs.
  - Le schéma est indépendant du refactor Java : renommer un champ Java
    n'implique pas de renommer une colonne SQL. Les migrations expand/contract
    (EX-NF-02) deviennent gérables.
  - Aucun risque que l'ORM invente des requêtes N+1 sur des collections que le
    domaine n'expose même pas.
- **Négatives** :
  - Coût de mapping explicite (~30 lignes par entité). Assumé.
  - Deux modèles à maintenir en cohérence, à partir d'une seule source de
    vérité qui est le schéma SQL (Flyway).

## Alternatives considérées

- **Annotations JPA sur les classes de domaine** : rejeté. Casse
  l'isolation du domaine (les tests exigent un `EntityManager`), rend les
  invariants fragiles (constructeur par défaut requis, setters exposés pour
  Hibernate). Sur un domaine avec des cas limites arithmétiques comme
  `TauxDeSurvie`, l'obligation d'un getter/setter public sur un `BigDecimal`
  invite à la mutation accidentelle depuis un test ou un contrôleur.
- **`ddl-auto=update`** en dev : rejeté. Divergence garantie entre le schéma
  effectif et les migrations Flyway. Un dev qui ajoute un champ voit son test
  passer localement (schéma mis à jour par Hibernate) puis casse en staging
  (Flyway n'a rien migré).
