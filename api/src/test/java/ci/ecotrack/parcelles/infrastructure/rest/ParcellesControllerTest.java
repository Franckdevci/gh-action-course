package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.application.ParcelleReferenceIntrouvableException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.TauxDeSurvie;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void should_200_when_liste_paginee_par_defaut() throws Exception {
        Parcelle enAlerte = enAlerteAvecTaux("PRC-2026-050", "0.5000", LocalDate.of(2026, 7, 22));
        Parcelle enSuivi = uneParcelle("PRC-2026-100");
        when(parcellesService.consulter(any(Pagination.class)))
                .thenReturn(new ParcellesRepository.PageParcelles(List.of(enAlerte, enSuivi), 2));

        mvc.perform(get("/api/v1/parcelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].code").value("PRC-2026-050"))
                .andExpect(jsonPath("$.contenu[0].statut").value("EN_ALERTE"))
                .andExpect(jsonPath("$.contenu[0].dernierTaux").value("50.0"))
                .andExpect(jsonPath("$.contenu[1].code").value("PRC-2026-100"))
                .andExpect(jsonPath("$.contenu[1].statut").value("EN_SUIVI"))
                .andExpect(jsonPath("$.contenu[1].dernierTaux").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(50))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void should_200_avec_contenu_vide_when_parc_vide() throws Exception {
        when(parcellesService.consulter(any(Pagination.class)))
                .thenReturn(new ParcellesRepository.PageParcelles(List.of(), 0));

        mvc.perform(get("/api/v1/parcelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isArray())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void should_200_avec_contenu_vide_when_page_au_dela_de_la_derniere() throws Exception {
        when(parcellesService.consulter(any(Pagination.class)))
                .thenReturn(new ParcellesRepository.PageParcelles(List.of(), 120));

        mvc.perform(get("/api/v1/parcelles").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isArray())
                .andExpect(jsonPath("$.total").value(120));
    }

    @Test
    void should_400_when_size_hors_bornes_liste() throws Exception {
        mvc.perform(get("/api/v1/parcelles").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void should_400_when_page_negative_liste() throws Exception {
        mvc.perform(get("/api/v1/parcelles").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_200_when_fiche_parcelle_nominale() throws Exception {
        Parcelle parcelle = uneParcelle("PRC-2026-042");
        when(parcellesService.consulterFiche("PRC-2026-042")).thenReturn(parcelle);

        mvc.perform(get("/api/v1/parcelles/PRC-2026-042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRC-2026-042"))
                .andExpect(jsonPath("$.localite").value("Bingerville"))
                .andExpect(jsonPath("$.statut").value("EN_SUIVI"))
                .andExpect(jsonPath("$.dernierTaux").isEmpty())
                .andExpect(jsonPath("$.dateDernierReleve").isEmpty());
    }

    @Test
    void should_404_when_fiche_code_inconnu() throws Exception {
        doThrow(new ParcelleReferenceIntrouvableException(
                "Aucune parcelle avec le code PRC-2026-999"))
                .when(parcellesService).consulterFiche("PRC-2026-999");

        mvc.perform(get("/api/v1/parcelles/PRC-2026-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Non trouve"))
                .andExpect(jsonPath("$.detail").value("Aucune parcelle avec le code PRC-2026-999"));
    }

    @Test
    void should_ne_pas_refleter_input_when_code_contient_script() throws Exception {
        mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"<script>alert(1)</script>","localite":"Bingerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<script>"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("alert(1)"))));
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

    private Parcelle enAlerteAvecTaux(String code, String taux, LocalDate dateReleve) {
        Parcelle p = uneParcelle(code);
        p.enregistrerDernierReleve(new TauxDeSurvie(new BigDecimal(taux)), dateReleve);
        return p;
    }
}
