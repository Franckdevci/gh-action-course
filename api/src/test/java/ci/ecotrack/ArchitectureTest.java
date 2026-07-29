package ci.ecotrack;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void les_modules_verifient_leurs_frontieres() {
        ApplicationModules.of(EcotrackApiApplication.class).verify();
    }
}
