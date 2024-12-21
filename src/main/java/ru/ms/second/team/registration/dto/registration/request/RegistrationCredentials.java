package ru.ms.second.team.registration.dto.registration.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ru.ms.second.team.registration.util.StrongPassword;

@Builder
@Schema(description = "Registrations credentials")
public record RegistrationCredentials(

        @Positive
        @NotNull
        @Schema(description = "Registration id")
        Long id,

        @NotBlank
        @Size(min = 8, message = "Password must contain at least 8 symbols")
        @StrongPassword
        @Schema(description = "Author's username")
        String password
) {
}
