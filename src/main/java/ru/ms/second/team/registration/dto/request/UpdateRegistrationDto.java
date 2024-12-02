package ru.ms.second.team.registration.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
@Schema(description = "Registration update data")
public record UpdateRegistrationDto(

        @Positive
        @NotNull
        @Schema(description = "Registration id")
        Long id,

        @NotBlank
        @Size(min = 4, max = 4, message = "Password must contain 4 symbols")
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
