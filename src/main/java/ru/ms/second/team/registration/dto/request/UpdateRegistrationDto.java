package ru.ms.second.team.registration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateRegistrationDto {
    @Positive
    @NotNull
    private Long id;
    @NotBlank
    @Size(min = 4, max = 4, message = "Password must contain 4 symbols")
    private String password;
    private String username;
    private String email;
    @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
    private String phone;
}
