CREATE TABLE releve (
    id              UUID          NOT NULL,
    parcelle_id     UUID          NOT NULL,
    date_observation DATE         NOT NULL,
    plants_vivants  INTEGER       NOT NULL,
    taux_survie     NUMERIC(5, 4) NOT NULL,
    cree_le         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT releve_parcelle_date_unique UNIQUE (parcelle_id, date_observation),
    CONSTRAINT releve_parcelle_fk FOREIGN KEY (parcelle_id) REFERENCES parcelle(id)
);

CREATE INDEX releve_parcelle_date_desc_idx ON releve (parcelle_id, date_observation DESC);
