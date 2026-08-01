package ci.ecotrack.releves.domaine;

import java.time.Clock;
import java.time.LocalDate;

public record DateObservation(LocalDate valeur) {

    public DateObservation {
        if (valeur == null) {
            throw new DonneeReleveInvalideException("La date d'observation est requise");
        }
    }

    public void validerCoherenceAvec(LocalDate datePlantation, Clock horloge) {
        if (valeur.isAfter(LocalDate.now(horloge))) {
            throw new DonneeReleveInvalideException(
                    "La date d'observation ne peut pas etre dans le futur");
        }
        if (valeur.isBefore(datePlantation)) {
            throw new DonneeReleveInvalideException(
                    "La date d'observation ne peut pas etre anterieure a la date de plantation");
        }
    }
}
