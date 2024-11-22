package ru.ms.second.team.registration.dto.response;

import lombok.Builder;
import ru.ms.second.team.registration.model.RegistrationStatus;

@Builder
public record UpdatedRegistrationResponseDto(
        Long id,
        String username,
        String email,
        String phone,
        RegistrationStatus status
) {
}
