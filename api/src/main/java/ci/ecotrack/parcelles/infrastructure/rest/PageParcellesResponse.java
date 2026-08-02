package ci.ecotrack.parcelles.infrastructure.rest;

import ci.ecotrack.parcelles.application.ParcellesRepository;

import java.util.List;

record PageParcellesResponse(
        List<ParcelleResponse> contenu,
        int page,
        int taille,
        long total,
        int totalPages) {

    static PageParcellesResponse de(ParcellesRepository.PageParcelles page, int pageIndex, int size) {
        List<ParcelleResponse> contenu = page.contenu().stream().map(ParcelleResponse::de).toList();
        int totalPages = (int) Math.ceil((double) page.total() / size);
        return new PageParcellesResponse(contenu, pageIndex, size, page.total(), totalPages);
    }
}
