package ci.ecotrack.releves;

import ci.ecotrack.releves.application.EnregistrerReleveCommande;
import ci.ecotrack.releves.application.EnregistrerReleveUseCase;
import ci.ecotrack.releves.domaine.DateObservation;
import ci.ecotrack.releves.domaine.NombrePlantsVivants;
import ci.ecotrack.releves.domaine.Releve;
import ci.ecotrack.releves.domaine.ReleveId;
import ci.ecotrack.shared.TauxDeSurvie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelevesServiceTest {

    private final EnregistrerReleveUseCase useCase = mock(EnregistrerReleveUseCase.class);
    private final RelevesService service = new RelevesService(useCase);

    @Test
    void should_deleguer_enregistrement_au_use_case() {
        EnregistrerReleveCommande commande = new EnregistrerReleveCommande(
                "PRC-2026-042", LocalDate.of(2026, 7, 20), 1700);
        Releve attendu = Releve.reconstituer(
                new ReleveId(UUID.randomUUID()),
                UUID.randomUUID(),
                new DateObservation(LocalDate.of(2026, 7, 20)),
                new NombrePlantsVivants(1700),
                new TauxDeSurvie(new BigDecimal("0.85")));
        when(useCase.executer(any(EnregistrerReleveCommande.class))).thenReturn(attendu);

        Releve rendu = service.enregistrer(commande);

        assertThat(rendu).isSameAs(attendu);
        verify(useCase).executer(commande);
    }
}
