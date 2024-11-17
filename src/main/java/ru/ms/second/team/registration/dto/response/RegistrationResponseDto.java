package ru.ms.second.team.registration.dto.response;

import lombok.Builder;

@Builder
public record RegistrationResponseDto(
        String username,
        String email,
        String phone,
        Long eventId
) {
}
