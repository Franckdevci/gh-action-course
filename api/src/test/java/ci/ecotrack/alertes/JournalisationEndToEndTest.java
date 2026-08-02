package ci.ecotrack.alertes;

import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.releves.StatutParcelleChange;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class JournalisationEndToEndTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AlertesRepository alertesRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void nettoyer() {
        jdbc.execute("DELETE FROM alerte");
        jdbc.execute("DELETE FROM event_publication");
        jdbc.execute("DELETE FROM releve");
        jdbc.execute("DELETE FROM parcelle");
    }

    @Test
    void should_ne_pas_marquer_traite_when_listener_throw_sur_code_invalide() {
        UUID parcelleId = insererParcelleDeReference();
        StatutParcelleChange evtInvalide = new StatutParcelleChange(
                parcelleId,
                "CODE-INVALIDE",
                StatutParcelle.EN_SUIVI,
                StatutParcelle.EN_ALERTE,
                new TauxDeSurvie(new BigDecimal("0.5000")),
                LocalDate.of(2026, 7, 20),
                Instant.parse("2026-07-20T10:15:30Z"));

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(evtInvalide));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Long publications = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM event_publication", Long.class);
            assertThat(publications).isEqualTo(1L);
            Long completes = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL",
                    Long.class);
            assertThat(completes).isEqualTo(0L);
        });

        AlertesRepository.PageEntreesJournal page =
                alertesRepository.listerAntichronologique(new Pagination(0, 50));
        assertThat(page.total()).isEqualTo(0L);
    }

    @Test
    void should_journaliser_alerte_when_crash_apres_enregistrement() {
        UUID parcelleId = insererParcelleDeReference();
        StatutParcelleChange evt = new StatutParcelleChange(
                parcelleId,
                "PRC-2026-777",
                StatutParcelle.EN_SUIVI,
                StatutParcelle.EN_ALERTE,
                new TauxDeSurvie(new BigDecimal("0.5995")),
                LocalDate.of(2026, 7, 20),
                Instant.parse("2026-07-20T10:15:30Z"));

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(evt));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            AlertesRepository.PageEntreesJournal page =
                    alertesRepository.listerAntichronologique(new Pagination(0, 50));
            assertThat(page.total()).isEqualTo(1);
            assertThat(page.contenu().get(0).code()).isEqualTo("PRC-2026-777");
            assertThat(page.contenu().get(0).nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Long complets = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL",
                    Long.class);
            assertThat(complets).isEqualTo(1L);
        });
    }

    private UUID insererParcelleDeReference() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO parcelle (id, code, localite, superficie, plants_initiaux, "
                        + "date_plantation, statut) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, "PRC-2026-777", "Bingerville",
                new BigDecimal("12.50"), 2000,
                LocalDate.of(2026, 6, 15), "EN_SUIVI");
        return id;
    }
}
