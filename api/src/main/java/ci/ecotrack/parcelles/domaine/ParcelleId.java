package ci.ecotrack.parcelles.domaine;

import java.util.UUID;

public record ParcelleId(UUID valeur) {

    public ParcelleId {
        if (valeur == null) {
            throw new DonneeParcelleInvalideException("L'identifiant de parcelle est requis");
        }
    }

    public static ParcelleId nouveau() {
        return new ParcelleId(UUID.randomUUID());
    }
}
