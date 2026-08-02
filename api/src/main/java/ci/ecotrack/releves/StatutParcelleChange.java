package ci.ecotrack.releves;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StatutParcelleChange(
        UUID parcelleId,
        String code,
        StatutParcelle ancienStatut,
        StatutParcelle nouveauStatut,
        TauxDeSurvie tauxDeclencheur,
        LocalDate dateReleve,
        Instant survenuLe) {
}
