package ci.ecotrack.parcelles.domaine;

import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

public class Parcelle {

    private final ParcelleId id;
    private final CodeParcelle code;
    private final Localite localite;
    private final Superficie superficie;
    private final NombrePlants plantsInitiaux;
    private final LocalDate datePlantation;
    private StatutParcelle statut;
    private TauxDeSurvie dernierTaux;
    private LocalDate dateDernierReleve;

    private Parcelle(ParcelleId id,
                     CodeParcelle code,
                     Localite localite,
                     Superficie superficie,
                     NombrePlants plantsInitiaux,
                     LocalDate datePlantation,
                     StatutParcelle statut,
                     TauxDeSurvie dernierTaux,
                     LocalDate dateDernierReleve) {
        this.id = id;
        this.code = code;
        this.localite = localite;
        this.superficie = superficie;
        this.plantsInitiaux = plantsInitiaux;
        this.datePlantation = datePlantation;
        this.statut = statut;
        this.dernierTaux = dernierTaux;
        this.dateDernierReleve = dateDernierReleve;
    }

    public static Parcelle creer(CodeParcelle code,
                                 Localite localite,
                                 Superficie superficie,
                                 NombrePlants plantsInitiaux,
                                 LocalDate datePlantation,
                                 Clock horloge) {
        if (datePlantation == null) {
            throw new DonneeParcelleInvalideException("La date de plantation est requise");
        }
        if (datePlantation.isAfter(LocalDate.now(horloge))) {
            throw new DonneeParcelleInvalideException(
                    "La date de plantation ne peut pas etre dans le futur");
        }
        return new Parcelle(
                ParcelleId.nouveau(),
                code,
                localite,
                superficie,
                plantsInitiaux,
                datePlantation,
                StatutParcelle.EN_SUIVI,
                null,
                null);
    }

    public static Parcelle reconstituer(ParcelleId id,
                                        CodeParcelle code,
                                        Localite localite,
                                        Superficie superficie,
                                        NombrePlants plantsInitiaux,
                                        LocalDate datePlantation,
                                        StatutParcelle statut,
                                        TauxDeSurvie dernierTaux,
                                        LocalDate dateDernierReleve) {
        return new Parcelle(id, code, localite, superficie, plantsInitiaux,
                datePlantation, statut, dernierTaux, dateDernierReleve);
    }

    public Optional<StatutChange> enregistrerDernierReleve(TauxDeSurvie taux, LocalDate dateObservation) {
        if (taux == null) {
            throw new DonneeParcelleInvalideException("Le taux de survie est requis");
        }
        if (dateObservation == null) {
            throw new DonneeParcelleInvalideException("La date d'observation est requise");
        }
        if (this.dateDernierReleve != null && !dateObservation.isAfter(this.dateDernierReleve)) {
            return Optional.empty();
        }
        this.dernierTaux = taux;
        this.dateDernierReleve = dateObservation;
        StatutParcelle ancien = this.statut;
        this.statut = taux.estCritique() ? StatutParcelle.EN_ALERTE : StatutParcelle.EN_SUIVI;
        if (this.statut == ancien) {
            return Optional.empty();
        }
        return Optional.of(new StatutChange(ancien, this.statut, taux, dateObservation));
    }

    public ParcelleId id() { return id; }
    public CodeParcelle code() { return code; }
    public Localite localite() { return localite; }
    public Superficie superficie() { return superficie; }
    public NombrePlants plantsInitiaux() { return plantsInitiaux; }
    public LocalDate datePlantation() { return datePlantation; }
    public StatutParcelle statut() { return statut; }
    public TauxDeSurvie dernierTaux() { return dernierTaux; }
    public LocalDate dateDernierReleve() { return dateDernierReleve; }
}
