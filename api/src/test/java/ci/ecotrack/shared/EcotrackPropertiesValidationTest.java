package ci.ecotrack.shared;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EcotrackPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    void should_demarrer_when_valeurs_valides() {
        runner.withPropertyValues(
                        "ecotrack.features.export-csv=false",
                        "ecotrack.export.max-lignes=10000",
                        "ecotrack.retention.journal-alertes-mois=24",
                        "ecotrack.admin.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    EcotrackProperties props = context.getBean(EcotrackProperties.class);
                    assertThat(props.retention().journalAlertesMois()).isEqualTo(24);
                    assertThat(props.admin().enabled()).isFalse();
                });
    }

    @Test
    void should_echouer_when_retention_inferieure_a_12_mois() {
        runner.withPropertyValues(
                        "ecotrack.features.export-csv=false",
                        "ecotrack.export.max-lignes=10000",
                        "ecotrack.retention.journal-alertes-mois=6",
                        "ecotrack.admin.enabled=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(ConfigurationPropertiesBindException.class)
                            .rootCause()
                            .hasMessageContaining("journalAlertesMois")
                            .hasMessageContaining("SEC-I-06");
                });
    }

    @Test
    void should_echouer_when_retention_zero() {
        runner.withPropertyValues(
                        "ecotrack.features.export-csv=false",
                        "ecotrack.export.max-lignes=10000",
                        "ecotrack.retention.journal-alertes-mois=0",
                        "ecotrack.admin.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void should_echouer_when_max_lignes_zero() {
        runner.withPropertyValues(
                        "ecotrack.features.export-csv=false",
                        "ecotrack.export.max-lignes=0",
                        "ecotrack.retention.journal-alertes-mois=24",
                        "ecotrack.admin.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(EcotrackProperties.class)
    static class Config {
    }
}
