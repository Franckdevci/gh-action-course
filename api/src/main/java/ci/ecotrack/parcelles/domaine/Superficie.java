package ci.ecotrack.parcelles.domaine;

import java.math.BigDecimal;

public record Superficie(BigDecimal valeur) {

    private static final BigDecimal MIN = new BigDecimal("0.01");
    private static final BigDecimal MAX = new BigDecimal("10000.00");
    private static final int ECHELLE_MAX = 2;

    public Superficie {
        if (valeur == null) {
            throw new DonneeParcelleInvalideException("La superficie est requise");
        }
        if (valeur.scale() > ECHELLE_MAX) {
            throw new DonneeParcelleInvalideException(
                    "La superficie ne peut pas avoir plus de 2 decimales");
        }
        if (valeur.compareTo(MIN) < 0 || valeur.compareTo(MAX) > 0) {
            throw new DonneeParcelleInvalideException(
                    "La superficie doit etre comprise entre 0.01 et 10000 ha");
        }
    }
}
