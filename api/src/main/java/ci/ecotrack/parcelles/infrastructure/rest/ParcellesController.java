package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
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
@RequestMapping("/api/v1/parcelles")
@Tag(name = "Parcelles",
        description = "Referentiel des parcelles de reboisement (SRS EX-F-01, EX-F-05, EX-F-06)")
class ParcellesController {

    private final ParcellesService parcellesService;

    ParcellesController(ParcellesService parcellesService) {
        this.parcellesService = parcellesService;
    }

    @PostMapping
    @Operation(
            summary = "Creer une parcelle",
            description = """
                    Cree une nouvelle parcelle avec son code unique, sa localite, sa superficie,
                    le nombre de plants initiaux et sa date de plantation. Le statut initial est
                    force a EN_SUIVI et le taux/statut ne sont jamais saisis (EX-NF-07).

                    Traca : SRS EX-F-01.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parcelle creee. En-tete Location vers la ressource.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParcelleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalide (validation Bean ou domaine)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Code de parcelle deja utilise",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ParcelleResponse> creer(@Valid @RequestBody CreerParcelleRequest requete) {
        CreerParcelleCommande commande = new CreerParcelleCommande(
                new CodeParcelle(requete.code()),
                new Localite(requete.localite()),
                new Superficie(requete.superficie()),
                new NombrePlants(requete.plantsInitiaux()),
                requete.datePlantation());
        Parcelle parcelle = parcellesService.creer(commande);
        URI location = UriComponentsBuilder.fromPath("/api/v1/parcelles/{code}")
                .buildAndExpand(parcelle.code().valeur())
                .toUri();
        return ResponseEntity.created(location).body(ParcelleResponse.de(parcelle));
    }

    @GetMapping
    @Operation(
            summary = "Lister les parcelles paginees",
            description = """
                    Retourne une page de parcelles triees selon la regle EX-F-05 R1 :
                    les parcelles EN_ALERTE sont retournees en premier, puis les autres
                    triees par code croissant.

                    Bornes de pagination : page in [0, 200], size in [1, 100]. Toute valeur
                    hors bornes retourne un 400 sans troncature silencieuse (SEC-02 / SEC-B-04).

                    Une page au-dela de la derniere retourne 200 avec `contenu: []` (EX-F-05
                    scenario "au-dela de la derniere page"). Une parcelle sans releve a
                    `dernierTaux: null` (EX-F-05 R4).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page des parcelles",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PageParcellesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination hors bornes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    PageParcellesResponse consulter(
            @Parameter(description = "Index de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "50")
            @RequestParam(defaultValue = "50") int size) {
        Pagination pagination = new Pagination(page, size);
        ParcellesRepository.PageParcelles contenu = parcellesService.consulter(pagination);
        return PageParcellesResponse.de(contenu, page, size);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Consulter la fiche d'une parcelle",
            description = """
                    Retourne les caracteristiques et le statut courant d'une parcelle.
                    L'historique des releves est expose separement via
                    GET /api/v1/parcelles/{code}/releves (SRS EX-F-06 R1).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la parcelle",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParcelleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aucune parcelle avec ce code",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ParcelleResponse consulterFiche(
            @Parameter(description = "Code de la parcelle (format PRC-AAAA-NNN)", example = "PRC-2026-042")
            @PathVariable String code) {
        Parcelle parcelle = parcellesService.consulterFiche(code);
        return ParcelleResponse.de(parcelle);
    }
}
