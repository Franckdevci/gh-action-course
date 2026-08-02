package ci.ecotrack.alertes.infrastructure.jpa;

import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.alertes.domaine.SensDeBascule;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAlertesRepository.class)
class JpaAlertesRepositoryTest {

    @Autowired
    private AlertesRepository repository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @BeforeEach
    void nettoyer() {
        jdbc.execute("DELETE FROM alerte");
        jdbc.execute("DELETE FROM releve");
        jdbc.execute("DELETE FROM parcelle");
    }

    @Test
    void should_persister_une_entree_et_la_relire_dans_la_page() {
        UUID parcelleId = insererParcelleDeReference("PRC-2026-001");
        EntreeJournal entree = uneBascule(parcelleId, "PRC-2026-001",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                "0.5995",
                Instant.parse("2026-07-20T10:15:30Z"));

        repository.enregistrer(entree);

        AlertesRepository.PageEntreesJournal page = repository.listerAntichronologique(new Pagination(0, 50));
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.contenu()).hasSize(1);
        assertThat(page.contenu().get(0).code()).isEqualTo("PRC-2026-001");
        assertThat(page.contenu().get(0).sens()).isEqualTo(SensDeBascule.PASSAGE_EN_ALERTE);
        assertThat(page.contenu().get(0).tauxDeclencheur().valeur()).isEqualByComparingTo("0.5995");
    }

    @Test
    void should_trier_antichronologique_when_plusieurs_entrees() {
        UUID p1 = insererParcelleDeReference("PRC-2026-010");
        UUID p2 = insererParcelleDeReference("PRC-2026-011");

        repository.enregistrer(uneBascule(p1, "PRC-2026-010",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE, "0.5000",
                Instant.parse("2026-07-01T08:00:00Z")));
        repository.enregistrer(uneBascule(p2, "PRC-2026-011",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE, "0.4000",
                Instant.parse("2026-07-15T08:00:00Z")));
        repository.enregistrer(uneBascule(p1, "PRC-2026-010",
                StatutParcelle.EN_ALERTE, StatutParcelle.EN_SUIVI, "0.7500",
                Instant.parse("2026-07-10T08:00:00Z")));

        AlertesRepository.PageEntreesJournal page = repository.listerAntichronologique(new Pagination(0, 50));

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.contenu())
                .extracting(e -> e.survenuLe().toString())
                .containsExactly(
                        "2026-07-15T08:00:00Z",
                        "2026-07-10T08:00:00Z",
                        "2026-07-01T08:00:00Z");
    }

    @Test
    void should_paginer_when_plus_d_entrees_que_size() {
        UUID p = insererParcelleDeReference("PRC-2026-050");
        for (int i = 0; i < 5; i++) {
            StatutParcelle ancien = i % 2 == 0 ? StatutParcelle.EN_SUIVI : StatutParcelle.EN_ALERTE;
            StatutParcelle nouveau = i % 2 == 0 ? StatutParcelle.EN_ALERTE : StatutParcelle.EN_SUIVI;
            repository.enregistrer(uneBascule(p, "PRC-2026-050", ancien, nouveau,
                    "0.5" + i + "00",
                    Instant.parse("2026-07-0" + (i + 1) + "T08:00:00Z")));
        }

        AlertesRepository.PageEntreesJournal page0 = repository.listerAntichronologique(new Pagination(0, 2));
        AlertesRepository.PageEntreesJournal page1 = repository.listerAntichronologique(new Pagination(1, 2));

        assertThat(page0.contenu()).hasSize(2);
        assertThat(page1.contenu()).hasSize(2);
        assertThat(page0.total()).isEqualTo(5);
        assertThat(page0.contenu().get(0).survenuLe()).isAfter(page1.contenu().get(0).survenuLe());
    }

    @Test
    void should_rejeter_bascule_incoherente_ancien_egal_nouveau_when_ecriture_directe_bdd() {
        UUID p = insererParcelleDeReference("PRC-2026-080");
        assertThat(
                assertThatThrownWhenInsertingAlerte(p, "EN_ALERTE", "EN_ALERTE"))
                .isTrue();
    }

    private boolean assertThatThrownWhenInsertingAlerte(UUID parcelleId,
                                                        String ancienStatut,
                                                        String nouveauStatut) {
        try {
            jdbc.update("INSERT INTO alerte (id, parcelle_id, code, sens, ancien_statut, "
                            + "nouveau_statut, taux_declencheur, date_releve, survenu_le) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), parcelleId, "PRC-2026-080", "PASSAGE_EN_ALERTE",
                    ancienStatut, nouveauStatut,
                    new BigDecimal("0.5000"),
                    LocalDate.of(2026, 7, 20),
                    java.sql.Timestamp.from(Instant.parse("2026-07-20T10:15:30Z")));
            return false;
        } catch (org.springframework.dao.DataAccessException expected) {
            return true;
        }
    }

    @Test
    void should_ignorer_doublon_when_meme_parcelle_et_survenuLe() {
        UUID p = insererParcelleDeReference("PRC-2026-060");
        Instant survenuLe = Instant.parse("2026-07-20T10:15:30Z");

        EntreeJournal premier = repository.enregistrer(uneBascule(p, "PRC-2026-060",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE, "0.5000", survenuLe));
        EntreeJournal doublon = repository.enregistrer(uneBascule(p, "PRC-2026-060",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE, "0.5000", survenuLe));

        AlertesRepository.PageEntreesJournal page = repository.listerAntichronologique(new Pagination(0, 50));
        assertThat(page.total()).isEqualTo(1);
        assertThat(doublon.id().valeur()).isEqualTo(premier.id().valeur());
    }

    @Test
    void should_normaliser_taux_when_valeur_bdd_a_scale_superieur_a_4() {
        UUID p = insererParcelleDeReference("PRC-2026-070");
        UUID alerteId = UUID.randomUUID();
        jdbc.update("INSERT INTO alerte (id, parcelle_id, code, sens, ancien_statut, "
                        + "nouveau_statut, taux_declencheur, date_releve, survenu_le) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                alerteId, p, "PRC-2026-070", "PASSAGE_EN_ALERTE", "EN_SUIVI", "EN_ALERTE",
                new BigDecimal("0.5000"),
                LocalDate.of(2026, 7, 20),
                java.sql.Timestamp.from(Instant.parse("2026-07-20T10:15:30Z")));

        AlertesRepository.PageEntreesJournal page = repository.listerAntichronologique(new Pagination(0, 50));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.contenu().get(0).tauxDeclencheur().valeur().scale()).isEqualTo(4);
    }

    private UUID insererParcelleDeReference(String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO parcelle (id, code, localite, superficie, plants_initiaux, "
                        + "date_plantation, statut) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, code, "Bingerville", new BigDecimal("12.50"), 2000,
                LocalDate.of(2026, 6, 15), "EN_SUIVI");
        return id;
    }

    private EntreeJournal uneBascule(UUID parcelleId,
                                      String code,
                                      StatutParcelle ancien,
                                      StatutParcelle nouveau,
                                      String taux,
                                      Instant survenuLe) {
        return EntreeJournal.consigner(
                parcelleId, code,
                ancien, nouveau,
                new TauxDeSurvie(new BigDecimal(taux)),
                LocalDate.of(2026, 7, 20),
                survenuLe);
    }
}
