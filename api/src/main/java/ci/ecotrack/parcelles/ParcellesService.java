package ci.ecotrack.parcelles;

import ci.ecotrack.parcelles.application.ConsulterFicheParcelleUseCase;
import ci.ecotrack.parcelles.application.ConsulterParcellesUseCase;
import ci.ecotrack.parcelles.application.CreerParcelleCommande;
import ci.ecotrack.parcelles.application.CreerParcelleUseCase;
import ci.ecotrack.parcelles.application.MettreAJourDernierReleveUseCase;
import ci.ecotrack.parcelles.application.ParcellesRepository;
import ci.ecotrack.parcelles.domaine.CodeParcelle;
import ci.ecotrack.parcelles.domaine.Parcelle;
import ci.ecotrack.parcelles.StatutChange;
import ci.ecotrack.shared.Pagination;
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
    private final ConsulterParcellesUseCase consulterParcellesUseCase;
    private final ConsulterFicheParcelleUseCase consulterFicheParcelleUseCase;
    private final ParcellesRepository parcellesRepository;

    public ParcellesService(CreerParcelleUseCase creerParcelleUseCase,
                            MettreAJourDernierReleveUseCase mettreAJourDernierReleveUseCase,
                            ConsulterParcellesUseCase consulterParcellesUseCase,
                            ConsulterFicheParcelleUseCase consulterFicheParcelleUseCase,
                            ParcellesRepository parcellesRepository) {
        this.creerParcelleUseCase = creerParcelleUseCase;
        this.mettreAJourDernierReleveUseCase = mettreAJourDernierReleveUseCase;
        this.consulterParcellesUseCase = consulterParcellesUseCase;
        this.consulterFicheParcelleUseCase = consulterFicheParcelleUseCase;
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
    public Optional<StatutChange> mettreAJourDernierReleve(UUID parcelleId,
                                                            TauxDeSurvie taux,
                                                            LocalDate dateObservation) {
        return mettreAJourDernierReleveUseCase.executer(parcelleId, taux, dateObservation);
    }

    @Transactional(readOnly = true)
    public ParcellesRepository.PageParcelles consulter(Pagination pagination) {
        return consulterParcellesUseCase.executer(pagination);
    }

    @Transactional(readOnly = true)
    public Parcelle consulterFiche(String code) {
        return consulterFicheParcelleUseCase.executer(new CodeParcelle(code));
    }
}
