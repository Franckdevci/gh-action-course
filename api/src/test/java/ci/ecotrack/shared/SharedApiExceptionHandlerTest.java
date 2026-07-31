package ci.ecotrack.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SharedApiExceptionHandlerTest.StubController.class)
@Import({SharedApiExceptionHandler.class, SharedApiExceptionHandlerTest.StubController.class})
class SharedApiExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void should_400_rfc7807_when_donnee_invalide() throws Exception {
        mvc.perform(get("/__test/donnee-invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.detail").value("Le parametre page doit etre compris entre 0 et 10000"));
    }

    @RestController
    static class StubController {

        @GetMapping("/__test/donnee-invalide")
        void declencher() {
            throw new DonneeInvalideException(
                    "Le parametre page doit etre compris entre 0 et 10000");
        }
    }
}
