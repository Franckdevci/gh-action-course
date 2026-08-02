package ci.ecotrack.alertes.domaine;

import ci.ecotrack.shared.DonneeInvalideException;

public class BasculeInvalideException extends DonneeInvalideException {

    public BasculeInvalideException(String message) {
        super(message);
    }
}
