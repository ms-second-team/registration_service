package ru.ms.second.team.registration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record NewRegistrationDto(
        @NotBlank(message = "Username cannot be blank")
        String username,
        @Email
        @NotBlank
        @Size(min = 6, max = 254, message = "Email's length cannot be less than 6 and more than 254 symbols")
        String email,
        @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
        @NotNull
        String phone,
        @Positive
        @NotNull
        Long eventId
) {
}
