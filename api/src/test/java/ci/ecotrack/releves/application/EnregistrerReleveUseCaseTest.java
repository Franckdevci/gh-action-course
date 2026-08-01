package ci.ecotrack.releves.application;

import ci.ecotrack.parcelles.ParcelleReference;
import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnregistrerReleveUseCaseTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private ParcellesService parcellesService;
    private RelevesRepositoryEnMemoire relevesRepository;
    private EnregistrerReleveUseCase useCase;

    @BeforeEach
    void setUp() {
        parcellesService = mock(ParcellesService.class);
        relevesRepository = new RelevesRepositoryEnMemoire();
        useCase = new EnregistrerReleveUseCase(relevesRepository, parcellesService, HORLOGE);
    }

    @Test
    void should_enregistrer_le_releve_et_declencher_la_denormalisation_when_commande_valide() {
        UUID parcelleId = UUID.randomUUID();
        when(parcellesService.trouverParCode("PRC-2026-042"))
                .thenReturn(Optional.of(new ParcelleReference(parcelleId, 2000, LocalDate.of(2026, 6, 15))));

        Releve resultat = useCase.executer(new EnregistrerReleveCommande(
                "PRC-2026-042", LocalDate.of(2026, 7, 20), 1700));

        assertThat(relevesRepository.contenu()).hasSize(1);
        assertThat(resultat.tauxDeSurvie().valeur()).isEqualByComparingTo("0.8500");
        verify(parcellesService, times(1)).mettreAJourDernierReleve(
                eq(parcelleId), any(TauxDeSurvie.class), eq(LocalDate.of(2026, 7, 20)));
    }

    @Test
    void should_lever_ParcelleIntrouvableException_when_code_inconnu() {
        when(parcellesService.trouverParCode("PRC-2026-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executer(new EnregistrerReleveCommande(
                "PRC-2026-999", LocalDate.of(2026, 7, 20), 1700)))
                .isInstanceOf(ParcelleIntrouvableException.class)
                .hasMessageContaining("PRC-2026-999");
    }

    @Test
    void should_lever_ReleveDoublonException_when_date_deja_utilisee() {
        UUID parcelleId = UUID.randomUUID();
        when(parcellesService.trouverParCode("PRC-2026-042"))
                .thenReturn(Optional.of(new ParcelleReference(parcelleId, 2000, LocalDate.of(2026, 6, 15))));
        relevesRepository.marquerDoublon(parcelleId, LocalDate.of(2026, 7, 20));

        assertThatThrownBy(() -> useCase.executer(new EnregistrerReleveCommande(
                "PRC-2026-042", LocalDate.of(2026, 7, 20), 1700)))
                .isInstanceOf(ReleveDoublonException.class)
                .hasMessageContaining("2026-07-20");
    }

    private static class RelevesRepositoryEnMemoire implements RelevesRepository {
        private final List<Releve> releves = new ArrayList<>();
        private UUID doublonParcelleId;
        private LocalDate doublonDate;

        @Override
        public Releve enregistrer(Releve releve) {
            releves.add(releve);
            return releve;
        }

        @Override
        public boolean existePourParcelleEtDate(UUID parcelleId, LocalDate date) {
            return parcelleId.equals(doublonParcelleId) && date.equals(doublonDate);
        }

        void marquerDoublon(UUID parcelleId, LocalDate date) {
            this.doublonParcelleId = parcelleId;
            this.doublonDate = date;
        }

        List<Releve> contenu() {
            return releves;
        }
    }
}
