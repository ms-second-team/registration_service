package ru.ms.second.team.registration.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
@Schema(name = "New registration data")
public record NewRegistrationDto(

        @NotBlank(message = "Username cannot be blank")
        @Schema(description = "Author's username")
        String username,

        @Email
        @NotBlank
        @Size(min = 6, max = 254, message = "Email's length cannot be less than 6 and more than 254 symbols")
        @Schema(description = "Author's email")
        String email,

        @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
        @NotNull
        @Schema(description = "Author's phone")
        String phone,

        @Positive
        @NotNull
        @Schema(description = "Event id")
        Long eventId
) {
}
