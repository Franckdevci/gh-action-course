package ci.ecotrack.releves.infrastructure.jpa;

import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaRelevesRepository.class)
class JpaRelevesRepositoryTest {

    @Autowired
    private RelevesRepository repository;

    @Test
    void should_persister_un_releve_et_le_relire() {
        UUID parcelleId = insererParcelleDeReference();
        Releve releve = unReleve(parcelleId, LocalDate.of(2026, 7, 20), 1700);

        Releve persiste = repository.enregistrer(releve);

        assertThat(persiste.id()).isEqualTo(releve.id());
        assertThat(persiste.parcelleId()).isEqualTo(parcelleId);
        assertThat(persiste.tauxDeSurvie().valeur()).isEqualByComparingTo("0.8500");
    }

    @Test
    void should_detecter_existant_pour_parcelle_et_date() {
        UUID parcelleId = insererParcelleDeReference();
        repository.enregistrer(unReleve(parcelleId, LocalDate.of(2026, 7, 20), 1700));

        assertThat(repository.existePourParcelleEtDate(parcelleId, LocalDate.of(2026, 7, 20))).isTrue();
        assertThat(repository.existePourParcelleEtDate(parcelleId, LocalDate.of(2026, 7, 21))).isFalse();
        assertThat(repository.existePourParcelleEtDate(UUID.randomUUID(), LocalDate.of(2026, 7, 20))).isFalse();
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private UUID insererParcelleDeReference() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO parcelle (id, code, localite, superficie, plants_initiaux, "
                        + "date_plantation, statut) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, "PRC-2026-" + id.toString().substring(0, 3),
                "Bingerville", new BigDecimal("12.50"), 2000,
                LocalDate.of(2026, 6, 15), "EN_SUIVI");
        return id;
    }

    private Releve unReleve(UUID parcelleId, LocalDate date, int plantsVivants) {
        return Releve.reconstituer(
                new ci.ecotrack.releves.domaine.ReleveId(UUID.randomUUID()),
                parcelleId,
                new DateObservation(date),
                new NombrePlantsVivants(plantsVivants),
                TauxDeSurvie.calculer(plantsVivants, 2000));
    }
}
