package ru.ms.second.team.registration.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Registrations credentials")
public record RegistrationCredentials(

        @Positive
        @NotNull
        @Schema(description = "Registration id")
        Long id,

        @NotBlank
        @Size(min = 4, max = 4, message = "Password must contain 4 symbols")
        @Schema(description = "Author's username")
        String password
) {
}
