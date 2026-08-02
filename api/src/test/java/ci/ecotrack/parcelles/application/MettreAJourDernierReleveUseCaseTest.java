package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

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

class MettreAJourDernierReleveUseCaseTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private final RepositoryEnMemoire repository = new RepositoryEnMemoire();
    private final MettreAJourDernierReleveUseCase useCase =
            new MettreAJourDernierReleveUseCase(repository);

    @Test
    void should_denormaliser_le_dernier_taux_when_parcelle_existe() {
        Parcelle parcelle = Parcelle.creer(
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                HORLOGE);
        repository.enregistrer(parcelle);
        TauxDeSurvie taux = new TauxDeSurvie(new BigDecimal("0.85"));

        useCase.executer(parcelle.id().valeur(), taux, LocalDate.of(2026, 7, 20));

        Parcelle relue = repository.trouverParId(parcelle.id().valeur()).orElseThrow();
        assertThat(relue.dernierTaux()).isEqualTo(taux);
        assertThat(relue.dateDernierReleve()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void should_remonter_le_StatutChange_when_releve_declenche_alerte() {
        Parcelle parcelle = Parcelle.creer(
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                HORLOGE);
        repository.enregistrer(parcelle);

        Optional<StatutChange> change = useCase.executer(
                parcelle.id().valeur(),
                new TauxDeSurvie(new BigDecimal("0.55")),
                LocalDate.of(2026, 7, 20));

        assertThat(change).isPresent();
        assertThat(change.get().nouveauStatut()).isEqualTo(StatutParcelle.EN_ALERTE);
    }

    @Test
    void should_lever_ParcelleReferenceIntrouvableException_when_id_inconnu() {
        UUID idInconnu = UUID.randomUUID();
        TauxDeSurvie taux = new TauxDeSurvie(new BigDecimal("0.85"));

        assertThatThrownBy(() -> useCase.executer(idInconnu, taux, LocalDate.of(2026, 7, 20)))
                .isInstanceOf(ParcelleReferenceIntrouvableException.class);
    }

    private static class RepositoryEnMemoire implements ParcellesRepository {
        private final List<Parcelle> parcelles = new ArrayList<>();

        @Override
        public Parcelle enregistrer(Parcelle parcelle) {
            parcelles.add(parcelle);
            return parcelle;
        }

        @Override
        public Optional<Parcelle> trouverParCode(CodeParcelle code) {
            return parcelles.stream()
                    .filter(p -> p.code().valeur().equals(code.valeur()))
                    .findFirst();
        }

        @Override
        public Optional<Parcelle> trouverParId(UUID id) {
            return parcelles.stream()
                    .filter(p -> p.id().valeur().equals(id))
                    .findFirst();
        }

        @Override
        public void sauvegarder(Parcelle parcelle) {
            parcelles.removeIf(p -> p.id().valeur().equals(parcelle.id().valeur()));
            parcelles.add(parcelle);
        }
    }
}
