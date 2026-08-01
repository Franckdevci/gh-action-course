package ci.ecotrack.shared;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class SharedApiExceptionHandler {

    @ExceptionHandler(DonneeInvalideException.class)
    ProblemDetail traiterDonneeInvalide(DonneeInvalideException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Requete invalide");
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail traiterViolationContrainte(DataIntegrityViolationException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Contrainte de base de donnees violee. Aucun detail n'est expose pour des raisons de securite.");
        pd.setTitle("Conflit");
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail traiterRessourceIntrouvable(NoResourceFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Ressource introuvable");
        pd.setTitle("Non trouve");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail traiterErreurInterne(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur interne. Aucun detail n'est expose pour des raisons de securite.");
        pd.setTitle("Erreur interne");
        return pd;
    }
}
