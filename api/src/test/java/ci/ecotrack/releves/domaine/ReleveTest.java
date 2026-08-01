package ci.ecotrack.releves.domaine;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleveTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private static final UUID PARCELLE_ID = UUID.randomUUID();
    private static final LocalDate DATE_PLANTATION = LocalDate.of(2026, 6, 15);
    private static final LocalDate DATE_OBSERVATION = LocalDate.of(2026, 7, 20);

    @Test
    void should_calculer_taux_nominal_when_1700_sur_2000() {
        Releve releve = Releve.enregistrer(
                PARCELLE_ID,
                new DateObservation(DATE_OBSERVATION),
                new NombrePlantsVivants(1700),
                2000,
                DATE_PLANTATION,
                HORLOGE);

        assertThat(releve.tauxDeSurvie().valeur()).isEqualByComparingTo("0.8500");
        assertThat(releve.parcelleId()).isEqualTo(PARCELLE_ID);
    }

    @Test
    void should_calculer_taux_5995_when_1199_sur_2000() {
        Releve releve = Releve.enregistrer(
                PARCELLE_ID,
                new DateObservation(DATE_OBSERVATION),
                new NombrePlantsVivants(1199),
                2000,
                DATE_PLANTATION,
                HORLOGE);

        assertThat(releve.tauxDeSurvie().valeur()).isEqualByComparingTo("0.5995");
    }

    @Test
    void should_calculer_taux_6000_when_1200_sur_2000() {
        Releve releve = Releve.enregistrer(
                PARCELLE_ID,
                new DateObservation(DATE_OBSERVATION),
                new NombrePlantsVivants(1200),
                2000,
                DATE_PLANTATION,
                HORLOGE);

        assertThat(releve.tauxDeSurvie().valeur()).isEqualByComparingTo("0.6000");
    }

    @Test
    void should_rejeter_when_parcelle_id_null() {
        DateObservation dateObs = new DateObservation(DATE_OBSERVATION);
        NombrePlantsVivants plants = new NombrePlantsVivants(1700);

        assertThatThrownBy(() -> Releve.enregistrer(null, dateObs, plants, 2000, DATE_PLANTATION, HORLOGE))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("parcelle");
    }
}
