package ci.ecotrack.parcelles.application;

import ci.ecotrack.shared.Pagination;
import org.springframework.stereotype.Service;

@Service
public class ConsulterParcellesUseCase {

    private final ParcellesRepository parcellesRepository;

    public ConsulterParcellesUseCase(ParcellesRepository parcellesRepository) {
        this.parcellesRepository = parcellesRepository;
    }

    public ParcellesRepository.PageParcelles executer(Pagination pagination) {
        return parcellesRepository.listerAlertesPuisCode(pagination);
    }
}
