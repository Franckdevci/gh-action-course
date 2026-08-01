package ci.ecotrack.parcelles.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataParcelleRepository extends JpaRepository<ParcelleJpaEntity, UUID> {

    Optional<ParcelleJpaEntity> findByCode(String code);
}
