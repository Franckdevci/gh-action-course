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
    }
}
