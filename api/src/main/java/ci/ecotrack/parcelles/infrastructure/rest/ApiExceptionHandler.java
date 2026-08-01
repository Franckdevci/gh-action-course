package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.domaine.DonneeParcelleInvalideException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail traiterValidation(MethodArgumentNotValidException e) {
        List<ChampErreur> champs = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ChampErreur(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Requete invalide");
        pd.setTitle("Requete invalide");
        pd.setProperty("champs", champs);
        return pd;
    }

    @ExceptionHandler(DonneeParcelleInvalideException.class)
    ProblemDetail traiterDomaineInvalide(DonneeParcelleInvalideException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Requete invalide");
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail traiterCorpsIllisible(HttpMessageNotReadableException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Corps de requete illisible");
        pd.setTitle("Requete invalide");
        return pd;
    }

    @ExceptionHandler(CodeParcelleDejaUtiliseException.class)
    ProblemDetail traiterConflit(CodeParcelleDejaUtiliseException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("Conflit");
        return pd;
    }

    record ChampErreur(String champ, String message) {
    }
}
