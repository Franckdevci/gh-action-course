package ci.ecotrack.alertes.domaine;

import ci.ecotrack.shared.StatutParcelle;

public enum SensDeBascule {

    PASSAGE_EN_ALERTE,
    RETABLISSEMENT;

    public static SensDeBascule deduire(StatutParcelle ancien, StatutParcelle nouveau) {
        if (ancien == nouveau) {
            throw new BasculeInvalideException(
                    "Aucune bascule : l'ancien et le nouveau statut sont identiques (" + ancien + ")");
        }
        if (nouveau == StatutParcelle.EN_ALERTE) {
            return PASSAGE_EN_ALERTE;
        }
        return RETABLISSEMENT;
    }
}
