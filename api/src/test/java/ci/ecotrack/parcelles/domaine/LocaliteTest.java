package ci.ecotrack.parcelles.domaine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocaliteTest {

    @Test
    void should_construire_when_texte_courant() {
        Localite localite = new Localite("Bingerville");

        assertThat(localite.valeur()).isEqualTo("Bingerville");
    }

    @Test
    void should_trim_les_espaces_perimetriques_when_texte_encadre_par_des_espaces() {
        Localite localite = new Localite("  Adzope  ");

        assertThat(localite.valeur()).isEqualTo("Adzope");
    }

    @Test
    void should_accepter_when_pile_100_caracteres() {
        String cent = "a".repeat(100);

        Localite localite = new Localite(cent);

        assertThat(localite.valeur()).hasSize(100);
    }

    @Test
    void should_rejeter_when_null() {
        assertThatThrownBy(() -> new Localite(null))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("localite");
    }

    @Test
    void should_rejeter_when_chaine_vide() {
        assertThatThrownBy(() -> new Localite(""))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }

    @Test
    void should_rejeter_when_100_espaces_car_vide_apres_trim() {
        String cent_espaces = " ".repeat(100);

        assertThatThrownBy(() -> new Localite(cent_espaces))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }

    @Test
    void should_rejeter_when_101_caracteres() {
        String cent_un = "a".repeat(101);

        assertThatThrownBy(() -> new Localite(cent_un))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("100");
    }

    @Test
    void should_rejeter_when_contient_null_byte() {
        assertThatThrownBy(() -> new Localite("Bing\0erville"))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("NUL");
    }

    @Test
    void should_rejeter_when_contient_rtl_override() {
        assertThatThrownBy(() -> new Localite("Bing‮erville"))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("directionnel");
    }

    @Test
    void should_rejeter_when_contient_lrm_isolate() {
        assertThatThrownBy(() -> new Localite("Bing⁦erville"))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("directionnel");
    }

    @Test
    void should_rejeter_when_contient_caractere_controle() {
        assertThatThrownBy(() -> new Localite("Bingerville"))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("controle");
    }

    @Test
    void should_accepter_when_contient_tab_ou_newline() {
        Localite avec_tab = new Localite("Ligne 1\tLigne 2");
        Localite avec_newline = new Localite("Ligne 1\nLigne 2");

        assertThat(avec_tab.valeur()).contains("\t");
        assertThat(avec_newline.valeur()).contains("\n");
    }
}
