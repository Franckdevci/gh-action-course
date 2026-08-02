package ci.ecotrack.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TauxDeSurvieTest {

    @Test
    void should_estCritique_when_taux_est_5995_pourcent() {
        TauxDeSurvie t = new TauxDeSurvie(new BigDecimal("0.5995"));

        assertThat(t.estCritique()).isTrue();
    }

    @Test
    void should_ne_pas_etre_critique_when_taux_exactement_60_pourcent() {
        TauxDeSurvie t = new TauxDeSurvie(new BigDecimal("0.60"));

        assertThat(t.estCritique()).isFalse();
    }

    @Test
    void should_ne_pas_etre_critique_when_taux_exactement_60_pourcent_ecrit_avec_scale_different() {
        // 0.60 == 0.6000 en compareTo, jamais en equals
        TauxDeSurvie t = new TauxDeSurvie(new BigDecimal("0.6000"));

        assertThat(t.estCritique()).isFalse();
    }

    @Test
    void should_construire_when_scale_4() {
        TauxDeSurvie t = new TauxDeSurvie(new BigDecimal("0.5995"));

        assertThat(t.valeur()).isEqualByComparingTo("0.5995");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.6", "0.60", "0.600"})
    void should_construire_when_scale_inferieur_a_4(String valeur) {
        assertThat(new TauxDeSurvie(new BigDecimal(valeur)).valeur())
                .isEqualByComparingTo(valeur);
    }

    @Test
    void should_construire_when_borne_min_zero() {
        assertThat(new TauxDeSurvie(new BigDecimal("0.0000")).valeur())
                .isEqualByComparingTo("0");
    }

    @Test
    void should_construire_when_borne_max_un() {
        assertThat(new TauxDeSurvie(new BigDecimal("1.0000")).valeur())
                .isEqualByComparingTo("1");
    }

    @Test
    void should_rejeter_when_null() {
        assertThatThrownBy(() -> new TauxDeSurvie(null))
                .isInstanceOf(DonneeInvalideException.class);
    }

    @Test
    void should_rejeter_when_scale_superieur_a_4() {
        assertThatThrownBy(() -> new TauxDeSurvie(new BigDecimal("0.59955")))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("decimales");
    }

    @Test
    void should_rejeter_when_negatif() {
        assertThatThrownBy(() -> new TauxDeSurvie(new BigDecimal("-0.0001")))
                .isInstanceOf(DonneeInvalideException.class);
    }

    @Test
    void should_rejeter_when_superieur_a_un() {
        assertThatThrownBy(() -> new TauxDeSurvie(new BigDecimal("1.0001")))
                .isInstanceOf(DonneeInvalideException.class);
    }
}
