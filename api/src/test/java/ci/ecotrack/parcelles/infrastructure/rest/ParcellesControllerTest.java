package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParcellesController.class)
@Import(ApiExceptionHandler.class)
class ParcellesControllerTest {

    private static final Clock HORLOGE = Clock.fixed(
            LocalDate.of(2026, 7, 29).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ParcellesService parcellesService;

    @Test
    void should_201_when_creation_nominale() throws Exception {
        Parcelle creee = Parcelle.creer(
                new CodeParcelle("PRC-2026-042"),
                new Localite("Bingerville"),
                new Superficie(new BigDecimal("12.50")),
                new NombrePlants(2000),
                LocalDate.of(2026, 6, 15),
                HORLOGE);
        when(parcellesService.creer(any(CreerParcelleCommande.class))).thenReturn(creee);

        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bingerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/parcelles/PRC-2026-042"))
                .andExpect(jsonPath("$.code").value("PRC-2026-042"))
                .andExpect(jsonPath("$.statut").value("EN_SUIVI"))
                .andExpect(jsonPath("$.dernierTaux").isEmpty())
                .andExpect(jsonPath("$.dateDernierReleve").isEmpty());
    }

    @Test
    void should_400_when_code_invalide() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PARCELLE-42","localite":"Bingerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.champs[?(@.champ=='code')]").exists());
    }

    @Test
    void should_409_when_code_deja_utilise() throws Exception {
        doThrow(new CodeParcelleDejaUtiliseException("Une parcelle avec ce code existe deja"))
                .when(parcellesService).creer(any(CreerParcelleCommande.class));

        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bingerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Une parcelle avec ce code existe deja"));
    }

    @Test
    void should_400_when_date_plantation_dans_le_futur() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bingerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs[?(@.champ=='datePlantation')]").exists());
    }

    @Test
    void should_400_when_superficie_hors_bornes() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bingerville",
                                 "superficie":0,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs[?(@.champ=='superficie')]").exists());
    }

    @Test
    void should_400_rfc7807_when_json_malforme() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.detail").value("Corps de requete illisible"));
    }

    @Test
    void should_400_when_localite_vide() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs[?(@.champ=='localite')]").exists());
    }
}
