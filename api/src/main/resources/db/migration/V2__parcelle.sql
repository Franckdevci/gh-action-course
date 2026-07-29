CREATE TABLE parcelle (
    id              UUID          NOT NULL,
    code            VARCHAR(20)   NOT NULL,
    localite        VARCHAR(100)  NOT NULL,
    superficie      NUMERIC(7, 2) NOT NULL,
    plants_initiaux INTEGER       NOT NULL,
    date_plantation DATE          NOT NULL,
    statut          VARCHAR(20)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT parcelle_code_unique UNIQUE (code)
);
