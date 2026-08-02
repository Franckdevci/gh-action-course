package ci.ecotrack.releves.application;

import ci.ecotrack.parcelles.ParcelleReference;
import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.releves.StatutParcelleChange;
import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class EnregistrerReleveUseCase {

    private final RelevesRepository relevesRepository;
    private final ParcellesService parcellesService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock horloge;

    public EnregistrerReleveUseCase(RelevesRepository relevesRepository,
                                    ParcellesService parcellesService,
                                    ApplicationEventPublisher eventPublisher,
                                    Clock horloge) {
        this.relevesRepository = relevesRepository;
        this.parcellesService = parcellesService;
        this.eventPublisher = eventPublisher;
        this.horloge = horloge;
    }

    public Releve executer(EnregistrerReleveCommande commande) {
        ParcelleReference reference = parcellesService.trouverParCode(commande.codeParcelle())
                .orElseThrow(() -> new ParcelleIntrouvableException(
                        "Aucune parcelle avec le code " + commande.codeParcelle()));

        if (relevesRepository.existePourParcelleEtDate(reference.id(), commande.dateObservation())) {
            throw new ReleveDoublonException(
                    "Un releve existe deja pour cette parcelle a la date " + commande.dateObservation());
        }

        DateObservation dateObservation = new DateObservation(commande.dateObservation());
        NombrePlantsVivants plantsVivants = new NombrePlantsVivants(commande.plantsVivants());

        Releve releve = Releve.enregistrer(
                reference.id(),
                dateObservation,
                plantsVivants,
                reference.plantsInitiaux(),
                reference.datePlantation(),
                horloge);

        Releve enregistre = relevesRepository.enregistrer(releve);

        Optional<StatutChange> change = parcellesService.mettreAJourDernierReleve(
                reference.id(),
                enregistre.tauxDeSurvie(),
                enregistre.dateObservation().valeur());

        change.ifPresent(sc -> eventPublisher.publishEvent(new StatutParcelleChange(
                reference.id(),
                commande.codeParcelle(),
                sc.ancienStatut(),
                sc.nouveauStatut(),
                sc.tauxDeclencheur(),
                sc.dateDeclencheur(),
                Instant.now(horloge))));

        return enregistre;
    }
}
