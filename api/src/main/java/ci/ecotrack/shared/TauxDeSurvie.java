package ci.ecotrack.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TauxDeSurvie(BigDecimal valeur) {

    private static final int ECHELLE_MAX = 4;
    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.ONE;
    private static final BigDecimal SEUIL_CRITIQUE = new BigDecimal("0.60");

    public TauxDeSurvie {
        if (valeur == null) {
            throw new DonneeInvalideException("Le taux de survie est requis");
        }
        if (valeur.scale() > ECHELLE_MAX) {
            throw new DonneeInvalideException(
                    "Le taux de survie ne peut pas avoir plus de "
                            + ECHELLE_MAX + " decimales");
        }
        if (valeur.compareTo(MIN) < 0 || valeur.compareTo(MAX) > 0) {
            throw new DonneeInvalideException(
                    "Le taux de survie doit etre compris entre 0 et 1");
        }
    }

    public boolean estCritique() {
        return valeur.compareTo(SEUIL_CRITIQUE) < 0;
    }

    public static TauxDeSurvie calculer(int plantsVivants, int plantsInitiaux) {
        if (plantsInitiaux <= 0) {
            throw new DonneeInvalideException(
                    "Le nombre de plants initiaux doit etre strictement positif");
        }
        BigDecimal ratio = new BigDecimal(plantsVivants)
                .divide(new BigDecimal(plantsInitiaux), ECHELLE_MAX, RoundingMode.HALF_UP);
        return new TauxDeSurvie(ratio);
    }
}
