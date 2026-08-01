package ci.ecotrack.releves.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "releve")
class ReleveJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "parcelle_id", nullable = false)
    private UUID parcelleId;

    @Column(name = "date_observation", nullable = false)
    private LocalDate dateObservation;

    @Column(name = "plants_vivants", nullable = false)
    private Integer plantsVivants;

    @Column(name = "taux_survie", nullable = false, precision = 5, scale = 4)
    private BigDecimal tauxSurvie;

    @Column(name = "cree_le", nullable = false, insertable = false, updatable = false)
    private Instant creeLe;

    protected ReleveJpaEntity() {
    }

    ReleveJpaEntity(UUID id,
                    UUID parcelleId,
                    LocalDate dateObservation,
                    Integer plantsVivants,
                    BigDecimal tauxSurvie) {
        this.id = id;
        this.parcelleId = parcelleId;
        this.dateObservation = dateObservation;
        this.plantsVivants = plantsVivants;
        this.tauxSurvie = tauxSurvie;
    }

    UUID getId() { return id; }
    UUID getParcelleId() { return parcelleId; }
    LocalDate getDateObservation() { return dateObservation; }
    Integer getPlantsVivants() { return plantsVivants; }
    BigDecimal getTauxSurvie() { return tauxSurvie; }
    Instant getCreeLe() { return creeLe; }
}
