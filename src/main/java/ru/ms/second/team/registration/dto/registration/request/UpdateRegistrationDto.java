package ru.ms.second.team.registration.dto.registration.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import ru.ms.second.team.registration.util.StrongPassword;

@Builder
@Schema(description = "Registration updateRegistration data")
public record UpdateRegistrationDto(

        @Positive
        @NotNull
        @Schema(description = "Registration id")
        Long id,

        @NotBlank
        @Size(min = 8, message = "Password must contain at least 8 symbols")
        @StrongPassword
        @Schema(description = "Registration password")
        String password,

        @Pattern(regexp = "^(?!\\s*$).+", message = "Username must be either null or not blank")
        @Schema(description = "Author's username")
        String username,

        @Email(message = "Email must be either null or a valid email address")
        @Schema(description = "Author's email")
        String email,

        @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
        @Schema(description = "Author's phone")
        String phone
) {
}