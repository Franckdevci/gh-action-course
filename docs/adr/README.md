# Architecture Decision Records (ADR) — EcoTrack

Ce répertoire consigne les décisions structurantes du projet EcoTrack. Chaque
décision est numérotée, datée, et référencée depuis le SDD (`docs/sdd.md`).

Format : MADR allégé (Statut · Contexte · Décision · Conséquences · Alternatives).

## Index

| N° | Titre | Statut | Référencée dans |
|---|---|---|---|
| [ADR-001](adr-001-spring-modulith.md) | Backend en Spring Modulith | Acceptée | SDD §2.1 |
| [ADR-002](adr-002-separation-domaine-jpa.md) | Séparation stricte domaine / JPA | Acceptée | SDD §3.4 |
| [ADR-003](adr-003-communication-par-event.md) | Communication `releves` → `alertes` par event uniquement | Acceptée | SDD §2.2 |
| [ADR-004](adr-004-feature-flags-backend.md) | Feature flags : backend seul dépositaire | Acceptée | SDD §5.3 |

## Règles

- Une ADR n'est **jamais modifiée sur le fond** une fois acceptée. Pour la
  remettre en cause, on ouvre une nouvelle ADR qui la **remplace** (statut
  `Superseded by ADR-N`).
- Toute décision structurante référencée dans le SDD doit avoir son ADR ici.
- Numérotation strictement séquentielle (`ADR-001`, `ADR-002`, …).
