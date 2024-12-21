package ru.ms.second.team.registration.dto.registration.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.ms.second.team.registration.model.RegistrationStatus;

@Builder
@Schema(description = "Updated registration")
public record UpdatedRegistrationResponseDto(

        @Schema(description = "Registration id")
        Long id,

        @Schema(description = "Author's id")
        String username,

        @Schema(description = "Author's email")
        String email,

        @Schema(description = "Author's phone")
        String phone,

        @Schema(description = "Registration status")
        RegistrationStatus status
) {
}
