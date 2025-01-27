package ru.ms.second.team.registration.dto.registration.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Registration credentials")
public record CreatedRegistrationResponseDto(

        @Schema(description = "Registration id")
        Long id,

        @Schema(description = "Registration password")
        String password
) {
}
