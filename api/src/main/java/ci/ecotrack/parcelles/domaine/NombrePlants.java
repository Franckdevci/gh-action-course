package ci.ecotrack.parcelles.domaine;

public record NombrePlants(int valeur) {
    public NombrePlants {
        if (valeur <= 0) {
            throw new DonneeParcelleInvalideException(
                    "Le nombre de plants initial doit etre strictement positif");
        }
    }
}
