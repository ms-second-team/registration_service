package ru.ms.second.team.registration.dto.response;

import lombok.Builder;

@Builder
public record UpdatedRegistrationResponseDto(
        Long id,
        String username,
        String email,
        String phone
) {
}
