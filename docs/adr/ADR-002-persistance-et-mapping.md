# ADR-002 — H2 en développement, PostgreSQL en staging, domaine séparé de JPA

- **Statut** : Accepté
- **Date** : 2026-07-29
- **Exigences concernées** : EI-03, EX-NF-01, EX-NF-07, EX-F-01 R2, EX-F-02 R3

## Contexte

Le SRS exige que les données survivent au redémarrage (EI-03) sans imposer de
mécanisme. Deux questions distinctes se posent :

1. **Quelle base, dans quel environnement ?** Une base embarquée accélère le
   développement et les tests ; une base réelle est indispensable en staging
   pour que les tests de charge (EX-NF-01) et les contraintes d'unicité aient
   un sens.
2. **Le domaine doit-il être annoté JPA ?** Annoter directement les objets de
   domaine évite le code de mapping, mais lie les invariants métier au cycle de
   vie d'Hibernate (proxies, chargement paresseux, constructeur sans argument
   exigé, setters de fait).

## Décision

**Bases** : H2 en mode compatibilité PostgreSQL pour les profils `dev` et
`test`, PostgreSQL 16 pour `staging` (et la production future). Le schéma est
géré **exclusivement par Flyway**, avec `spring.jpa.hibernate.ddl-auto=validate`
sur tous les profils.

**Mapping** : le domaine reste du **Java pur** (aucun import `jakarta.persistence`).
Chaque module possède ses entités `*JpaEntity` dans son package
`infrastructure/jpa`, avec un mapper explicite domaine ↔ entité.

Les contraintes structurantes sont déclarées **en base** et pas seulement en
Java : `UNIQUE(code)` sur `parcelle` (EX-F-01 R2),
`UNIQUE(parcelle_id, date_observation)` sur `releve` (EX-F-02 R3).

## Conséquences

**Positives**
- Le domaine est testable en millisecondes, sans contexte Spring ni base : les
  cas limites d'EX-F-03 (seuil, arrondi, relevé antidaté) se testent en JUnit pur.
- Les invariants sont garantis par les constructeurs des objets de valeur, sans
  compromis imposé par Hibernate (pas de constructeur vide public, pas de setter
  de complaisance) — ce qui soutient directement EX-NF-07.
- `ddl-auto=validate` transforme un écart de schéma en échec au démarrage,
  attrapé par la sonde readiness, plutôt qu'en corruption silencieuse.
- Les contraintes en base protègent des écritures concurrentes que la
  validation applicative ne voit pas (deux créations simultanées du même code).
- H2 en mode PostgreSQL réduit — sans l'éliminer — l'écart de comportement SQL
  entre développement et staging.

**Négatives**
- Code de mapping à écrire et maintenir pour chaque agrégat : coût réel,
  assumé comme le prix de l'indépendance du domaine.
- L'écart H2/PostgreSQL subsiste (types, tri, fonctions) : un test vert en local
  peut échouer en staging. Mitigation : contraintes et migrations Flyway
  identiques partout, et tests de bout en bout exécutés sur PostgreSQL réel.
- Toute évolution de schéma exige une migration Flyway explicite, rétro-compatible
  d'une version (règle *expand/contract* imposée par EX-NF-02).

## Alternatives rejetées

- **`ddl-auto=update`** : rejeté sans hésitation. Aucun historique, aucun
  rollback, comportement dépendant du dialecte — incompatible avec un
  déploiement sans coupure où deux versions coexistent.
- **Domaine annoté JPA** : rejeté car il subordonne les règles métier aux
  contraintes du framework de persistance et rend les tests de domaine
  dépendants d'un contexte.
- **PostgreSQL partout, y compris en test** : envisageable via conteneurs
  éphémères, mais alourdit le cycle de feedback local. À réévaluer si l'écart
  H2/PostgreSQL cause des incidents.
