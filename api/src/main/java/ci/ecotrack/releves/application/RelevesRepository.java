package ci.ecotrack.releves.application;

import ci.ecotrack.releves.domaine.Releve;

import java.time.LocalDate;
import java.util.UUID;

public interface RelevesRepository {

    Releve enregistrer(Releve releve);

    boolean existePourParcelleEtDate(UUID parcelleId, LocalDate date);
}
