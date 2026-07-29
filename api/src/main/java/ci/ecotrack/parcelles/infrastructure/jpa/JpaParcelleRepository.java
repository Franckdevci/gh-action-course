package ci.ecotrack.parcelles.infrastructure.jpa;

import ci.ecotrack.parcelles.application.CodeParcelleDejaUtiliseException;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.Parcelle;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

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
}
