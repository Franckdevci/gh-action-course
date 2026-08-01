package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.domaine.Releve;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

record ReleveResponse(
        String id,
        LocalDate dateObservation,
        int plantsVivants,
        String tauxSurvie) {

    private static final BigDecimal CENT = new BigDecimal("100");

    static ReleveResponse de(Releve releve) {
        String tauxFormate = releve.tauxDeSurvie().valeur()
                .multiply(CENT)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString();
        return new ReleveResponse(
                releve.id().valeur().toString(),
                releve.dateObservation().valeur(),
                releve.plantsVivants().valeur(),
                tauxFormate);
    }
}
