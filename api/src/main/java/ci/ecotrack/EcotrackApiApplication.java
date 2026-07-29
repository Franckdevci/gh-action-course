package ci.ecotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(systemName = "EcoTrack API")
public class EcotrackApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcotrackApiApplication.class, args);
    }
}
