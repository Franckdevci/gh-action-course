package ci.ecotrack.alertes.application;

import ci.ecotrack.alertes.domaine.EntreeJournal;
import ci.ecotrack.alertes.domaine.SensDeBascule;
import ci.ecotrack.shared.Pagination;
import ci.ecotrack.shared.StatutParcelle;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JournaliserBasculeUseCaseTest {

    private final AlertesRepositoryEnMemoire repo = new AlertesRepositoryEnMemoire();
    private final JournaliserBasculeUseCase useCase = new JournaliserBasculeUseCase(repo);

    @Test
    void should_persister_entree_passage_en_alerte_when_bascule_valide() {
        UUID parcelleId = UUID.randomUUID();
        JournaliserBasculeCommande cmd = new JournaliserBasculeCommande(
                parcelleId, "PRC-2026-042",
                StatutParcelle.EN_SUIVI, StatutParcelle.EN_ALERTE,
                new TauxDeSurvie(new BigDecimal("0.5995")),
                LocalDate.of(2026, 7, 20),
                Instant.parse("2026-07-20T10:15:30Z"));

        EntreeJournal enregistree = useCase.executer(cmd);

        assertThat(enregistree.sens()).isEqualTo(SensDeBascule.PASSAGE_EN_ALERTE);
        assertThat(enregistree.code()).isEqualTo("PRC-2026-042");
        assertThat(repo.contenu).hasSize(1);
    }

    private static final class AlertesRepositoryEnMemoire implements AlertesRepository {
        private final List<EntreeJournal> contenu = new ArrayList<>();

        @Override
        public EntreeJournal enregistrer(EntreeJournal entree) {
            contenu.add(entree);
            return entree;
        }

        @Override
        public PageEntreesJournal listerAntichronologique(Pagination pagination) {
            List<EntreeJournal> trie = contenu.stream()
                    .sorted(Comparator.comparing(EntreeJournal::survenuLe).reversed())
                    .toList();
            int from = Math.min(pagination.page() * pagination.size(), trie.size());
            int to = Math.min(from + pagination.size(), trie.size());
            return new PageEntreesJournal(trie.subList(from, to), trie.size());
        }
    }
}
