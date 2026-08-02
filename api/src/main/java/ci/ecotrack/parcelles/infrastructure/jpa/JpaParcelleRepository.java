package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.shared.Pagination;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaParcelleRepository implements ParcellesRepository {

    private final SpringDataParcelleRepository springData;

    JpaParcelleRepository(SpringDataParcelleRepository springData) {
        this.springData = springData;
    }

    @Override
    public Parcelle enregistrer(Parcelle parcelle) {
        try {
            ParcelleJpaEntity persistee = springData.saveAndFlush(ParcelleMapper.versEntite(parcelle));
            return ParcelleMapper.versDomaine(persistee);
        } catch (DataIntegrityViolationException e) {
            throw new CodeParcelleDejaUtiliseException(
                    "Une parcelle avec ce code existe deja");
        }
    }

    @Override
    public Optional<Parcelle> trouverParCode(CodeParcelle code) {
        return springData.findByCode(code.valeur()).map(ParcelleMapper::versDomaine);
    }

    @Override
    public Optional<Parcelle> trouverParId(UUID id) {
        return springData.findById(id).map(ParcelleMapper::versDomaine);
    }

    @Override
    public void sauvegarder(Parcelle parcelle) {
        springData.saveAndFlush(ParcelleMapper.versEntite(parcelle));
    }

    @Override
    public PageParcelles listerAlertesPuisCode(Pagination pagination) {
        Page<ParcelleJpaEntity> page = springData.listerAlertesPuisCode(
                PageRequest.of(pagination.page(), pagination.size()));
        List<Parcelle> contenu = page.getContent().stream()
                .map(ParcelleMapper::versDomaine)
                .toList();
        return new PageParcelles(contenu, page.getTotalElements());
    }
}
