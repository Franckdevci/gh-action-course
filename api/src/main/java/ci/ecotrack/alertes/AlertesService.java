package ci.ecotrack.alertes;

import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.alertes.application.ConsulterJournalUseCase;
import ci.ecotrack.alertes.application.JournaliserBasculeCommande;
import ci.ecotrack.alertes.application.JournaliserBasculeUseCase;
import ci.ecotrack.releves.StatutParcelleChange;
import ci.ecotrack.shared.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertesService {

    private static final Logger log = LoggerFactory.getLogger(AlertesService.class);

    private final JournaliserBasculeUseCase journaliserBasculeUseCase;
    private final ConsulterJournalUseCase consulterJournalUseCase;

    public AlertesService(JournaliserBasculeUseCase journaliserBasculeUseCase,
                          ConsulterJournalUseCase consulterJournalUseCase) {
        this.journaliserBasculeUseCase = journaliserBasculeUseCase;
        this.consulterJournalUseCase = consulterJournalUseCase;
    }

    // SEC-ELEV-03 : on trace tout echec du listener (parcelle + survenu_le) puis on rethrow.
    // Modulith laisse alors event_publication.completion_date NULL → l'event sera rejoue au boot
    // (durabilite EX-NF-03 conservee). Un compteur d'echecs / bascule FAILED reste dette ADR-008.
    @ApplicationModuleListener
    public void surStatutParcelleChange(StatutParcelleChange evt) {
        try {
            journaliserBasculeUseCase.executer(new JournaliserBasculeCommande(
                    evt.parcelleId(),
                    evt.code(),
                    evt.ancienStatut(),
                    evt.nouveauStatut(),
                    evt.tauxDeclencheur(),
                    evt.dateReleve(),
                    evt.survenuLe()));
            log.info("Bascule journalisee: parcelle={} ancien={} nouveau={}",
                    evt.code(), evt.ancienStatut(), evt.nouveauStatut());
        } catch (RuntimeException e) {
            log.error("Echec journalisation bascule: parcelle={} survenuLe={} cause={}",
                    evt.code(), evt.survenuLe(), e.getClass().getSimpleName());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public AlertesRepository.PageEntreesJournal consulter(Pagination pagination) {
        return consulterJournalUseCase.executer(pagination);
    }
}
