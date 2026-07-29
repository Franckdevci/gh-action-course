package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaParcelleRepository.class)
class JpaParcelleRepositoryTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Autowired
    private ParcellesRepository repository;

    @Test
    void should_persister_une_parcelle_et_retourner_le_meme_agregat() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");

        Parcelle persistee = repository.enregistrer(parcelle);

        assertThat(persistee.id()).isEqualTo(parcelle.id());
        assertThat(persistee.code().valeur()).isEqualTo("PRC-2026-042");
        assertThat(persistee.localite().valeur()).isEqualTo("Bingerville");
        assertThat(persistee.statut()).isEqualTo(StatutParcelle.EN_SUIVI);
    }

    @Test
    void should_lever_CodeParcelleDejaUtiliseException_when_code_deja_present() {
        repository.enregistrer(uneParcelle("PRC-2026-042"));

        assertThatThrownBy(() -> repository.enregistrer(uneParcelle("PRC-2026-042")))
                .isInstanceOf(CodeParcelleDejaUtiliseException.class);
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
