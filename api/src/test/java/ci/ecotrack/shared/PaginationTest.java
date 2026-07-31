package ci.ecotrack.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationTest {

    @Test
    void should_construire_when_valeurs_valides() {
        Pagination p = new Pagination(0, 50);

        assertThat(p.page()).isZero();
        assertThat(p.size()).isEqualTo(50);
    }

    @Test
    void should_construire_when_size_borne_min_1() {
        assertThat(new Pagination(0, 1).size()).isEqualTo(1);
    }

    @Test
    void should_construire_when_size_borne_max_100() {
        assertThat(new Pagination(0, 100).size()).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void should_rejeter_when_page_negative(int page) {
        assertThatThrownBy(() -> new Pagination(page, 50))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("page");
    }

    @Test
    void should_rejeter_when_size_zero() {
        assertThatThrownBy(() -> new Pagination(0, 0))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("size");
    }

    @Test
    void should_rejeter_when_size_negatif() {
        assertThatThrownBy(() -> new Pagination(0, -5))
                .isInstanceOf(DonneeInvalideException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 500, 1000000, Integer.MAX_VALUE})
    void should_rejeter_when_size_superieur_a_100(int size) {
        assertThatThrownBy(() -> new Pagination(0, size))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("100");
    }
}
