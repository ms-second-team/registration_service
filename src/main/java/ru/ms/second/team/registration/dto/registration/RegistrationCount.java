package ru.ms.second.team.registration.dto.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Registrations count for event")
public record RegistrationCount(

        @Schema(description = "Number of pending registrations")
        long numberOfPendingRegistrations,

        @Schema(description = "Number of approved registrations")
        long numberOfApprovedRegistrations,

        @Schema(description = "Number of waiting registrations")
        long numberOfWaitingRegistrations,

        @Schema(description = "Number of declined registrations")
        long numberOfDeclinedRegistrations
) {
}
