package ci.ecotrack.parcelles.domaine;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelleIdTest {

    @Test
    void should_construire_when_uuid_fourni() {
        UUID uuid = UUID.randomUUID();

        ParcelleId id = new ParcelleId(uuid);

        assertThat(id.valeur()).isEqualTo(uuid);
    }

    @Test
    void should_generer_un_uuid_when_appel_de_nouveau() {
        ParcelleId id = ParcelleId.nouveau();

        assertThat(id.valeur()).isNotNull();
    }

    @Test
    void should_generer_des_ids_distincts_when_deux_appels_de_nouveau() {
        ParcelleId a = ParcelleId.nouveau();
        ParcelleId b = ParcelleId.nouveau();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void should_rejeter_when_uuid_null() {
        assertThatThrownBy(() -> new ParcelleId(null))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }
}
