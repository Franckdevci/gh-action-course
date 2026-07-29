package ci.ecotrack.parcelles;

import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.application.CreerParcelleUseCase;
import ci.ecotrack.parcelles.domaine.Parcelle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParcellesService {

    private final CreerParcelleUseCase creerParcelleUseCase;

    public ParcellesService(CreerParcelleUseCase creerParcelleUseCase) {
        this.creerParcelleUseCase = creerParcelleUseCase;
    }

    @Transactional
    public Parcelle creer(CreerParcelleCommande commande) {
        return creerParcelleUseCase.executer(commande);
    }
}
