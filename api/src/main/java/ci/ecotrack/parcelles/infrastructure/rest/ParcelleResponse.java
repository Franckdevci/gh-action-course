package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.shared.StatutParcelle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

record ParcelleResponse(
        String code,
        String localite,
        BigDecimal superficie,
        Integer plantsInitiaux,
        LocalDate datePlantation,
        StatutParcelle statut,
        String dernierTaux,
        LocalDate dateDernierReleve) {

    static ParcelleResponse de(Parcelle parcelle) {
        String tauxFormate = parcelle.dernierTaux() == null
                ? null
                : String.format(Locale.US, "%.1f",
                        parcelle.dernierTaux().valeur().doubleValue() * 100.0);
        return new ParcelleResponse(
                parcelle.code().valeur(),
                parcelle.localite().valeur(),
                parcelle.superficie().valeur(),
                parcelle.plantsInitiaux().valeur(),
                parcelle.datePlantation(),
                parcelle.statut(),
                tauxFormate,
                parcelle.dateDernierReleve());
    }
}
