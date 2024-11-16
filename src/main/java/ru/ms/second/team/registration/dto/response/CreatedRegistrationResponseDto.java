package ru.ms.second.team.registration.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatedRegistrationResponseDto {
    private String username;
    private String password;
}
