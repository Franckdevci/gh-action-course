package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/parcelles")
@Tag(name = "Parcelles", description = "Referentiel des parcelles de reboisement (SRS EX-F-01)")
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
}
