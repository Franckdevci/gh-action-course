package ci.ecotrack.releves.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

interface SpringDataReleveRepository extends JpaRepository<ReleveJpaEntity, UUID> {

    boolean existsByParcelleIdAndDateObservation(UUID parcelleId, LocalDate dateObservation);

    // EX-F-06 R1 : historique antichronologique par dateObservation.
    // L'index releve(parcelle_id, date_observation DESC) (V3) supporte ce tri.
    Page<ReleveJpaEntity> findByParcelleIdOrderByDateObservationDesc(UUID parcelleId, Pageable pageable);
}
