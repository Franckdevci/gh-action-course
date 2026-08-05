package ci.ecotrack.releves.application;

import ci.ecotrack.parcelles.ParcelleReference;
import ci.ecotrack.parcelles.ParcellesService;
import ci.ecotrack.shared.Pagination;
import org.springframework.stereotype.Service;

@Service
public class ConsulterHistoriqueUseCase {

    private final ParcellesService parcellesService;
    private final RelevesRepository relevesRepository;

    public ConsulterHistoriqueUseCase(ParcellesService parcellesService,
                                      RelevesRepository relevesRepository) {
        this.parcellesService = parcellesService;
        this.relevesRepository = relevesRepository;
    }

    public RelevesRepository.PageReleves executer(String codeParcelle, Pagination pagination) {
        ParcelleReference reference = parcellesService.trouverParCode(codeParcelle)
                .orElseThrow(() -> new ParcelleIntrouvableException(
                        "Aucune parcelle avec le code " + codeParcelle));
        return relevesRepository.listerParParcelleAntichronologique(reference.id(), pagination);
    }
}
