package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.RelevesService;
import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.domaine.Releve;
import jakarta.validation.Valid;
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
class RelevesController {

    private final RelevesService relevesService;

    RelevesController(RelevesService relevesService) {
        this.relevesService = relevesService;
    }

    @PostMapping
    ResponseEntity<ReleveResponse> enregistrer(@PathVariable String code,
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
