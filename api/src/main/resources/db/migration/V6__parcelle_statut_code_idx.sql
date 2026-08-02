-- SDD §3.2 : index composite pour le tri par defaut de la liste EX-F-05
-- (EN_ALERTE d'abord puis code) — support EX-NF-01 P95 < 500 ms sur 5 000 parcelles.
CREATE INDEX parcelle_statut_code_idx ON parcelle (statut, code);
