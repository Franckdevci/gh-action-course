package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.Parcelle;

import java.time.Clock;

public class CreerParcelleUseCase {

    private final ParcellesRepository repository;
    private final Clock horloge;

    public CreerParcelleUseCase(ParcellesRepository repository, Clock horloge) {
        this.repository = repository;
        this.horloge = horloge;
    }

    public Parcelle executer(CreerParcelleCommande commande) {
        Parcelle parcelle = Parcelle.creer(
                commande.code(),
                commande.localite(),
                commande.superficie(),
                commande.plantsInitiaux(),
                commande.datePlantation(),
                horloge);
        return repository.enregistrer(parcelle);
    }
}
