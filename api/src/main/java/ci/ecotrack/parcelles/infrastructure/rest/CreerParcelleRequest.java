package ci.ecotrack.parcelles.infrastructure.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

record CreerParcelleRequest(

        @NotBlank(message = "requis")
        @Pattern(regexp = "^PRC-\\d{4}-\\d{1,3}$",
                message = "format invalide, attendu PRC-AAAA-NNN")
        String code,

        @NotBlank(message = "requise")
        @Size(max = 100, message = "100 caracteres maximum")
        String localite,

        @NotNull(message = "requise")
        @DecimalMin(value = "0.01", message = "superficie minimale : 0.01 ha")
        @DecimalMax(value = "10000.00", message = "superficie maximale : 10000 ha")
        @Digits(integer = 5, fraction = 2, message = "au plus 2 decimales")
        BigDecimal superficie,

        @NotNull(message = "requis")
        @Positive(message = "doit etre strictement positif")
        Integer plantsInitiaux,

        @NotNull(message = "requise")
        @PastOrPresent(message = "ne peut pas etre dans le futur")
        LocalDate datePlantation) {
}
