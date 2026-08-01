package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.RelevesService;
import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.domaine.Releve;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/parcelles/{code}/releves")
@Tag(name = "Releves", description = "Relevés de survie associés à une parcelle (SRS EX-F-02). "
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
}
