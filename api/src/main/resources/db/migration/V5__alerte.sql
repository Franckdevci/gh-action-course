-- SEC-ELEV-01 hardening prod : à appliquer manuellement sur PostgreSQL avant Phase 10
--   REVOKE UPDATE, DELETE ON alerte FROM <role_app>;
--   La purge ADR-007 (24 mois) tournera sous <role_janitor> distinct.
-- Défense in-app : @Immutable + @Column(updatable=false) sur AlerteJpaEntity.

CREATE TABLE alerte (
    id               UUID          NOT NULL,
    parcelle_id      UUID          NOT NULL,
    code             VARCHAR(20)   NOT NULL,
    sens             VARCHAR(32)   NOT NULL,
    ancien_statut    VARCHAR(16)   NOT NULL,
    nouveau_statut   VARCHAR(16)   NOT NULL,
    taux_declencheur NUMERIC(5, 4) NOT NULL,
    date_releve      DATE          NOT NULL,
    survenu_le       TIMESTAMP WITH TIME ZONE NOT NULL,
    cree_le          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT alerte_parcelle_fk FOREIGN KEY (parcelle_id) REFERENCES parcelle(id),
    CONSTRAINT alerte_bascule_coherente CHECK (ancien_statut <> nouveau_statut),
    CONSTRAINT alerte_unique_bascule UNIQUE (parcelle_id, survenu_le)
);

CREATE INDEX alerte_survenu_le_desc_idx ON alerte (survenu_le DESC);
