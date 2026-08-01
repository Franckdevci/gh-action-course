package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Localite;
import ci.ecotrack.parcelles.domaine.NombrePlants;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.domaine.ParcelleId;
import ci.ecotrack.parcelles.domaine.Superficie;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;

final class ParcelleMapper {

    private ParcelleMapper() {
    }

    static ParcelleJpaEntity versEntite(Parcelle parcelle) {
        return new ParcelleJpaEntity(
                parcelle.id().valeur(),
                parcelle.code().valeur(),
                parcelle.localite().valeur(),
                parcelle.superficie().valeur(),
                parcelle.plantsInitiaux().valeur(),
                parcelle.datePlantation(),
                parcelle.statut().name(),
                parcelle.dernierTaux() == null ? null : parcelle.dernierTaux().valeur(),
                parcelle.dateDernierReleve());
    }

    static Parcelle versDomaine(ParcelleJpaEntity entite) {
        return Parcelle.reconstituer(
                new ParcelleId(entite.getId()),
                new CodeParcelle(entite.getCode()),
                new Localite(entite.getLocalite()),
                new Superficie(entite.getSuperficie()),
                new NombrePlants(entite.getPlantsInitiaux()),
                entite.getDatePlantation(),
                StatutParcelle.valueOf(entite.getStatut()),
                entite.getDernierTaux() == null ? null : new TauxDeSurvie(entite.getDernierTaux()),
                entite.getDateDernierReleve());
    }
}
