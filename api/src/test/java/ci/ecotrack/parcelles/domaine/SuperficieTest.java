package ci.ecotrack.parcelles.domaine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperficieTest {

    @Test
    void should_construire_when_borne_min_0_01() {
        Superficie s = new Superficie(new BigDecimal("0.01"));

        assertThat(s.valeur()).isEqualByComparingTo("0.01");
    }

    @Test
    void should_construire_when_borne_max_10000_00() {
        Superficie s = new Superficie(new BigDecimal("10000.00"));

        assertThat(s.valeur()).isEqualByComparingTo("10000.00");
    }

    @Test
    void should_construire_when_valeur_courante() {
        Superficie s = new Superficie(new BigDecimal("12.50"));

        assertThat(s.valeur()).isEqualByComparingTo("12.50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12.5", "0.10"})
    void should_construire_when_scale_inferieur_ou_egal_a_2(String valeur) {
        Superficie s = new Superficie(new BigDecimal(valeur));

        assertThat(s.valeur()).isEqualByComparingTo(valeur);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1"})
    void should_rejeter_when_inferieur_a_borne_min(String valeur) {
        assertThatThrownBy(() -> new Superficie(new BigDecimal(valeur)))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("0.01");
    }

    @Test
    void should_rejeter_when_superieur_a_borne_max() {
        assertThatThrownBy(() -> new Superficie(new BigDecimal("10000.01")))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("10000");
    }

    @Test
    void should_rejeter_when_plus_de_2_decimales() {
        assertThatThrownBy(() -> new Superficie(new BigDecimal("12.501")))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("decimales");
    }

    @Test
    void should_rejeter_when_null() {
        assertThatThrownBy(() -> new Superficie(null))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }
}
