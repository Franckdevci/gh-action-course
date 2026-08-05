package ci.ecotrack.releves.application;

import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.Pagination;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RelevesRepository {

    Releve enregistrer(Releve releve);

    boolean existePourParcelleEtDate(UUID parcelleId, LocalDate date);

    PageReleves listerParParcelleAntichronologique(UUID parcelleId, Pagination pagination);

    record PageReleves(List<Releve> contenu, long total) {
    }
}
