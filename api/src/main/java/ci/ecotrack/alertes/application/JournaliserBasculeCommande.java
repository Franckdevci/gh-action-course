package ci.ecotrack.alertes.application;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournaliserBasculeCommande(
        UUID parcelleId,
        String codeParcelle,
        StatutParcelle ancienStatut,
        StatutParcelle nouveauStatut,
        TauxDeSurvie tauxDeclencheur,
        LocalDate dateReleve,
        Instant survenuLe) {
}
