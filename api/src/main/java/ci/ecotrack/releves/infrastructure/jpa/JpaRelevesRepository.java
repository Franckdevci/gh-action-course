package ci.ecotrack.releves.infrastructure.jpa;

import ci.ecotrack.releves.application.ReleveDoublonException;
import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.releves.domaine.Releve;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
class JpaRelevesRepository implements RelevesRepository {

    private final SpringDataReleveRepository springData;

    JpaRelevesRepository(SpringDataReleveRepository springData) {
        this.springData = springData;
    }

    @Override
    public Releve enregistrer(Releve releve) {
        try {
            ReleveJpaEntity persistee = springData.saveAndFlush(ReleveMapper.versEntite(releve));
            return ReleveMapper.versDomaine(persistee);
        } catch (DataIntegrityViolationException e) {
            throw new ReleveDoublonException(
                    "Un releve existe deja pour cette parcelle a cette date");
        }
    }

    @Override
    public boolean existePourParcelleEtDate(UUID parcelleId, LocalDate date) {
        return springData.existsByParcelleIdAndDateObservation(parcelleId, date);
    }
}
