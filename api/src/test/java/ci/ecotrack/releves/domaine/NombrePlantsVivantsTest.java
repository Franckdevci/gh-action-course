package ci.ecotrack.releves.domaine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NombrePlantsVivantsTest {

    @Test
    void should_rejeter_when_valeur_negative() {
        assertThatThrownBy(() -> new NombrePlantsVivants(-1))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("positif");
    }

    @Test
    void should_accepter_when_valeur_zero() {
        assertThat(new NombrePlantsVivants(0).valeur()).isZero();
    }

    @Test
    void should_accepter_when_valeur_egale_plants_initiaux() {
        NombrePlantsVivants n = new NombrePlantsVivants(2000);

        n.validerContrePlantsInitiaux(2000);

        assertThat(n.valeur()).isEqualTo(2000);
    }

    @Test
    void should_rejeter_when_valeur_superieure_plants_initiaux() {
        NombrePlantsVivants n = new NombrePlantsVivants(2001);

        assertThatThrownBy(() -> n.validerContrePlantsInitiaux(2000))
                .isInstanceOf(DonneeReleveInvalideException.class)
                .hasMessageContaining("depasser");
    }
}
