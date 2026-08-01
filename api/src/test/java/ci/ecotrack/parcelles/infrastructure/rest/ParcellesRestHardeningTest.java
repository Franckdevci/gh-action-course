package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParcellesController.class)
@Import(ApiExceptionHandler.class)
class ParcellesRestHardeningTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ParcellesService parcellesService;

    @Test
    void should_400_and_no_input_reflection_when_localite_contient_null_byte() throws Exception {
        MvcResult resultat = mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bing\\u0000erville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("NUL")))
                .andReturn();

        assertThat(resultat.getResponse().getContentAsString()).doesNotContain("Bing");
    }

    @Test
    void should_400_and_no_input_reflection_when_localite_contient_caractere_directionnel_rtl() throws Exception {
        MvcResult resultat = mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bing\\u202Eerville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("directionnel")))
                .andReturn();

        assertThat(resultat.getResponse().getContentAsString()).doesNotContain("Bing");
    }

    @Test
    void should_400_and_no_input_reflection_when_localite_contient_caractere_controle() throws Exception {
        MvcResult resultat = mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PRC-2026-042","localite":"Bing\\u0007erville",
                                 "superficie":12.50,"plantsInitiaux":2000,
                                 "datePlantation":"2026-06-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("controle")))
                .andReturn();

        assertThat(resultat.getResponse().getContentAsString()).doesNotContain("Bing");
    }

    @Test
    void should_400_and_no_reflection_when_champ_inconnu_gigantesque() throws Exception {
        String bourrage = "A".repeat(300_000);
        String corps = """
                {"code":"PRC-2026-042","localite":"Bingerville",
                 "superficie":12.50,"plantsInitiaux":2000,
                 "datePlantation":"2026-06-15",
                 "champInconnu":"%s"}
                """.formatted(bourrage);

        MvcResult resultat = mvc.perform(post("/api/v1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        String reponse = resultat.getResponse().getContentAsString();
        assertThat(reponse).doesNotContain(bourrage);
        assertThat(reponse).doesNotContain("champInconnu");
    }
}
