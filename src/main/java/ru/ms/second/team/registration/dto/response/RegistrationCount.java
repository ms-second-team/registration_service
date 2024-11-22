package ru.ms.second.team.registration.dto.response;

import lombok.Builder;

@Builder
public record RegistrationCount(

        long numberOfPendingRegistrations,

        long numberOfApprovedRegistrations,

        long numberOfWaitingRegistrations,

        long numberOfDeclinedRegistrations
) {
}
