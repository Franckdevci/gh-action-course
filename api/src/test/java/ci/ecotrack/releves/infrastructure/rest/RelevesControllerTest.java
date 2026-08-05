package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.RelevesService;
import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.application.ParcelleIntrouvableException;
import ci.ecotrack.releves.application.ReleveDoublonException;
import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.DonneeReleveInvalideException;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.releves.domaine.ReleveId;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelevesController.class)
@Import(RelevesApiExceptionHandler.class)
class RelevesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RelevesService relevesService;

    private static Releve unReleve(UUID id) {
        return Releve.reconstituer(
                new ReleveId(id),
                UUID.randomUUID(),
                new DateObservation(LocalDate.of(2026, 7, 20)),
                new NombrePlantsVivants(1700),
                new TauxDeSurvie(new BigDecimal("0.8500")));
    }

    @Test
    void should_201_when_creation_nominale() throws Exception {
        UUID releveId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(relevesService.enregistrer(any(EnregistrerReleveCommande.class)))
                .thenReturn(unReleve(releveId));

        mvc.perform(post("/api/v1/parcelles/PRC-2026-042/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateObservation":"2026-07-20","plantsVivants":1700}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/parcelles/PRC-2026-042/releves/" + releveId))
                .andExpect(jsonPath("$.id").value(releveId.toString()))
                .andExpect(jsonPath("$.dateObservation").value("2026-07-20"))
                .andExpect(jsonPath("$.plantsVivants").value(1700))
                .andExpect(jsonPath("$.tauxSurvie").value("85.0"))
                .andExpect(jsonPath("$.tauxSurvie").isString());
    }

    @Test
    void should_404_when_parcelle_inexistante() throws Exception {
        doThrow(new ParcelleIntrouvableException("Aucune parcelle avec le code PRC-2026-999"))
                .when(relevesService).enregistrer(any(EnregistrerReleveCommande.class));

        mvc.perform(post("/api/v1/parcelles/PRC-2026-999/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateObservation":"2026-07-20","plantsVivants":1700}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Aucune parcelle avec le code PRC-2026-999"));
    }

    @Test
    void should_409_when_doublon_date() throws Exception {
        doThrow(new ReleveDoublonException("Un releve existe deja pour cette parcelle a la date 2026-07-20"))
                .when(relevesService).enregistrer(any(EnregistrerReleveCommande.class));

        mvc.perform(post("/api/v1/parcelles/PRC-2026-042/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateObservation":"2026-07-20","plantsVivants":1700}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Un releve existe deja pour cette parcelle a la date 2026-07-20"));
    }

    @Test
    void should_400_when_plants_vivants_negatif() throws Exception {
        mvc.perform(post("/api/v1/parcelles/PRC-2026-042/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateObservation":"2026-07-20","plantsVivants":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.champs[?(@.champ=='plantsVivants')]").exists());
    }

    @Test
    void should_400_when_date_observation_dans_le_futur() throws Exception {
        doThrow(new DonneeReleveInvalideException(
                "La date d'observation ne peut pas etre dans le futur"))
                .when(relevesService).enregistrer(any(EnregistrerReleveCommande.class));

        mvc.perform(post("/api/v1/parcelles/PRC-2026-042/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateObservation":"2026-07-20","plantsVivants":1700}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "La date d'observation ne peut pas etre dans le futur"));
    }

    @Test
    void should_400_rfc7807_when_json_malforme() throws Exception {
        mvc.perform(post("/api/v1/parcelles/PRC-2026-042/releves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.detail").value("Corps de requete illisible"));
    }

    @Test
    void should_200_when_historique_paginee_par_defaut() throws Exception {
        Releve r1 = unReleveDate(LocalDate.of(2026, 7, 20), 1700);
        Releve r2 = unReleveDate(LocalDate.of(2026, 6, 15), 1800);
        when(relevesService.consulterHistorique(eq("PRC-2026-042"), any(Pagination.class)))
                .thenReturn(new RelevesRepository.PageReleves(List.of(r1, r2), 2));

        mvc.perform(get("/api/v1/parcelles/PRC-2026-042/releves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].dateObservation").value("2026-07-20"))
                .andExpect(jsonPath("$.contenu[1].dateObservation").value("2026-06-15"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(50))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void should_200_avec_contenu_vide_when_parcelle_sans_releve() throws Exception {
        when(relevesService.consulterHistorique(eq("PRC-2026-042"), any(Pagination.class)))
                .thenReturn(new RelevesRepository.PageReleves(List.of(), 0));

        mvc.perform(get("/api/v1/parcelles/PRC-2026-042/releves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void should_404_when_historique_parcelle_inconnue() throws Exception {
        doThrow(new ParcelleIntrouvableException("Aucune parcelle avec le code PRC-2026-999"))
                .when(relevesService).consulterHistorique(eq("PRC-2026-999"), any(Pagination.class));

        mvc.perform(get("/api/v1/parcelles/PRC-2026-999/releves"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    void should_400_when_historique_size_hors_bornes() throws Exception {
        mvc.perform(get("/api/v1/parcelles/PRC-2026-042/releves").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private static Releve unReleveDate(LocalDate date, int plants) {
        return Releve.reconstituer(
                new ReleveId(UUID.randomUUID()),
                UUID.randomUUID(),
                new DateObservation(date),
                new NombrePlantsVivants(plants),
                new TauxDeSurvie(new BigDecimal("0.8500")));
    }
}
