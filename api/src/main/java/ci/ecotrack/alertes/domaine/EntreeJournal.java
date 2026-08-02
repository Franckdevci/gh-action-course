package ci.ecotrack.alertes.domaine;

import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

public final class EntreeJournal {

    // SEC-FAIB-03 : defense en profondeur, on ne fait pas confiance a l'emetteur amont.
    // Aligne sur ci.ecotrack.parcelles.domaine.CodeParcelle.FORMAT (dupliquee volontairement :
    // on ne veut pas croiser les modules pour un simple regex de garde-fou).
    private static final Pattern FORMAT_CODE = Pattern.compile("^PRC-\\d{4}-\\d{1,3}$");

    private final EntreeJournalId id;
    private final UUID parcelleId;
    private final String code;
    private final SensDeBascule sens;
    private final StatutParcelle ancienStatut;
    private final StatutParcelle nouveauStatut;
    private final TauxDeSurvie tauxDeclencheur;
    private final LocalDate dateReleve;
    private final Instant survenuLe;

    private EntreeJournal(EntreeJournalId id,
                          UUID parcelleId,
                          String code,
                          SensDeBascule sens,
                          StatutParcelle ancienStatut,
                          StatutParcelle nouveauStatut,
                          TauxDeSurvie tauxDeclencheur,
                          LocalDate dateReleve,
                          Instant survenuLe) {
        this.id = id;
        this.parcelleId = parcelleId;
        this.code = code;
        this.sens = sens;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.tauxDeclencheur = tauxDeclencheur;
        this.dateReleve = dateReleve;
        this.survenuLe = survenuLe;
    }

    public static EntreeJournal consigner(UUID parcelleId,
                                          String code,
                                          StatutParcelle ancienStatut,
                                          StatutParcelle nouveauStatut,
                                          TauxDeSurvie tauxDeclencheur,
                                          LocalDate dateReleve,
                                          Instant survenuLe) {
        if (parcelleId == null) {
            throw new BasculeInvalideException("L'identifiant de la parcelle est requis");
        }
        if (code == null || code.isBlank()) {
            throw new BasculeInvalideException("Le code de la parcelle est requis");
        }
        if (!FORMAT_CODE.matcher(code).matches()) {
            throw new BasculeInvalideException("Le code doit respecter le format PRC-AAAA-NNN");
        }
        if (ancienStatut == null || nouveauStatut == null) {
            throw new BasculeInvalideException("Les statuts ancien et nouveau sont requis");
        }
        if (tauxDeclencheur == null) {
            throw new BasculeInvalideException("Le taux declencheur est requis");
        }
        if (dateReleve == null) {
            throw new BasculeInvalideException("La date du releve declencheur est requise");
        }
        if (survenuLe == null) {
            throw new BasculeInvalideException("L'horodatage de survenue est requis");
        }
        SensDeBascule sens = SensDeBascule.deduire(ancienStatut, nouveauStatut);
        return new EntreeJournal(
                EntreeJournalId.nouveau(),
                parcelleId, code, sens,
                ancienStatut, nouveauStatut,
                tauxDeclencheur, dateReleve, survenuLe);
    }

    public static EntreeJournal reconstituer(UUID id,
                                             UUID parcelleId,
                                             String code,
                                             StatutParcelle ancienStatut,
                                             StatutParcelle nouveauStatut,
                                             TauxDeSurvie tauxDeclencheur,
                                             LocalDate dateReleve,
                                             Instant survenuLe) {
        SensDeBascule sens = ancienStatut == nouveauStatut
                ? null
                : (nouveauStatut == StatutParcelle.EN_ALERTE
                        ? SensDeBascule.PASSAGE_EN_ALERTE
                        : SensDeBascule.RETABLISSEMENT);
        return new EntreeJournal(
                new EntreeJournalId(id),
                parcelleId, code, sens,
                ancienStatut, nouveauStatut,
                tauxDeclencheur, dateReleve, survenuLe);
    }

    public EntreeJournalId id() { return id; }
    public UUID parcelleId() { return parcelleId; }
    public String code() { return code; }
    public SensDeBascule sens() { return sens; }
    public StatutParcelle ancienStatut() { return ancienStatut; }
    public StatutParcelle nouveauStatut() { return nouveauStatut; }
    public TauxDeSurvie tauxDeclencheur() { return tauxDeclencheur; }
    public LocalDate dateReleve() { return dateReleve; }
    public Instant survenuLe() { return survenuLe; }
}
