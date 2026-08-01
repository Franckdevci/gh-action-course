package ci.ecotrack;

import ci.ecotrack.shared.EcotrackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(EcotrackProperties.class)
@Modulithic(systemName = "EcoTrack API")
public class EcotrackApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcotrackApiApplication.class, args);
    }

    @Bean
    Clock horlogeSysteme() {
        return Clock.systemUTC();
    }
}
