package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.domaine.Releve;

import java.time.LocalDate;
import java.util.Locale;

record ReleveResponse(
        String id,
        LocalDate dateObservation,
        int plantsVivants,
        String tauxSurvie) {

    static ReleveResponse de(Releve releve) {
        String tauxFormate = String.format(Locale.US, "%.1f",
                releve.tauxDeSurvie().valeur().doubleValue() * 100.0);
        return new ReleveResponse(
                releve.id().valeur().toString(),
                releve.dateObservation().valeur(),
                releve.plantsVivants().valeur(),
                tauxFormate);
    }
}
