package ci.ecotrack.releves.domaine;

import ci.ecotrack.shared.TauxDeSurvie;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class Releve {

    private final ReleveId id;
    private final UUID parcelleId;
    private final DateObservation dateObservation;
    private final NombrePlantsVivants plantsVivants;
    private final TauxDeSurvie tauxDeSurvie;

    private Releve(ReleveId id,
                   UUID parcelleId,
                   DateObservation dateObservation,
                   NombrePlantsVivants plantsVivants,
                   TauxDeSurvie tauxDeSurvie) {
        this.id = id;
        this.parcelleId = parcelleId;
        this.dateObservation = dateObservation;
        this.plantsVivants = plantsVivants;
        this.tauxDeSurvie = tauxDeSurvie;
    }

    public static Releve enregistrer(UUID parcelleId,
                                     DateObservation dateObservation,
                                     NombrePlantsVivants plantsVivants,
                                     int plantsInitiaux,
                                     LocalDate datePlantation,
                                     Clock horloge) {
        if (parcelleId == null) {
            throw new DonneeReleveInvalideException("La reference de parcelle est requise");
        }
        dateObservation.validerCoherenceAvec(datePlantation, horloge);
        plantsVivants.validerContrePlantsInitiaux(plantsInitiaux);
        TauxDeSurvie taux = TauxDeSurvie.calculer(plantsVivants.valeur(), plantsInitiaux);
        return new Releve(
                ReleveId.nouveau(),
                parcelleId,
                dateObservation,
                plantsVivants,
                taux);
    }

    public static Releve reconstituer(ReleveId id,
                                      UUID parcelleId,
                                      DateObservation dateObservation,
                                      NombrePlantsVivants plantsVivants,
                                      TauxDeSurvie tauxDeSurvie) {
        return new Releve(id, parcelleId, dateObservation, plantsVivants, tauxDeSurvie);
    }

    public ReleveId id() { return id; }
    public UUID parcelleId() { return parcelleId; }
    public DateObservation dateObservation() { return dateObservation; }
    public NombrePlantsVivants plantsVivants() { return plantsVivants; }
    public TauxDeSurvie tauxDeSurvie() { return tauxDeSurvie; }
}
