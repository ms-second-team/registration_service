package ru.ms.second.team.registration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteRegistrationDto {
    @Positive
    @NotNull
    private Long id;
    @NotBlank
    @Size(min = 4, max = 4, message = "Password must contain 4 symbols")
    private String password;
}
