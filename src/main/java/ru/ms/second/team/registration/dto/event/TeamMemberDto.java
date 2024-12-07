package ru.ms.second.team.registration.dto.event;

import lombok.Builder;

@Builder
public record TeamMemberDto(
        Long eventId,
        Long userId,
        TeamMemberRole role
) {
}
