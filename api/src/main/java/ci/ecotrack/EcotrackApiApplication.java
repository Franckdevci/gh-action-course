package ci.ecotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

import java.time.Clock;

@SpringBootApplication
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
