package ci.ecotrack.parcelles;

import ci.ecotrack.parcelles.application.ConsulterFicheParcelleUseCase;
import ci.ecotrack.parcelles.application.ConsulterParcellesUseCase;
import ci.ecotrack.parcelles.application.CreerParcelleUseCase;
import ci.ecotrack.parcelles.application.MettreAJourDernierReleveUseCase;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParcellesServiceTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private final ParcellesRepository repository = mock(ParcellesRepository.class);
    private final CreerParcelleUseCase creerUseCase = mock(CreerParcelleUseCase.class);
    private final MettreAJourDernierReleveUseCase majUseCase =
            new MettreAJourDernierReleveUseCase(repository);
    private final ConsulterParcellesUseCase consulterUseCase =
            new ConsulterParcellesUseCase(repository);
    private final ConsulterFicheParcelleUseCase consulterFicheUseCase =
            new ConsulterFicheParcelleUseCase(repository);
    private final ParcellesService service =
            new ParcellesService(creerUseCase, majUseCase, consulterUseCase, consulterFicheUseCase, repository);

    @Test
    void should_retourner_ParcelleReference_when_code_existe() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        when(repository.trouverParCode(new CodeParcelle("PRC-2026-042")))
                .thenReturn(Optional.of(parcelle));

        Optional<ParcelleReference> ref = service.trouverParCode("PRC-2026-042");

        assertThat(ref).isPresent();
        assertThat(ref.get().id()).isEqualTo(parcelle.id().valeur());
        assertThat(ref.get().plantsInitiaux()).isEqualTo(2000);
        assertThat(ref.get().datePlantation()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void should_retourner_empty_when_code_inconnu() {
        when(repository.trouverParCode(any(CodeParcelle.class))).thenReturn(Optional.empty());

        Optional<ParcelleReference> ref = service.trouverParCode("PRC-2026-999");

        assertThat(ref).isEmpty();
    }

    @Test
    void should_deleguer_mettre_a_jour_dernier_releve_au_use_case() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        when(repository.trouverParId(parcelle.id().valeur())).thenReturn(Optional.of(parcelle));
        TauxDeSurvie taux = new TauxDeSurvie(new BigDecimal("0.85"));

        service.mettreAJourDernierReleve(parcelle.id().valeur(), taux, LocalDate.of(2026, 7, 20));

        verify(repository, times(1)).sauvegarder(parcelle);
        assertThat(parcelle.dernierTaux()).isEqualTo(taux);
    }

    @Test
    void should_remonter_le_StatutChange_when_releve_declenche_alerte() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        when(repository.trouverParId(parcelle.id().valeur())).thenReturn(Optional.of(parcelle));

        Optional<StatutChange> change = service.mettreAJourDernierReleve(
                parcelle.id().valeur(),
                new TauxDeSurvie(new BigDecimal("0.55")),
                LocalDate.of(2026, 7, 20));

        assertThat(change).isPresent();
        assertThat(change.get().nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
    }

    private Parcelle uneParcelle(String code) {
        return Parcelle.creer(
                new CodeParcelle(code),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                HORLOGE);
    }
}
