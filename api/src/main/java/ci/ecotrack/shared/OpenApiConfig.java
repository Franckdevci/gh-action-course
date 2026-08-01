package ci.ecotrack.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfig {

    private final String applicationName;
    private final String version;

    OpenApiConfig(@Value("${spring.application.name:ecotrack-api}") String applicationName,
                  @Value("${info.build.version:0.1.0-SNAPSHOT}") String version) {
        this.applicationName = applicationName;
        this.version = version;
    }

    @Bean
    OpenAPI ecotrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version(version)
                        .description("""
                                API EcoTrack - suivi de parcelles de reboisement.
                                Contrats metier : docs/srs.md (SRS). Conception : docs/sdd.md (SDD).
                                Erreurs : format RFC 7807 (application/problem+json).
                                """)
                        .contact(new Contact().name("EcoTrack"))
                        .license(new License().name("Interne")));
    }
}
