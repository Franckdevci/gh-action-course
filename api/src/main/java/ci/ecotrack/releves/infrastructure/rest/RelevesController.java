package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.RelevesService;
import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/parcelles/{code}/releves")
@Tag(name = "Releves", description = "Relevés de survie associés à une parcelle (SRS EX-F-02, EX-F-06). "
        + "La denormalisation du dernier taux sur la parcelle est mise a jour dans la meme transaction (ADR-005).")
class RelevesController {

    private final RelevesService relevesService;

    RelevesController(RelevesService relevesService) {
        this.relevesService = relevesService;
    }

    @PostMapping
    @Operation(
            summary = "Enregistrer un releve",
            description = """
                    Enregistre un releve de survie pour la parcelle identifiee par son code.
                    Le taux est calcule par le systeme (plantsVivants / plantsInitiaux) : ni le taux
                    ni le statut ne sont acceptes en entree (EX-NF-07).

                    Contraintes cles : unicite (parcelle, dateObservation) verrouillee en base,
                    plantsVivants borne par plantsInitiaux de la parcelle. Traca : SRS EX-F-02.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Releve enregistre. En-tete Location vers la ressource.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReleveResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalide (validation Bean ou regle domaine)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Parcelle introuvable pour ce code",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Releve deja enregistre pour cette dateObservation",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ReleveResponse> enregistrer(
            @Parameter(description = "Code de la parcelle (format PRC-AAAA-NNN)", example = "PRC-2026-001")
            @PathVariable String code,
            @Valid @RequestBody EnregistrerReleveRequest requete) {
        EnregistrerReleveCommande commande = new EnregistrerReleveCommande(
                code,
                requete.dateObservation(),
                requete.plantsVivants());
        Releve releve = relevesService.enregistrer(commande);
        URI location = UriComponentsBuilder.fromPath("/api/v1/parcelles/{code}/releves/{id}")
                .buildAndExpand(code, releve.id().valeur())
                .toUri();
        return ResponseEntity.created(location).body(ReleveResponse.de(releve));
    }

    @GetMapping
    @Operation(
            summary = "Consulter l'historique des releves d'une parcelle",
            description = """
                    Retourne la liste paginee des releves d'une parcelle, tries du plus recent
                    au plus ancien par dateObservation (SRS EX-F-06 R1). Un releve antidate peut
                    donc apparaitre entre deux releves posterieurs mais enregistres avant lui.

                    Bornes de pagination : page in [0, 200], size in [1, 100].
                    Une parcelle sans releve retourne 200 avec `contenu: []` (EX-F-06 scenario
                    "parcelle sans releve"). Un code inconnu retourne 404 RFC 7807.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de l'historique",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PageRelevesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination hors bornes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Aucune parcelle avec ce code",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    PageRelevesResponse consulterHistorique(
            @Parameter(description = "Code de la parcelle (format PRC-AAAA-NNN)", example = "PRC-2026-042")
            @PathVariable String code,
            @Parameter(description = "Index de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "50")
            @RequestParam(defaultValue = "50") int size) {
        Pagination pagination = new Pagination(page, size);
        RelevesRepository.PageReleves contenu = relevesService.consulterHistorique(code, pagination);
        return PageRelevesResponse.de(contenu, page, size);
    }
}
