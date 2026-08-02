package ci.ecotrack.alertes.domaine;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntreeJournalTest {

    private static final UUID PARCELLE_ID = UUID.randomUUID();
    private static final String CODE = "PRC-2026-042";
    private static final TauxDeSurvie TAUX = new TauxDeSurvie(new BigDecimal("0.5995"));
    private static final LocalDate DATE_RELEVE = LocalDate.of(2026, 7, 20);
    private static final Instant SURVENU_LE = Instant.parse("2026-07-20T10:15:30Z");

    @Test
    void should_creer_entree_passage_alerte_when_ancien_en_suivi_et_nouveau_en_alerte() {
        EntreeJournal entree = EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE);

        assertThat(entree.id()).isNotNull();
        assertThat(entree.parcelleId()).isEqualTo(PARCELLE_ID);
        assertThat(entree.code()).isEqualTo(CODE);
        assertThat(entree.sens()).isEqualTo(SensDeBascule.PASSAGE_EN_ALERTE);
        assertThat(entree.ancienStatut()).isEqualTo(StatutParcelle.EN_SUIVI);
        assertThat(entree.nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
        assertThat(entree.tauxDeclencheur()).isEqualTo(TAUX);
        assertThat(entree.dateReleve()).isEqualTo(DATE_RELEVE);
        assertThat(entree.survenuLe()).isEqualTo(SURVENU_LE);
    }

    @Test
    void should_creer_entree_retablissement_when_ancien_en_alerte_et_nouveau_en_suivi() {
        EntreeJournal entree = EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_ALERTE, StatutParcelle.EN_SUIVI,
                new TauxDeSurvie(new BigDecimal("0.6500")),
                DATE_RELEVE, SURVENU_LE);

        assertThat(entree.sens()).isEqualTo(SensDeBascule.RETABLISSEMENT);
    }

    @Test
    void should_rejeter_when_ancien_egale_nouveau() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_ALERTE, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("bascule");
    }

    @Test
    void should_rejeter_when_parcelle_id_null() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                null, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("parcelle");
    }

    @Test
    void should_rejeter_when_code_null_ou_blanc() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, "   ",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("code");
    }

    @Test
    void should_rejeter_when_code_ne_respecte_pas_le_format() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, "PARCELLE-42",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("PRC-AAAA-NNN");
    }

    @Test
    void should_rejeter_when_code_contient_saut_de_ligne() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, "PRC-2026-042\ninjection",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class);
    }

    @Test
    void should_rejeter_when_taux_null() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                null, DATE_RELEVE, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("taux");
    }

    @Test
    void should_rejeter_when_date_releve_null() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, null, SURVENU_LE))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("date");
    }

    @Test
    void should_rejeter_when_survenu_le_null() {
        assertThatThrownBy(() -> EntreeJournal.consigner(
                PARCELLE_ID, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, null))
                .isInstanceOf(BasculeInvalideException.class)
                .hasMessageContaining("survenu");
    }

    @Test
    void should_reconstituer_sans_valider_les_bornes_metier() {
        UUID id = UUID.randomUUID();
        EntreeJournal entree = EntreeJournal.reconstituer(
                id, PARCELLE_ID, CODE,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                TAUX, DATE_RELEVE, SURVENU_LE);

        assertThat(entree.id().valeur()).isEqualTo(id);
        assertThat(entree.sens()).isEqualTo(SensDeBascule.PASSAGE_EN_ALERTE);
    }
}
