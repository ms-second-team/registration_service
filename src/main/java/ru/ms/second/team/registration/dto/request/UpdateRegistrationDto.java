package ru.ms.second.team.registration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record UpdateRegistrationDto(
        @Positive
        @NotNull
        Long id,
        @NotBlank
        @Size(min = 4, max = 4, message = "Password must contain 4 symbols")
        String password,
        @Pattern(regexp = "^(?!\\s*$).+", message = "Username must be either null or not blank")
        String username,
        @Email(message = "Email must be either null or a valid email address")
        String email,
        @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
        String phone
) {
}
