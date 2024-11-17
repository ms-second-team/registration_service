package ru.ms.second.team.registration.dto.response;

import lombok.Builder;

@Builder
public record CreatedRegistrationResponseDto(
        Long id,
        String password
) {
}
