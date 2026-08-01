package ci.ecotrack.releves.application;

import java.time.LocalDate;

public record EnregistrerReleveCommande(
        String codeParcelle,
        LocalDate dateObservation,
        int plantsVivants) {
}
