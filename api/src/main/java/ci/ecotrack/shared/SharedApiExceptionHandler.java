package ci.ecotrack.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class SharedApiExceptionHandler {

    @ExceptionHandler(DonneeInvalideException.class)
    ProblemDetail traiterDonneeInvalide(DonneeInvalideException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Requete invalide");
        return pd;
    }
}
