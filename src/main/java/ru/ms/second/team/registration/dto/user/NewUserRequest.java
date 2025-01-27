package ru.ms.second.team.registration.dto.user;

import lombok.Builder;

@Builder
public record NewUserRequest(
        String name,
        String email,
        String password,
        String aboutMe
) {
}