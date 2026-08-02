package ci.ecotrack.releves.domaine;

import ci.ecotrack.shared.StatutParcelle;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class RegleStatutAlerte {

    private RegleStatutAlerte() {
    }

    public static StatutParcelle evaluer(List<Releve> releves) {
        Objects.requireNonNull(releves, "La liste des releves est requise");
        return releves.stream()
                .max(Comparator.comparing(r -> r.dateObservation().valeur()))
                .map(r -> r.tauxDeSurvie().estCritique()
                        ? StatutParcelle.EN_ALERTE
                        : StatutParcelle.EN_SUIVI)
                .orElse(StatutParcelle.EN_SUIVI);
    }
}
