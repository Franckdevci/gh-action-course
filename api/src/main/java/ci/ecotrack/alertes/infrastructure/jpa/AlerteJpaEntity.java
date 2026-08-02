package ci.ecotrack.alertes.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// SEC-ELEV-01 : immutabilite EX-F-07 R1 defendue en-app (@Immutable + updatable=false).
// Hardening BDD (REVOKE role) : voir V5__alerte.sql + issue prod Phase 10.
@Entity
@Immutable
@Table(name = "alerte")
class AlerteJpaEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "parcelle_id", nullable = false, updatable = false)
    private UUID parcelleId;

    @Column(name = "code", nullable = false, length = 20, updatable = false)
    private String code;

    @Column(name = "sens", nullable = false, length = 32, updatable = false)
    private String sens;

    @Column(name = "ancien_statut", nullable = false, length = 16, updatable = false)
    private String ancienStatut;

    @Column(name = "nouveau_statut", nullable = false, length = 16, updatable = false)
    private String nouveauStatut;

    @Column(name = "taux_declencheur", nullable = false, precision = 5, scale = 4, updatable = false)
    private BigDecimal tauxDeclencheur;

    @Column(name = "date_releve", nullable = false, updatable = false)
    private LocalDate dateReleve;

    @Column(name = "survenu_le", nullable = false, updatable = false)
    private Instant survenuLe;

    @Column(name = "cree_le", nullable = false, insertable = false, updatable = false)
    private Instant creeLe;

    protected AlerteJpaEntity() {
    }

    AlerteJpaEntity(UUID id,
                    UUID parcelleId,
                    String code,
                    String sens,
                    String ancienStatut,
                    String nouveauStatut,
                    BigDecimal tauxDeclencheur,
                    LocalDate dateReleve,
                    Instant survenuLe) {
        this.id = id;
        this.parcelleId = parcelleId;
        this.code = code;
        this.sens = sens;
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.tauxDeclencheur = tauxDeclencheur;
        this.dateReleve = dateReleve;
        this.survenuLe = survenuLe;
    }

    UUID getId() { return id; }
    UUID getParcelleId() { return parcelleId; }
    String getCode() { return code; }
    String getSens() { return sens; }
    String getAncienStatut() { return ancienStatut; }
    String getNouveauStatut() { return nouveauStatut; }
    BigDecimal getTauxDeclencheur() { return tauxDeclencheur; }
    LocalDate getDateReleve() { return dateReleve; }
    Instant getSurvenuLe() { return survenuLe; }
    Instant getCreeLe() { return creeLe; }
}
