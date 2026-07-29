package ci.ecotrack.parcelles.domaine;

import ci.ecotrack.shared.StatutParcelle;

import java.time.Clock;
import java.time.LocalDate;

public class Parcelle {

    private final ParcelleId id;
    private final CodeParcelle code;
    private final Localite localite;
    private final Superficie superficie;
    private final NombrePlants plantsInitiaux;
    private final LocalDate datePlantation;
    private StatutParcelle statut;

    private Parcelle(ParcelleId id,
                     CodeParcelle code,
                     Localite localite,
                     Superficie superficie,
                     NombrePlants plantsInitiaux,
                     LocalDate datePlantation,
                     StatutParcelle statut) {
        this.id = id;
        this.code = code;
        this.localite = localite;
        this.superficie = superficie;
        this.plantsInitiaux = plantsInitiaux;
        this.datePlantation = datePlantation;
        this.statut = statut;
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
                StatutParcelle.EN_SUIVI);
    }

    public ParcelleId id() { return id; }
    public CodeParcelle code() { return code; }
    public Localite localite() { return localite; }
    public Superficie superficie() { return superficie; }
    public NombrePlants plantsInitiaux() { return plantsInitiaux; }
    public LocalDate datePlantation() { return datePlantation; }
    public StatutParcelle statut() { return statut; }
}
