package ci.ecotrack.alertes.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAlerteRepository extends JpaRepository<AlerteJpaEntity, UUID> {

    Page<AlerteJpaEntity> findAllByOrderBySurvenuLeDesc(Pageable pageable);

    Optional<AlerteJpaEntity> findByParcelleIdAndSurvenuLe(UUID parcelleId, Instant survenuLe);
}
