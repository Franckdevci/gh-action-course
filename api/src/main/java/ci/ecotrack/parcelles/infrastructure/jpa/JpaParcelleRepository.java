package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

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
}
