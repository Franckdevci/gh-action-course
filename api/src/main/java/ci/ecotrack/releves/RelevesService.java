package ci.ecotrack.releves;

import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.application.EnregistrerReleveUseCase;
import ci.ecotrack.releves.domaine.Releve;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelevesService {

    private final EnregistrerReleveUseCase enregistrerReleveUseCase;

    public RelevesService(EnregistrerReleveUseCase enregistrerReleveUseCase) {
        this.enregistrerReleveUseCase = enregistrerReleveUseCase;
    }

    @Transactional
    public Releve enregistrer(EnregistrerReleveCommande commande) {
        return enregistrerReleveUseCase.executer(commande);
    }
}
