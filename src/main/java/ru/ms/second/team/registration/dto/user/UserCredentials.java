package ru.ms.second.team.registration.dto.user;

import lombok.Builder;

@Builder
public record UserCredentials(
        String email,
        String password
) {
}
