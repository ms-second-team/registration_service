package ru.ms.second.team.registration.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.ms.second.team.registration.model.RegistrationStatus;

@Builder
@Schema(description = "Registration")
public record RegistrationResponseDto(

        @Schema(description = "Author's username")
        String username,

        @Schema(description = "Author's email")
        String email,

        @Schema(description = "Author's phone")
        String phone,

        @Schema(description = "Event id")
        Long eventId,

        @Schema(description = "Registration status")
        RegistrationStatus status,

        @Schema(description = "User id")
        Long userId
) {
}
