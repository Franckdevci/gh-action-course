package ci.ecotrack.alertes.domaine;

import java.util.UUID;

public record EntreeJournalId(UUID valeur) {

    public EntreeJournalId {
        if (valeur == null) {
            throw new BasculeInvalideException("L'identifiant de l'entree de journal est requis");
        }
    }

    public static EntreeJournalId nouveau() {
        return new EntreeJournalId(UUID.randomUUID());
    }
}
