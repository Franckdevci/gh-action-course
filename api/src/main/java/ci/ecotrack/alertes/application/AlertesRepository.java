package ci.ecotrack.alertes.application;

import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.shared.Pagination;

import java.util.List;

public interface AlertesRepository {

    EntreeJournal enregistrer(EntreeJournal entree);

    PageEntreesJournal listerAntichronologique(Pagination pagination);

    record PageEntreesJournal(List<EntreeJournal> contenu, long total) {
    }
}
