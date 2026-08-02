package ci.ecotrack.alertes.infrastructure.rest;

import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.alertes.domaine.SensDeBascule;
import ci.ecotrack.shared.StatutParcelle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record AlerteResponse(
        UUID id,
        String code,
        SensDeBascule sens,
        StatutParcelle ancienStatut,
        StatutParcelle nouveauStatut,
        String tauxDeclencheur,
        LocalDate dateReleve,
        Instant survenuLe) {

    private static final BigDecimal CENT = new BigDecimal("100");

    static AlerteResponse de(EntreeJournal e) {
        String tauxFormate = e.tauxDeclencheur().valeur()
                .multiply(CENT)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString();
        return new AlerteResponse(
                e.id().valeur(),
                e.code(),
                e.sens(),
                e.ancienStatut(),
                e.nouveauStatut(),
                tauxFormate,
                e.dateReleve(),
                e.survenuLe());
    }
}
