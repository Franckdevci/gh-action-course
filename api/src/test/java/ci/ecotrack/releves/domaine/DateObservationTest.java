package ci.ecotrack.releves.domaine;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateObservationTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private static final LocalDate DATE_PLANTATION = LocalDate.of(2026, 6, 15);

    @Test
    void should_rejeter_when_valeur_null() {
        assertThatThrownBy(() -> new DateObservation(null))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("requise");
    }

    @Test
    void should_rejeter_when_date_dans_le_futur() {
        DateObservation demain = new DateObservation(LocalDate.of(2026, 7, 30));

        assertThatThrownBy(() -> demain.validerCoherenceAvec(DATE_PLANTATION, HORLOGE))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void should_rejeter_when_date_anterieure_a_plantation() {
        DateObservation avant = new DateObservation(LocalDate.of(2026, 6, 14));

        assertThatThrownBy(() -> avant.validerCoherenceAvec(DATE_PLANTATION, HORLOGE))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("anterieure");
    }

    @Test
    void should_accepter_when_date_egale_a_plantation() {
        DateObservation memeJour = new DateObservation(DATE_PLANTATION);

        memeJour.validerCoherenceAvec(DATE_PLANTATION, HORLOGE);

        assertThat(memeJour.valeur()).isEqualTo(DATE_PLANTATION);
    }

    @Test
    void should_accepter_when_date_egale_a_aujourdhui() {
        DateObservation aujourdhui = new DateObservation(LocalDate.of(2026, 7, 29));

        aujourdhui.validerCoherenceAvec(DATE_PLANTATION, HORLOGE);

        assertThat(aujourdhui.valeur()).isEqualTo(LocalDate.of(2026, 7, 29));
    }
}
