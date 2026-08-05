package ci.ecotrack.releves.application;

import ci.ecotrack.parcelles.ParcelleReference;
import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.releves.StatutParcelleChange;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnregistrerReleveUseCaseTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private ParcellesService parcellesService;
    private RelevesRepositoryEnMemoire relevesRepository;
    private ApplicationEventPublisher eventPublisher;
    private EnregistrerReleveUseCase useCase;

    @BeforeEach
    void setUp() {
        parcellesService = mock(ParcellesService.class);
        relevesRepository = new RelevesRepositoryEnMemoire();
        eventPublisher = mock(ApplicationEventPublisher.class);
        useCase = new EnregistrerReleveUseCase(relevesRepository, parcellesService, eventPublisher, HORLOGE);
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
    void should_publier_StatutParcelleChange_when_releve_declenche_alerte() {
        UUID parcelleId = UUID.randomUUID();
        TauxDeSurvie tauxDeclencheur = new TauxDeSurvie(new BigDecimal("0.5500"));
        LocalDate dateReleve = LocalDate.of(2026, 7, 20);
        when(parcellesService.trouverParCode("PRC-2026-042"))
                .thenReturn(Optional.of(new ParcelleReference(parcelleId, 2000, LocalDate.of(2026, 6, 15))));
        when(parcellesService.mettreAJourDernierReleve(eq(parcelleId), any(TauxDeSurvie.class), eq(dateReleve)))
                .thenReturn(Optional.of(new StatutChange(
                        StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                        tauxDeclencheur, dateReleve)));

        useCase.executer(new EnregistrerReleveCommande("PRC-2026-042", dateReleve, 1100));

        ArgumentCaptor<StatutParcelleChange> captor = ArgumentCaptor.forClass(StatutParcelleChange.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        StatutParcelleChange evt = captor.getValue();
        assertThat(evt.parcelleId()).isEqualTo(parcelleId);
        assertThat(evt.code()).isEqualTo("PRC-2026-042");
        assertThat(evt.ancienStatut()).isEqualTo(StatutParcelle.EN_SUIVI);
        assertThat(evt.nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
        assertThat(evt.tauxDeclencheur()).isEqualTo(tauxDeclencheur);
        assertThat(evt.dateReleve()).isEqualTo(dateReleve);
        assertThat(evt.survenuLe()).isNotNull();
    }

    @Test
    void should_ne_pas_publier_event_when_statut_ne_change_pas() {
        UUID parcelleId = UUID.randomUUID();
        when(parcellesService.trouverParCode("PRC-2026-042"))
                .thenReturn(Optional.of(new ParcelleReference(parcelleId, 2000, LocalDate.of(2026, 6, 15))));
        when(parcellesService.mettreAJourDernierReleve(eq(parcelleId), any(TauxDeSurvie.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        useCase.executer(new EnregistrerReleveCommande("PRC-2026-042", LocalDate.of(2026, 7, 20), 1700));

        verify(eventPublisher, never()).publishEvent(any(StatutParcelleChange.class));
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

        @Override
        public PageReleves listerParParcelleAntichronologique(UUID parcelleId, ci.ecotrack.shared.Pagination pagination) {
            throw new UnsupportedOperationException();
        }
    }
}
