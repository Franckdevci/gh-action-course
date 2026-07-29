package ci.ecotrack.parcelles.domaine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeParcelleTest {

    @ParameterizedTest
    @ValueSource(strings = {"PRC-2026-042", "PRC-2026-1", "PRC-2026-999", "PRC-1999-1"})
    void should_construire_when_format_valide(String valeur) {
        CodeParcelle code = new CodeParcelle(valeur);

        assertThat(code.valeur()).isEqualTo(valeur);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-042",           // prefixe absent
            "PRC-26-042",         // annee pas sur 4 chiffres
            "PRC-2026-1000",      // numero sur 4 chiffres
            "PRC-2026-",          // numero absent
            "prc-2026-042",       // casse
            " PRC-2026-042",      // espace en tete
            "PRC-2026-042 "       // espace en fin
    })
    void should_rejeter_when_format_invalide(String valeur) {
        assertThatThrownBy(() -> new CodeParcelle(valeur))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("PRC-AAAA-NNN");
    }

    @Test
    void should_rejeter_when_null() {
        assertThatThrownBy(() -> new CodeParcelle(null))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }

    @Test
    void should_rejeter_when_chaine_vide() {
        assertThatThrownBy(() -> new CodeParcelle(""))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }
}
