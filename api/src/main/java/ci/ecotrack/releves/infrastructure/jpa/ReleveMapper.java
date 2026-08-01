package ci.ecotrack.releves.infrastructure.jpa;

import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.releves.domaine.ReleveId;
import ci.ecotrack.shared.TauxDeSurvie;

final class ReleveMapper {

    private ReleveMapper() {
    }

    static ReleveJpaEntity versEntite(Releve releve) {
        return new ReleveJpaEntity(
                releve.id().valeur(),
                releve.parcelleId(),
                releve.dateObservation().valeur(),
                releve.plantsVivants().valeur(),
                releve.tauxDeSurvie().valeur());
    }

    static Releve versDomaine(ReleveJpaEntity entite) {
        return Releve.reconstituer(
                new ReleveId(entite.getId()),
                entite.getParcelleId(),
                new DateObservation(entite.getDateObservation()),
                new NombrePlantsVivants(entite.getPlantsVivants()),
                new TauxDeSurvie(entite.getTauxSurvie()));
    }
}
