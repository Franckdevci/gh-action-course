package ci.ecotrack.parcelles;

import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.application.CreerParcelleUseCase;
import ci.ecotrack.parcelles.application.MettreAJourDernierReleveUseCase;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ParcellesService {

    private final CreerParcelleUseCase creerParcelleUseCase;
    private final MettreAJourDernierReleveUseCase mettreAJourDernierReleveUseCase;
    private final ParcellesRepository parcellesRepository;

    public ParcellesService(CreerParcelleUseCase creerParcelleUseCase,
                            MettreAJourDernierReleveUseCase mettreAJourDernierReleveUseCase,
                            ParcellesRepository parcellesRepository) {
        this.creerParcelleUseCase = creerParcelleUseCase;
        this.mettreAJourDernierReleveUseCase = mettreAJourDernierReleveUseCase;
        this.parcellesRepository = parcellesRepository;
    }

    @Transactional
    public Parcelle creer(CreerParcelleCommande commande) {
        return creerParcelleUseCase.executer(commande);
    }

    @Transactional(readOnly = true)
    public Optional<ParcelleReference> trouverParCode(String code) {
        return parcellesRepository.trouverParCode(new CodeParcelle(code))
                .map(p -> new ParcelleReference(
                        p.id().valeur(),
                        p.plantsInitiaux().valeur(),
                        p.datePlantation()));
    }

    @Transactional
    public void mettreAJourDernierReleve(UUID parcelleId,
                                         TauxDeSurvie taux,
                                         LocalDate dateObservation) {
        mettreAJourDernierReleveUseCase.executer(parcelleId, taux, dateObservation);
    }
}
