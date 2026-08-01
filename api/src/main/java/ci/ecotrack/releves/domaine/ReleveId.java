package ci.ecotrack.releves.domaine;

import java.util.UUID;

public record ReleveId(UUID valeur) {

    public ReleveId {
        if (valeur == null) {
            throw new DonneeReleveInvalideException("L'identifiant du releve est requis");
        }
    }

    public static ReleveId nouveau() {
        return new ReleveId(UUID.randomUUID());
    }
}
