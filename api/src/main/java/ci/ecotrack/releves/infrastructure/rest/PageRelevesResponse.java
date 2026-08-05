package ci.ecotrack.releves.infrastructure.rest;

import ci.ecotrack.releves.application.RelevesRepository;

import java.util.List;

record PageRelevesResponse(
        List<ReleveResponse> contenu,
        int page,
        int taille,
        long total,
        int totalPages) {

    static PageRelevesResponse de(RelevesRepository.PageReleves page, int pageIndex, int size) {
        List<ReleveResponse> contenu = page.contenu().stream().map(ReleveResponse::de).toList();
        int totalPages = (int) Math.ceil((double) page.total() / size);
        return new PageRelevesResponse(contenu, pageIndex, size, page.total(), totalPages);
    }
}
