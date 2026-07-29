package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.shared.StatutParcelle;

import java.math.BigDecimal;
import java.time.LocalDate;

record ParcelleResponse(
        String code,
        String localite,
        BigDecimal superficie,
        Integer plantsInitiaux,
        LocalDate datePlantation,
        StatutParcelle statut,
        BigDecimal dernierTaux,
        LocalDate dateDernierReleve) {

    static ParcelleResponse de(Parcelle parcelle) {
        return new ParcelleResponse(
                parcelle.code().valeur(),
                parcelle.localite().valeur(),
                parcelle.superficie().valeur(),
                parcelle.plantsInitiaux().valeur(),
                parcelle.datePlantation(),
                parcelle.statut(),
                null,
                null);
    }
}
