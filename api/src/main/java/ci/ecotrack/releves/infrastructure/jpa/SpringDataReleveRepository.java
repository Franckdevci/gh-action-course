package ci.ecotrack.releves.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

interface SpringDataReleveRepository extends JpaRepository<ReleveJpaEntity, UUID> {

    boolean existsByParcelleIdAndDateObservation(UUID parcelleId, LocalDate dateObservation);
}
