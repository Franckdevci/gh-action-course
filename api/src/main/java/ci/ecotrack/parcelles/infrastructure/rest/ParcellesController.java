package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.Superficie;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/parcelles")
class ParcellesController {

    private final ParcellesService parcellesService;

    ParcellesController(ParcellesService parcellesService) {
        this.parcellesService = parcellesService;
    }

    @PostMapping
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
