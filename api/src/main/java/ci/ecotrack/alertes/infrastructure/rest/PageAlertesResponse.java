package ci.ecotrack.alertes.infrastructure.rest;

import ci.ecotrack.alertes.application.AlertesRepository;

import java.util.List;

record PageAlertesResponse(
        List<AlerteResponse> contenu,
        int page,
        int taille,
        long total,
        int totalPages) {

    static PageAlertesResponse de(AlertesRepository.PageEntreesJournal page, int pageIndex, int size) {
        List<AlerteResponse> contenu = page.contenu().stream().map(AlerteResponse::de).toList();
        int totalPages = (int) Math.ceil((double) page.total() / size);
        return new PageAlertesResponse(contenu, pageIndex, size, page.total(), totalPages);
    }
}
