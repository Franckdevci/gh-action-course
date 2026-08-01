package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.ParcelleId;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ParcelleResponseTest {

    @ParameterizedTest(name = "taux {0} → \"{1}\"")
    @CsvSource({
            "0.5994, 59.9",
            "0.5995, 60.0",
            "0.6000, 60.0",
            "0.8500, 85.0",
            "1.0000, 100.0",
            "0.0000, 0.0"
    })
    void should_formater_dernier_taux_en_pourcentage_arrondi_half_up(
            String valeurBrute, String attendu) {
        Parcelle parcelle = parcelleAvecTaux(new BigDecimal(valeurBrute));

        ParcelleResponse response = ParcelleResponse.de(parcelle);

        assertThat(response.dernierTaux()).isEqualTo(attendu);
    }

    @org.junit.jupiter.api.Test
    void should_retourner_null_dernier_taux_when_aucun_releve() {
        Parcelle parcelle = Parcelle.reconstituer(
                ParcelleId.nouveau(),
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                StatutParcelle.EN_SUIVI,
                null,
                null);

        ParcelleResponse response = ParcelleResponse.de(parcelle);

        assertThat(response.dernierTaux()).isNull();
    }

    private static Parcelle parcelleAvecTaux(BigDecimal valeur) {
        return Parcelle.reconstituer(
                ParcelleId.nouveau(),
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                StatutParcelle.EN_SUIVI,
                new TauxDeSurvie(valeur),
                LocalDate.of(2026, 7, 20));
    }
}
