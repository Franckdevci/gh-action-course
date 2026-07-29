package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.Parcelle;

public interface ParcellesRepository {
    Parcelle enregistrer(Parcelle parcelle);
}
