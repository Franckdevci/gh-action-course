package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.application.ParcelleIntrouvableException;
import ci.ecotrack.releves.application.ReleveDoublonException;
import ci.ecotrack.releves.domaine.DonneeReleveInvalideException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelevesApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new RelevesApiExceptionHandler())
                .build();
    }

    @Test
    void should_400_avec_champs_when_bean_validation_echoue() throws Exception {
        mvc.perform(post("/__test/valider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantsVivants\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.champs[?(@.champ=='plantsVivants')]").exists());
    }

    @Test
    void should_400_when_corps_illisible() throws Exception {
        mvc.perform(post("/__test/valider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Corps de requete illisible"));
    }

    @Test
    void should_400_when_donnee_releve_invalide() throws Exception {
        mvc.perform(get("/__test/donnee-invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Date en dehors des bornes"));
    }

    @Test
    void should_404_when_parcelle_introuvable() throws Exception {
        mvc.perform(get("/__test/introuvable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    void should_409_when_doublon_releve() throws Exception {
        mvc.perform(get("/__test/doublon"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit"));
    }

    @RestController
    static class StubController {

        @PostMapping("/__test/valider")
        void valider(@Valid @RequestBody StubBody body) {
        }

        @GetMapping("/__test/donnee-invalide")
        void donneeInvalide() {
            throw new DonneeReleveInvalideException("Date en dehors des bornes");
        }

        @GetMapping("/__test/introuvable")
        void introuvable() {
            throw new ParcelleIntrouvableException("Parcelle inconnue");
        }

        @GetMapping("/__test/doublon")
        void doublon() {
            throw new ReleveDoublonException("Doublon de date");
        }
    }

    record StubBody(@NotNull @Min(0) Integer plantsVivants) {
    }
}
