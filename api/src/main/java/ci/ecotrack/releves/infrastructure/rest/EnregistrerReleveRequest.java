package ci.ecotrack.releves.infrastructure.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

record EnregistrerReleveRequest(

        @NotNull(message = "requise")
        @PastOrPresent(message = "ne peut pas etre dans le futur")
        LocalDate dateObservation,

        @NotNull(message = "requis")
        @Min(value = 0, message = "doit etre positif ou nul")
        Integer plantsVivants) {
}
