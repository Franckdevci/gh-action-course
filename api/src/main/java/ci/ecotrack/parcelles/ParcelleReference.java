package ci.ecotrack.parcelles;

import java.time.LocalDate;
import java.util.UUID;

public record ParcelleReference(UUID id, int plantsInitiaux, LocalDate datePlantation) {
}
