package ci.ecotrack.parcelles.domaine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NombrePlantsTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2000, Integer.MAX_VALUE})
    void should_construire_when_strictement_positif(int valeur) {
        NombrePlants n = new NombrePlants(valeur);

        assertThat(n.valeur()).isEqualTo(valeur);
    }

    @Test
    void should_rejeter_when_zero() {
        assertThatThrownBy(() -> new NombrePlants(0))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("strictement positif");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void should_rejeter_when_negatif(int valeur) {
        assertThatThrownBy(() -> new NombrePlants(valeur))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }
}
