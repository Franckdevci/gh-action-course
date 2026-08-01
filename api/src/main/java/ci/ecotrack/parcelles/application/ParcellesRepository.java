package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;

import java.util.Optional;
import java.util.UUID;

public interface ParcellesRepository {

    Parcelle enregistrer(Parcelle parcelle);

    Optional<Parcelle> trouverParCode(CodeParcelle code);

    Optional<Parcelle> trouverParId(UUID id);

    void sauvegarder(Parcelle parcelle);
}
