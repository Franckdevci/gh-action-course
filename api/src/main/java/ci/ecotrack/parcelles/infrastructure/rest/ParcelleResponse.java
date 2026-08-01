package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.shared.StatutParcelle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

record ParcelleResponse(
        String code,
        String localite,
        BigDecimal superficie,
        Integer plantsInitiaux,
        LocalDate datePlantation,
        StatutParcelle statut,
        String dernierTaux,
        LocalDate dateDernierReleve) {

    private static final BigDecimal CENT = new BigDecimal("100");

    static ParcelleResponse de(Parcelle parcelle) {
        String tauxFormate = parcelle.dernierTaux() == null
                ? null
                : parcelle.dernierTaux().valeur()
                        .multiply(CENT)
                        .setScale(1, RoundingMode.HALF_UP)
                        .toPlainString();
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
