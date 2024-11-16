package ru.ms.second.team.registration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class NewRegistrationDto {

    @NotBlank(message = "Username cannot be blank")
    private String username;
    @Email
    @NotBlank
    @Size(min = 6, max = 254, message = "Email's length cannot be less than 6 and more than 254 symbols")
    private String email;
    @Pattern(regexp = "7\\d{10}", message = "Phone Number is Incorrect")
    @NotNull
    private String phone;
    @Positive
    @NotNull
    private Long eventId;
}
