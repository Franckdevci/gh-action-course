package ci.ecotrack.alertes.application;

import ci.ecotrack.shared.Pagination;
import org.springframework.stereotype.Service;

@Service
public class ConsulterJournalUseCase {

    private final AlertesRepository alertesRepository;

    public ConsulterJournalUseCase(AlertesRepository alertesRepository) {
        this.alertesRepository = alertesRepository;
    }

    public AlertesRepository.PageEntreesJournal executer(Pagination pagination) {
        return alertesRepository.listerAntichronologique(pagination);
    }
}
