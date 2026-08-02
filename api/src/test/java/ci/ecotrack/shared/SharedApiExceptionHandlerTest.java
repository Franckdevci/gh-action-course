package ci.ecotrack.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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
                .andExpect(jsonPath("$.detail").value("Le parametre page doit etre compris entre 0 et 200"));
    }

    @Test
    void should_409_neutre_when_data_integrity_violation() throws Exception {
        mvc.perform(get("/__test/data-integrity-violation"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflit"))
                .andExpect(jsonPath("$.detail").value(
                        "Contrainte de base de donnees violee. Aucun detail n'est expose pour des raisons de securite."))
                .andExpect(content().string(not(containsString("parcelle_code_unique"))))
                .andExpect(content().string(not(containsString("PSQLException"))))
                .andExpect(content().string(not(containsString("SQLState"))));
    }

    @Test
    void should_500_neutre_when_exception_non_geree() throws Exception {
        mvc.perform(get("/__test/exception-non-geree"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Erreur interne"))
                .andExpect(jsonPath("$.detail").value(
                        "Erreur interne. Aucun detail n'est expose pour des raisons de securite."))
                .andExpect(content().string(not(containsString("NullPointerException"))))
                .andExpect(content().string(not(containsString("at ci.ecotrack"))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void should_400_neutre_when_page_non_numerique() throws Exception {
        mvc.perform(get("/__test/pagine").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requete invalide"))
                .andExpect(jsonPath("$.detail").value("Le parametre 'page' est invalide"))
                .andExpect(content().string(not(containsString("NumberFormatException"))))
                .andExpect(content().string(not(containsString("java.lang.Integer"))))
                .andExpect(content().string(not(containsString("abc"))));
    }

    @Test
    void should_400_neutre_when_size_deborde_int_max() throws Exception {
        mvc.perform(get("/__test/pagine").param("size", "99999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le parametre 'size' est invalide"))
                .andExpect(content().string(not(containsString("99999999999"))));
    }

    @Test
    void should_ne_pas_exposer_schema_when_violation_contrainte_avec_message_sql() throws Exception {
        mvc.perform(get("/__test/data-integrity-violation-avec-message-sql"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("PSQLException"))))
                .andExpect(content().string(not(containsString("ERROR: duplicate key value"))))
                .andExpect(content().string(not(containsString("parcelle_code_unique_INDEX"))));
    }

    @RestController
    static class StubController {

        @GetMapping("/__test/donnee-invalide")
        void declencherDonneeInvalide() {
            throw new DonneeInvalideException(
                    "Le parametre page doit etre compris entre 0 et 200");
        }

        @GetMapping("/__test/data-integrity-violation")
        void declencherDataIntegrityViolation() {
            throw new DataIntegrityViolationException(
                    "could not execute statement; SQL [n/a]; constraint [parcelle_code_unique]");
        }

        @GetMapping("/__test/data-integrity-violation-avec-message-sql")
        void declencherDataIntegrityViolationAvecMessageSql() {
            throw new DataIntegrityViolationException(
                    "ERROR: duplicate key value violates unique constraint \"parcelle_code_unique_INDEX\"");
        }

        @GetMapping("/__test/exception-non-geree")
        void declencherExceptionNonGeree() {
            throw new NullPointerException("Bug interne : referenceIntrouvable au chemin /users/admin/config");
        }

        @GetMapping("/__test/pagine")
        void pagine(@RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "50") int size) {
            // aucun corps : on cible la phase de conversion des @RequestParam par Spring
        }
    }
}
