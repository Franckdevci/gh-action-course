package ci.ecotrack.parcelles.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "parcelle")
class ParcelleJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "localite", nullable = false, length = 100)
    private String localite;

    @Column(name = "superficie", nullable = false, precision = 7, scale = 2)
    private BigDecimal superficie;

    @Column(name = "plants_initiaux", nullable = false)
    private Integer plantsInitiaux;

    @Column(name = "date_plantation", nullable = false)
    private LocalDate datePlantation;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "dernier_taux", precision = 5, scale = 4)
    private BigDecimal dernierTaux;

    @Column(name = "date_dernier_releve")
    private LocalDate dateDernierReleve;

    protected ParcelleJpaEntity() {
    }

    ParcelleJpaEntity(UUID id,
                      String code,
                      String localite,
                      BigDecimal superficie,
                      Integer plantsInitiaux,
                      LocalDate datePlantation,
                      String statut,
                      BigDecimal dernierTaux,
                      LocalDate dateDernierReleve) {
        this.id = id;
        this.code = code;
        this.localite = localite;
        this.superficie = superficie;
        this.plantsInitiaux = plantsInitiaux;
        this.datePlantation = datePlantation;
        this.statut = statut;
        this.dernierTaux = dernierTaux;
        this.dateDernierReleve = dateDernierReleve;
    }

    UUID getId() { return id; }
    String getCode() { return code; }
    String getLocalite() { return localite; }
    BigDecimal getSuperficie() { return superficie; }
    Integer getPlantsInitiaux() { return plantsInitiaux; }
    LocalDate getDatePlantation() { return datePlantation; }
    String getStatut() { return statut; }
    BigDecimal getDernierTaux() { return dernierTaux; }
    LocalDate getDateDernierReleve() { return dateDernierReleve; }
}
