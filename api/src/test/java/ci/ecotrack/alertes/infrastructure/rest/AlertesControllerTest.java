package ci.ecotrack.alertes.infrastructure.rest;

import ci.ecotrack.alertes.AlertesService;
import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertesController.class)
class AlertesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlertesService alertesService;

    @Test
    void should_200_when_liste_paginee_par_defaut() throws Exception {
        EntreeJournal entree = uneEntree("PRC-2026-042",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                "0.5000",
                Instant.parse("2026-07-20T10:15:30Z"));
        when(alertesService.consulter(any(Pagination.class)))
                .thenReturn(new AlertesRepository.PageEntreesJournal(List.of(entree), 1));

        mvc.perform(get("/api/v1/alertes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].code").value("PRC-2026-042"))
                .andExpect(jsonPath("$.contenu[0].sens").value("PASSAGE_EN_ALERTE"))
                .andExpect(jsonPath("$.contenu[0].ancienStatut").value("EN_SUIVI"))
                .andExpect(jsonPath("$.contenu[0].nouveauStatut").value("EN_ALERTE"))
                .andExpect(jsonPath("$.contenu[0].tauxDeclencheur").value("50.0"))
                .andExpect(jsonPath("$.contenu[0].dateReleve").value("2026-07-20"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(50))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void should_400_when_size_hors_bornes() throws Exception {
        mvc.perform(get("/api/v1/alertes").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requete invalide"));
    }

    @Test
    void should_400_when_page_negative() throws Exception {
        mvc.perform(get("/api/v1/alertes").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void should_400_when_page_hors_borne_haute() throws Exception {
        mvc.perform(get("/api/v1/alertes").param("page", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_afficher_60_0_when_taux_est_5995_pourcent_declencheur_alerte() throws Exception {
        EntreeJournal entree = uneEntree("PRC-2026-999",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                "0.5995",
                Instant.parse("2026-07-20T10:15:30Z"));
        when(alertesService.consulter(any(Pagination.class)))
                .thenReturn(new AlertesRepository.PageEntreesJournal(List.of(entree), 1));

        mvc.perform(get("/api/v1/alertes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].nouveauStatut").value("EN_ALERTE"))
                .andExpect(jsonPath("$.contenu[0].tauxDeclencheur").value("60.0"));
    }

    @Test
    void should_ne_pas_exposer_de_champ_statut_bricolable_en_ecriture() throws Exception {
        when(alertesService.consulter(any(Pagination.class)))
                .thenReturn(new AlertesRepository.PageEntreesJournal(List.of(), 0));

        mvc.perform(get("/api/v1/alertes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    private EntreeJournal uneEntree(String code,
                                     StatutParcelle ancien,
                                     StatutParcelle nouveau,
                                     String taux,
                                     Instant survenuLe) {
        return EntreeJournal.reconstituer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                code,
                ancien, nouveau,
                new TauxDeSurvie(new BigDecimal(taux)),
                LocalDate.of(2026, 7, 20),
                survenuLe);
    }
}
