package ci.ecotrack.releves.domaine;

public record NombrePlantsVivants(int valeur) {

    public NombrePlantsVivants {
        if (valeur < 0) {
            throw new DonneeReleveInvalideException(
                    "Le nombre de plants vivants doit etre positif ou nul");
        }
    }

    public void validerContrePlantsInitiaux(int plantsInitiaux) {
        if (valeur > plantsInitiaux) {
            throw new DonneeReleveInvalideException(
                    "Le nombre de plants vivants (" + valeur
                            + ") ne peut pas depasser le nombre de plants initiaux ("
                            + plantsInitiaux + ")");
        }
    }
}
