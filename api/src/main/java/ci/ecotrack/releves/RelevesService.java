package ci.ecotrack.releves;

import ci.ecotrack.releves.application.ConsulterHistoriqueUseCase;
import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.application.EnregistrerReleveUseCase;
import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.Pagination;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelevesService {

    private final EnregistrerReleveUseCase enregistrerReleveUseCase;
    private final ConsulterHistoriqueUseCase consulterHistoriqueUseCase;

    public RelevesService(EnregistrerReleveUseCase enregistrerReleveUseCase,
                          ConsulterHistoriqueUseCase consulterHistoriqueUseCase) {
        this.enregistrerReleveUseCase = enregistrerReleveUseCase;
        this.consulterHistoriqueUseCase = consulterHistoriqueUseCase;
    }

    @Transactional
    public Releve enregistrer(EnregistrerReleveCommande commande) {
        return enregistrerReleveUseCase.executer(commande);
    }

    @Transactional(readOnly = true)
    public RelevesRepository.PageReleves consulterHistorique(String codeParcelle, Pagination pagination) {
        return consulterHistoriqueUseCase.executer(codeParcelle, pagination);
    }
}
