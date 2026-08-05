package ci.ecotrack.parcelles.application;

import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;
import org.springframework.stereotype.Service;

@Service
public class ConsulterFicheParcelleUseCase {

    private final ParcellesRepository parcellesRepository;

    public ConsulterFicheParcelleUseCase(ParcellesRepository parcellesRepository) {
        this.parcellesRepository = parcellesRepository;
    }

    public Parcelle executer(CodeParcelle code) {
        return parcellesRepository.trouverParCode(code)
                .orElseThrow(() -> new ParcelleReferenceIntrouvableException(
                        "Aucune parcelle avec le code " + code.valeur()));
    }
}
