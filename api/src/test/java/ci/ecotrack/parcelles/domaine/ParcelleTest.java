package ci.ecotrack.parcelles.domaine;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelleTest {

    private static final Clock HORLOGE_FIGEE_29_JUILLET_2026 =
            Clock.fixed(LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
                    ZoneOffset.UTC);

    private static final CodeParcelle CODE = new CodeParcelle("PRC-2026-042");
    private static final Localite LOCALITE = new Localite("Bingerville");
    private static final Superficie SUPERFICIE = new Superficie(new BigDecimal("12.50"));
    private static final NombrePlants PLANTS = new NombrePlants(2000);

    @Test
    void should_avoir_le_statut_EN_SUIVI_when_parcelle_creee() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.statut()).isEqualTo(StatutParcelle.EN_SUIVI);
    }

    @Test
    void should_avoir_un_id_non_null_when_parcelle_creee() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.id()).isNotNull();
        assertThat(p.id().valeur()).isNotNull();
    }

    @Test
    void should_accepter_when_date_plantation_dans_le_passe() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2020, 1, 1), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.datePlantation()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void should_accepter_when_date_plantation_egale_a_aujourdhui() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 7, 29), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.datePlantation()).isEqualTo(LocalDate.of(2026, 7, 29));
    }

    @Test
    void should_rejeter_when_date_plantation_est_demain() {
        assertThatThrownBy(() -> Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 7, 30), HORLOGE_FIGEE_29_JUILLET_2026))
                .isInstanceOf(DonneeParcelleInvalideException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void should_rejeter_when_date_plantation_est_null() {
        assertThatThrownBy(() -> Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                null, HORLOGE_FIGEE_29_JUILLET_2026))
                .isInstanceOf(DonneeParcelleInvalideException.class);
    }

    @Test
    void should_exposer_les_caracteristiques_immuables_when_parcelle_creee() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.code()).isEqualTo(CODE);
        assertThat(p.localite()).isEqualTo(LOCALITE);
        assertThat(p.superficie()).isEqualTo(SUPERFICIE);
        assertThat(p.plantsInitiaux()).isEqualTo(PLANTS);
    }

    @Test
    void should_avoir_dernier_taux_null_when_parcelle_creee() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);

        assertThat(p.dernierTaux()).isNull();
        assertThat(p.dateDernierReleve()).isNull();
    }

    @Test
    void should_denormaliser_dernier_taux_when_premier_releve() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);
        TauxDeSurvie taux = new TauxDeSurvie(new BigDecimal("0.85"));

        p.enregistrerDernierReleve(taux, LocalDate.of(2026, 7, 20));

        assertThat(p.dernierTaux()).isEqualTo(taux);
        assertThat(p.dateDernierReleve()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void should_ignorer_when_releve_antidate() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);
        TauxDeSurvie recent = new TauxDeSurvie(new BigDecimal("0.85"));
        p.enregistrerDernierReleve(recent, LocalDate.of(2026, 7, 20));
        TauxDeSurvie ancien = new TauxDeSurvie(new BigDecimal("0.60"));

        p.enregistrerDernierReleve(ancien, LocalDate.of(2026, 6, 30));

        assertThat(p.dernierTaux()).isEqualTo(recent);
        assertThat(p.dateDernierReleve()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void should_ignorer_when_releve_meme_date() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);
        TauxDeSurvie premier = new TauxDeSurvie(new BigDecimal("0.85"));
        p.enregistrerDernierReleve(premier, LocalDate.of(2026, 7, 20));
        TauxDeSurvie second = new TauxDeSurvie(new BigDecimal("0.75"));

        p.enregistrerDernierReleve(second, LocalDate.of(2026, 7, 20));

        assertThat(p.dernierTaux()).isEqualTo(premier);
    }

    @Test
    void should_mettre_a_jour_when_releve_plus_recent() {
        Parcelle p = Parcelle.creer(CODE, LOCALITE, SUPERFICIE, PLANTS,
                LocalDate.of(2026, 6, 15), HORLOGE_FIGEE_29_JUILLET_2026);
        p.enregistrerDernierReleve(
                new TauxDeSurvie(new BigDecimal("0.85")), LocalDate.of(2026, 7, 20));
        TauxDeSurvie plusRecent = new TauxDeSurvie(new BigDecimal("0.70"));

        p.enregistrerDernierReleve(plusRecent, LocalDate.of(2026, 8, 15));

        assertThat(p.dernierTaux()).isEqualTo(plusRecent);
        assertThat(p.dateDernierReleve()).isEqualTo(LocalDate.of(2026, 8, 15));
    }
}
