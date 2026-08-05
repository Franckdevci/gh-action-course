package ci.ecotrack.releves.infrastructure.jpa;

import ci.ecotrack.releves.application.ReleveDoublonException;
import ci.ecotrack.releves.application.RelevesRepository;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.shared.Pagination;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    @Override
    public PageReleves listerParParcelleAntichronologique(UUID parcelleId, Pagination pagination) {
        Page<ReleveJpaEntity> page = springData.findByParcelleIdOrderByDateObservationDesc(
                parcelleId, PageRequest.of(pagination.page(), pagination.size()));
        List<Releve> contenu = page.getContent().stream()
                .map(ReleveMapper::versDomaine)
                .toList();
        return new PageReleves(contenu, page.getTotalElements());
    }
}
