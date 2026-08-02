package ci.ecotrack.alertes.infrastructure.jpa;

import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

import java.math.RoundingMode;

final class AlerteMapper {

    private AlerteMapper() {
    }

    static AlerteJpaEntity versEntite(EntreeJournal entree) {
        return new AlerteJpaEntity(
                entree.id().valeur(),
                entree.parcelleId(),
                entree.code(),
                entree.sens().name(),
                entree.ancienStatut().name(),
                entree.nouveauStatut().name(),
                entree.tauxDeclencheur().valeur(),
                entree.dateReleve(),
                entree.survenuLe());
    }

    static EntreeJournal versDomaine(AlerteJpaEntity e) {
        // SEC-MOY-02 : NUMERIC(5,4) protege l'ecriture Hibernate, mais un ALTER COLUMN futur
        // pourrait passer a une echelle > 4 et empoisonner tout GET /alertes via l'invariant
        // TauxDeSurvie.scale == 4. Normaliser defensivement decouple integrite BDD et disponibilite API.
        return EntreeJournal.reconstituer(
                e.getId(),
                e.getParcelleId(),
                e.getCode(),
                StatutParcelle.valueOf(e.getAncienStatut()),
                StatutParcelle.valueOf(e.getNouveauStatut()),
                new TauxDeSurvie(e.getTauxDeclencheur().setScale(4, RoundingMode.HALF_UP)),
                e.getDateReleve(),
                e.getSurvenuLe());
    }
}
