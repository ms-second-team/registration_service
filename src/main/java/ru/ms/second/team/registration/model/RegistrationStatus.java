package ru.ms.second.team.registration.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registration status")
public enum RegistrationStatus {

    @Schema(description = "Default status upon creation")
    PENDING,

    @Schema(description = "Waiting for approval")
    WAITING,

    @Schema(description = "Registration approved")
    APPROVED,

    @Schema(description = "Registration declined")
    DECLINED
}
