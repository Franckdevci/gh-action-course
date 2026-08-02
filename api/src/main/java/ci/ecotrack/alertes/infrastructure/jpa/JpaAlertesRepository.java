package ci.ecotrack.alertes.infrastructure.jpa;

import ci.ecotrack.alertes.application.AlertesRepository;
import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.shared.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JpaAlertesRepository implements AlertesRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaAlertesRepository.class);

    private final SpringDataAlerteRepository springData;

    JpaAlertesRepository(SpringDataAlerteRepository springData) {
        this.springData = springData;
    }

    // SEC-ELEV-02 : le contrat Modulith est at-least-once. Check-first pour eviter la corruption
    // de la transaction Hibernate en rollback-only apres DIVE. La contrainte UNIQUE(parcelle_id,
    // survenu_le) en base (V5) reste le rempart final contre les inserts concurrents (race window
    // etroite, on retourne alors l'entree d'entree — le listener n'utilise pas l'ID en retour).
    @Override
    public EntreeJournal enregistrer(EntreeJournal entree) {
        var existante = springData.findByParcelleIdAndSurvenuLe(entree.parcelleId(), entree.survenuLe());
        if (existante.isPresent()) {
            log.info("Rejeu idempotent ignore (check-first): parcelle={} survenuLe={}",
                    entree.code(), entree.survenuLe());
            return AlerteMapper.versDomaine(existante.get());
        }
        try {
            AlerteJpaEntity persistee = springData.saveAndFlush(AlerteMapper.versEntite(entree));
            return AlerteMapper.versDomaine(persistee);
        } catch (DataIntegrityViolationException e) {
            log.info("Rejeu concurrent detecte (race UNIQUE): parcelle={} survenuLe={}",
                    entree.code(), entree.survenuLe());
            return entree;
        }
    }

    @Override
    public PageEntreesJournal listerAntichronologique(Pagination pagination) {
        Page<AlerteJpaEntity> page = springData.findAllByOrderBySurvenuLeDesc(
                PageRequest.of(pagination.page(), pagination.size()));
        List<EntreeJournal> contenu = page.getContent().stream()
                .map(AlerteMapper::versDomaine)
                .toList();
        return new PageEntreesJournal(contenu, page.getTotalElements());
    }
}
