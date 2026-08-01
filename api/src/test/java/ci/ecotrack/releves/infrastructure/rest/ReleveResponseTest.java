package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.releves.domaine.ReleveId;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleveResponseTest {

    @ParameterizedTest(name = "taux {0} → \"{1}\"")
    @CsvSource({
            "0.5994, 59.9",
            "0.5995, 60.0",
            "0.6000, 60.0",
            "0.8500, 85.0",
            "1.0000, 100.0",
            "0.0000, 0.0"
    })
    void should_formater_taux_survie_en_pourcentage_arrondi_half_up(
            String valeurBrute, String attendu) {
        Releve releve = Releve.reconstituer(
                ReleveId.nouveau(),
                UUID.randomUUID(),
                new DateObservation(LocalDate.of(2026, 7, 20)),
                new NombrePlantsVivants(1200),
                new TauxDeSurvie(new BigDecimal(valeurBrute)));

        ReleveResponse response = ReleveResponse.de(releve);

        assertThat(response.tauxSurvie()).isEqualTo(attendu);
    }
}
