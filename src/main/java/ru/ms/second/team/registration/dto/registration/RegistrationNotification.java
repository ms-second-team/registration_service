package ru.ms.second.team.registration.dto.registration;

import lombok.Builder;

@Builder
public record RegistrationNotification(

        Long eventOwnerId,

        String eventName,

        String participantEmail
) {
}
