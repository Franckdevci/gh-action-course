package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Superficie;

import java.time.LocalDate;

public record CreerParcelleCommande(
        CodeParcelle code,
        Localite localite,
        Superficie superficie,
        NombrePlants plantsInitiaux,
        LocalDate datePlantation) {
}
