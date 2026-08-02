package ci.ecotrack.releves.domaine;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegleStatutAlerteTest {

    @Test
    void should_rester_en_suivi_when_aucun_releve() {
        StatutParcelle statut = RegleStatutAlerte.evaluer(List.of());

        assertThat(statut).isEqualTo(StatutParcelle.EN_SUIVI);
    }

    @Test
    void should_passer_en_alerte_when_taux_est_5995_pourcent() {
        // 1199 / 2000 = 0.5995 (BigDecimal exact) < 0.60 -> EN_ALERTE
        Releve releve = releveAvec(LocalDate.of(2026, 7, 20), new BigDecimal("0.5995"));

        StatutParcelle statut = RegleStatutAlerte.evaluer(List.of(releve));

        assertThat(statut).isEqualTo(StatutParcelle.EN_ALERTE);
    }

    @Test
    void should_rester_en_suivi_when_taux_exactement_60_pourcent() {
        // 1200 / 2000 = 0.60 exact ; seuil est STRICTEMENT inferieur, donc EN_SUIVI
        Releve releve = releveAvec(LocalDate.of(2026, 7, 20), new BigDecimal("0.60"));

        StatutParcelle statut = RegleStatutAlerte.evaluer(List.of(releve));

        assertThat(statut).isEqualTo(StatutParcelle.EN_SUIVI);
    }

    @Test
    void should_ignorer_releve_antidate_when_determine_statut() {
        // Le plus recent PAR DATE : 2026-07-20, taux 80% -> EN_SUIVI
        // Un releve antidate 2026-06-20 avec taux 45% est en TETE de la liste
        // (ordre d'insertion) mais sa date est anterieure -- il ne doit PAS piloter le statut.
        // Le naif `get(0)` verrait antidate d'abord et repondrait EN_ALERTE : ce test le casse.
        Releve antidate = releveAvec(LocalDate.of(2026, 6, 20), new BigDecimal("0.4500"));
        Releve plusRecent = releveAvec(LocalDate.of(2026, 7, 20), new BigDecimal("0.8000"));

        StatutParcelle statut = RegleStatutAlerte.evaluer(List.of(antidate, plusRecent));

        assertThat(statut).isEqualTo(StatutParcelle.EN_SUIVI);
    }

    private static Releve releveAvec(LocalDate date, BigDecimal taux) {
        return Releve.reconstituer(
                ReleveId.nouveau(),
                UUID.randomUUID(),
                new DateObservation(date),
                new NombrePlantsVivants(0),
                new TauxDeSurvie(taux));
    }
}
