package ci.ecotrack.releves;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatutParcelleChangeTest {

    @Test
    void should_exposer_les_sept_champs_du_SDD() {
        UUID parcelleId = UUID.randomUUID();
        String code = "PRC-2026-042";
        TauxDeSurvie taux = new TauxDeSurvie(new BigDecimal("0.5995"));
        LocalDate dateReleve = LocalDate.of(2026, 7, 20);
        Instant survenuLe = Instant.parse("2026-07-20T10:15:30Z");

        StatutParcelleChange evt = new StatutParcelleChange(
                parcelleId, code,
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                taux, dateReleve, survenuLe);

        assertThat(evt.parcelleId()).isEqualTo(parcelleId);
        assertThat(evt.code()).isEqualTo(code);
        assertThat(evt.ancienStatut()).isEqualTo(StatutParcelle.EN_SUIVI);
        assertThat(evt.nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
        assertThat(evt.tauxDeclencheur()).isEqualTo(taux);
        assertThat(evt.dateReleve()).isEqualTo(dateReleve);
        assertThat(evt.survenuLe()).isEqualTo(survenuLe);
    }
}
