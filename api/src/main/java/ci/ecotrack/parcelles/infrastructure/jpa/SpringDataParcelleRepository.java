package ci.ecotrack.parcelles.infrastructure.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface SpringDataParcelleRepository extends JpaRepository<ParcelleJpaEntity, UUID> {

    Optional<ParcelleJpaEntity> findByCode(String code);

    // EX-F-05 R1 : tri explicite EN_ALERTE d'abord puis code croissant.
    // On evite Sort.by("statut") qui reposerait sur l'ordre alphabetique des noms d'enum,
    // fragile si un futur StatutParcelle est introduit (ex. EN_CLOTURE).
    @Query("SELECT p FROM ParcelleJpaEntity p "
            + "ORDER BY CASE WHEN p.statut = 'EN_ALERTE' THEN 0 ELSE 1 END, p.code")
    Page<ParcelleJpaEntity> listerAlertesPuisCode(Pageable pageable);
}
