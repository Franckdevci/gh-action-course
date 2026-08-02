package ci.ecotrack.alertes.application;

import ci.ecotrack.alertes.domaine.EntreeJournal;
import org.springframework.stereotype.Service;

@Service
public class JournaliserBasculeUseCase {

    private final AlertesRepository alertesRepository;

    public JournaliserBasculeUseCase(AlertesRepository alertesRepository) {
        this.alertesRepository = alertesRepository;
    }

    public EntreeJournal executer(JournaliserBasculeCommande commande) {
        EntreeJournal entree = EntreeJournal.consigner(
                commande.parcelleId(),
                commande.codeParcelle(),
                commande.ancienStatut(),
                commande.nouveauStatut(),
                commande.tauxDeclencheur(),
                commande.dateReleve(),
                commande.survenuLe());
        return alertesRepository.enregistrer(entree);
    }
}
