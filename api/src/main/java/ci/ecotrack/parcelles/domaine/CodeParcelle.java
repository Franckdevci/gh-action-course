package ci.ecotrack.parcelles.domaine;

import java.util.regex.Pattern;

public record CodeParcelle(String valeur) {

    private static final Pattern FORMAT = Pattern.compile("^PRC-\\d{4}-\\d{1,3}$");

    public CodeParcelle {
        if (valeur == null || !FORMAT.matcher(valeur).matches()) {
            throw new DonneeParcelleInvalideException(
                    "Le code doit respecter le format PRC-AAAA-NNN");
        }
    }
}
