package ci.ecotrack.parcelles;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.time.LocalDate;

public record StatutChange(
        StatutParcelle ancienStatut,
        StatutParcelle nouveauStatut,
        TauxDeSurvie tauxDeclencheur,
        LocalDate dateDeclencheur) {
}
