package ci.ecotrack.alertes.infrastructure.rest;

import ci.ecotrack.alertes.AlertesService;
import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.shared.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alertes")
@Tag(name = "Alertes",
        description = "Journal immuable des changements de statut d'alerte (SRS EX-F-07). "
                + "Tri antichronologique, pagination bornée (SEC-02). Alimente par l'écoute de "
                + "l'event StatutParcelleChange (ADR-003, EX-NF-03).")
class AlertesController {

    private final AlertesService alertesService;

    AlertesController(AlertesService alertesService) {
        this.alertesService = alertesService;
    }

    @GetMapping
    @Operation(
            summary = "Consulter le journal des alertes",
            description = """
                    Retourne une page du journal des changements de statut, triée du plus récent
                    au plus ancien. Les entrées sont immuables (SRS EX-F-07 R1).

                    Bornes de pagination : page ∈ [0, 200], size ∈ [1, 100]. Toute valeur hors
                    bornes retourne un 400 sans troncature silencieuse (SEC-02 / SEC-B-04).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page du journal",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PageAlertesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres de pagination hors bornes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    PageAlertesResponse consulter(
            @Parameter(description = "Index de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "50")
            @RequestParam(defaultValue = "50") int size) {
        Pagination pagination = new Pagination(page, size);
        AlertesRepository.PageEntreesJournal contenu = alertesService.consulter(pagination);
        return PageAlertesResponse.de(contenu, page, size);
    }
}
