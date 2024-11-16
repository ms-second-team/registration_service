package ru.ms.second.team.registration.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponseDto {
    private String username;
    private String email;
    private String phone;
    private Long eventId;
}
