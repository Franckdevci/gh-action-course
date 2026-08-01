package ci.ecotrack.shared;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ecotrack")
@Validated
public record EcotrackProperties(
        @Valid @NotNull Features features,
        @Valid @NotNull Export export,
        @Valid @NotNull Retention retention,
        @Valid @NotNull Admin admin) {

    public record Features(boolean exportCsv) {
    }

    public record Export(@Min(1) int maxLignes) {
    }

    public record Retention(@Min(value = 12, message = "La retention du journal des alertes doit etre >= 12 mois (SEC-I-06, exigence bailleur)")
                            int journalAlertesMois) {
    }

    public record Admin(boolean enabled) {
    }
}
