package ci.ecotrack.parcelles.domaine;

public record Localite(String valeur) {

    private static final int LONGUEUR_MAX = 100;

    public Localite {
        if (valeur == null) {
            throw new DonneeParcelleInvalideException("La localite est requise");
        }
        valeur = valeur.trim();
        if (valeur.isEmpty()) {
            throw new DonneeParcelleInvalideException("La localite ne peut pas etre vide");
        }
        if (valeur.length() > LONGUEUR_MAX) {
            throw new DonneeParcelleInvalideException(
                    "La localite ne peut pas depasser " + LONGUEUR_MAX + " caracteres");
        }
        for (int i = 0; i < valeur.length(); i++) {
            char c = valeur.charAt(i);
            if (c == '\0') {
                throw new DonneeParcelleInvalideException(
                        "La localite ne peut pas contenir de caractere NUL");
            }
            if ((c >= '\u202A' && c <= '\u202E') || (c >= '\u2066' && c <= '\u2069')) {
                throw new DonneeParcelleInvalideException(
                        "La localite ne peut pas contenir de caractere directionnel Unicode");
            }
            boolean isControleAutorise = c == '\t' || c == '\n' || c == '\r';
            if (Character.isISOControl(c) && !isControleAutorise) {
                throw new DonneeParcelleInvalideException(
                        "La localite ne peut pas contenir de caractere de controle");
            }
        }
    }
}
