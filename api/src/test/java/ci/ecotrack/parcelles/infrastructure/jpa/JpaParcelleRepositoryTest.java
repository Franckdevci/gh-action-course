package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
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

    @Test
    void should_retrouver_une_parcelle_par_code() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        repository.enregistrer(parcelle);

        var retrouvee = repository.trouverParCode(new CodeParcelle("PRC-2026-042"));

        assertThat(retrouvee).isPresent();
        assertThat(retrouvee.get().id()).isEqualTo(parcelle.id());
    }

    @Test
    void should_retourner_empty_when_code_inconnu() {
        var retrouvee = repository.trouverParCode(new CodeParcelle("PRC-2026-999"));

        assertThat(retrouvee).isEmpty();
    }

    @Test
    void should_retrouver_une_parcelle_par_id() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        repository.enregistrer(parcelle);

        var retrouvee = repository.trouverParId(parcelle.id().valeur());

        assertThat(retrouvee).isPresent();
        assertThat(retrouvee.get().code().valeur()).isEqualTo("PRC-2026-042");
    }

    @Test
    void should_retourner_empty_when_id_inconnu() {
        var retrouvee = repository.trouverParId(java.util.UUID.randomUUID());

        assertThat(retrouvee).isEmpty();
    }

    @Test
    void should_sauvegarder_les_modifications_de_denormalisation() {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        repository.enregistrer(parcelle);
        parcelle.enregistrerDernierReleve(
                new ci.ecotrack.shared.TauxDeSurvie(new BigDecimal("0.85")),
                LocalDate.of(2026, 7, 20));

        repository.sauvegarder(parcelle);

        var relue = repository.trouverParId(parcelle.id().valeur()).orElseThrow();
        assertThat(relue.dernierTaux().valeur()).isEqualByComparingTo("0.85");
        assertThat(relue.dateDernierReleve()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void should_lister_en_alerte_avant_en_suivi_puis_par_code() {
        Parcelle p1 = uneParcelle("PRC-2026-100");
        Parcelle p2 = uneParcelle("PRC-2026-050");
        Parcelle p3 = uneParcelle("PRC-2026-200");
        repository.enregistrer(p1);
        repository.enregistrer(p2);
        repository.enregistrer(p3);
        // p2 passe EN_ALERTE
        p2.enregistrerDernierReleve(new TauxDeSurvie(new BigDecimal("0.5000")),
                LocalDate.of(2026, 7, 20));
        repository.sauvegarder(p2);

        var page = repository.listerAlertesPuisCode(new Pagination(0, 50));

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.contenu()).extracting(p -> p.code().valeur())
                .containsExactly("PRC-2026-050", "PRC-2026-100", "PRC-2026-200");
        assertThat(page.contenu().get(0).statut()).isEqualTo(StatutParcelle.EN_ALERTE);
    }

    @Test
    void should_retourner_page_vide_when_parc_vide() {
        var page = repository.listerAlertesPuisCode(new Pagination(0, 50));

        assertThat(page.total()).isEqualTo(0);
        assertThat(page.contenu()).isEmpty();
    }

    @Test
    void should_retourner_contenu_vide_when_page_au_dela_de_la_derniere() {
        repository.enregistrer(uneParcelle("PRC-2026-001"));
        repository.enregistrer(uneParcelle("PRC-2026-002"));

        var page = repository.listerAlertesPuisCode(new Pagination(5, 50));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.contenu()).isEmpty();
    }

    @Test
    void should_paginer_when_plus_de_parcelles_que_size() {
        for (int i = 1; i <= 5; i++) {
            repository.enregistrer(uneParcelle(String.format("PRC-2026-%03d", i)));
        }

        var page0 = repository.listerAlertesPuisCode(new Pagination(0, 2));
        var page1 = repository.listerAlertesPuisCode(new Pagination(1, 2));
        var page2 = repository.listerAlertesPuisCode(new Pagination(2, 2));

        assertThat(page0.total()).isEqualTo(5);
        assertThat(page0.contenu()).hasSize(2);
        assertThat(page1.contenu()).hasSize(2);
        assertThat(page2.contenu()).hasSize(1);
    }

    @Test
    void should_conserver_dernierTaux_null_when_parcelle_sans_releve() {
        repository.enregistrer(uneParcelle("PRC-2026-500"));

        var page = repository.listerAlertesPuisCode(new Pagination(0, 50));

        assertThat(page.contenu()).hasSize(1);
        assertThat(page.contenu().get(0).dernierTaux()).isNull();
        assertThat(page.contenu().get(0).dateDernierReleve()).isNull();
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
