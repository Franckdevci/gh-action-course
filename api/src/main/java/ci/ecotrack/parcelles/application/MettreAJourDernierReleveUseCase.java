package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.shared.TauxDeSurvie;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class MettreAJourDernierReleveUseCase {

    private final ParcellesRepository repository;

    public MettreAJourDernierReleveUseCase(ParcellesRepository repository) {
        this.repository = repository;
    }

    public Optional<StatutChange> executer(UUID parcelleId, TauxDeSurvie taux, LocalDate dateObservation) {
        Parcelle parcelle = repository.trouverParId(parcelleId)
                .orElseThrow(() -> new ParcelleReferenceIntrouvableException(
                        "Parcelle introuvable pour l'id " + parcelleId));
        Optional<StatutChange> change = parcelle.enregistrerDernierReleve(taux, dateObservation);
        repository.sauvegarder(parcelle);
        return change;
    }
}
