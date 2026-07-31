# Architecture Decision Records (ADR) — EcoTrack

Ce répertoire consigne les décisions structurantes du projet EcoTrack. Chaque
décision est numérotée, datée, et tracée aux exigences du SRS (`docs/srs.md`)
et aux sections du SDD (`docs/sdd.md`) qui la mobilisent.

Format : MADR allégé (Statut · Date · Exigences concernées · Contexte · Décision · Conséquences · Alternatives).

## Index

| N° | Titre | Statut | Exigences principales |
|---|---|---|---|
| [ADR-001](ADR-001-spring-modulith.md) | Spring Modulith plutôt que microservices ou monolithe en couches | Accepté | Toutes (structurante), EX-NF-02 |
| [ADR-002](ADR-002-persistance-et-mapping.md) | H2 en développement, PostgreSQL en staging, domaine séparé de JPA | Accepté | EI-03, EX-NF-01, EX-NF-07, EX-F-01 R2, EX-F-02 R3 |
| [ADR-003](ADR-003-events-inter-modules.md) | Communication `releves` → `alertes` par event Spring Modulith | Accepté | EX-F-03 R2, EX-F-07, EX-NF-03 |
| [ADR-004](ADR-004-feature-flags.md) | Feature flags par propriétés Spring, exposés au front par l'API | Accepté | EX-F-04 R1, C2 |
| [ADR-005](ADR-005-denormalisation-dernier-releve.md) | Dénormalisation du dernier relevé sur la ligne `parcelle` | Accepté | EX-NF-01, EX-F-05, EX-F-04 |
| [ADR-006](ADR-006-plafonds-export-csv.md) | Plafonds et streaming de l'export CSV | Accepté | EX-F-04, EX-NF-01, SEC-03 |
| [ADR-007](ADR-007-retention-et-purges.md) | Rétention du journal des alertes et du registry d'events | Proposé | EX-F-07 R1, EX-NF-03, SEC-06, SEC-07 |
| [ADR-008](ADR-008-dlq-event-statut-parcelle.md) | Stratégie DLQ et rejeu de l'event `StatutParcelleChange` (complète ADR-003) | Proposé | EX-NF-03, EX-F-07 |
| [ADR-009](ADR-009-journalisation-applicative.md) | Politique de journalisation applicative | Proposé | EX-NF-06, SEC-08 |

## Règles

- Une ADR n'est **jamais modifiée sur le fond** une fois acceptée. Pour la
  remettre en cause, on ouvre une nouvelle ADR qui la **remplace** (statut
  `Superseded by ADR-N`).
- Toute décision structurante référencée dans le SDD doit avoir son ADR ici.
- Numérotation strictement séquentielle (`ADR-001`, `ADR-002`, …).
