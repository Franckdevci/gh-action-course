package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreerParcelleUseCaseTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private final ParcellesRepositoryEnMemoire repository = new ParcellesRepositoryEnMemoire();
    private final CreerParcelleUseCase useCase = new CreerParcelleUseCase(repository, HORLOGE);

    @Test
    void should_persister_la_parcelle_construite_when_commande_valide() {
        CreerParcelleCommande commande = new CreerParcelleCommande(
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15));

        Parcelle rendue = useCase.executer(commande);

        assertThat(repository.contenu()).hasSize(1);
        Parcelle persistee = repository.contenu().get(0);
        assertThat(persistee.code().valeur()).isEqualTo("PRC-2026-042");
        assertThat(persistee.localite().valeur()).isEqualTo("Bingerville");
        assertThat(persistee.statut()).isEqualTo(StatutParcelle.EN_SUIVI);
        assertThat(rendue).isSameAs(persistee);
    }

    @Test
    void should_propager_exception_when_repository_leve_conflit_unicite() {
        ParcellesRepository repositoryEnConflit = parcelle -> {
            throw new CodeParcelleDejaUtiliseException("Une parcelle avec ce code existe deja");
        };
        CreerParcelleUseCase useCaseEnConflit = new CreerParcelleUseCase(repositoryEnConflit, HORLOGE);
        CreerParcelleCommande commande = new CreerParcelleCommande(
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15));

        assertThatThrownBy(() -> useCaseEnConflit.executer(commande))
                .isInstanceOf(CodeParcelleDejaUtiliseException.class)
                .hasMessageContaining("code existe deja");
    }

    private static class ParcellesRepositoryEnMemoire implements ParcellesRepository {
        private final List<Parcelle> parcelles = new ArrayList<>();

        @Override
        public Parcelle enregistrer(Parcelle parcelle) {
            parcelles.add(parcelle);
            return parcelle;
        }

        List<Parcelle> contenu() {
            return parcelles;
        }
    }
}
