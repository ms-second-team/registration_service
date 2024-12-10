package ru.ms.second.team.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationCount;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.model.ErrorResponse;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.service.RegistrationService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@RequestMapping("/registrations")
@Tag(name = "Registrations API")
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "Create registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created new registration", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = CreatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedRegistrationResponseDto createRegistration(@Parameter(description = "New registration data")
                                                             @RequestBody @Valid NewRegistrationDto registrationDto) {
        log.debug("RegistrationController: POST /registrations");
        return registrationService.createRegistration(registrationDto);
    }

    @Operation(summary = "Update registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration is updated", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Wrong password", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping
    public UpdatedRegistrationResponseDto updateRegistration(@Parameter(
            description = "New registration data for updateRegistration")
                                                             @RequestBody @Valid UpdateRegistrationDto updateDto) {
        log.debug("RegistrationController: PATCH /registrations");
        return registrationService.updateRegistration(updateDto);
    }

    @Operation(summary = "Find registration by registration id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration is found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping("/{id}")
    public RegistrationResponseDto findRegistrationById(@Parameter(description = "Registration id")
                                                        @PathVariable @Positive Long id) {
        log.debug("RegistrationController: GET /registrations/{}", id);
        return registrationService.findRegistrationById(id);
    }

    @Operation(summary = "Find registrations by event id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registrations are found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping
    public List<RegistrationResponseDto> findAllRegistrationsByEventId(@Parameter(description = "Page number")
                                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                       @Parameter(description =
                                                                               "Number of registrations per page")
                                                                       @RequestParam(defaultValue = "10") @Positive int size,
                                                                       @Parameter(description = "Event id")
                                                                       @RequestParam @Positive Long eventId) {
        log.info("RegistrationController: GET /registrations, params page={}, size={}, eventId={}",
                page, size, eventId);
        return registrationService.findAllRegistrationsByEventId(page, size, eventId);
    }

    @Operation(summary = "Delete registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration is deleted", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Wrong password", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegistration(@Parameter(description = "Registration credentials")
                                   @RequestBody @Valid RegistrationCredentials deleteDto) {
        log.debug("RegistrationController: DELETE /registrations");
        registrationService.deleteRegistration(deleteDto);
    }

    @Operation(summary = "Update registration status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration status is updated", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Wrong password", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping("/{registrationId}/status")
    public RegistrationStatus updateRegistrationStatus(@RequestHeader("X-User-Id") Long userId,
                                                       @Parameter(description = "Registration id")
                                                       @PathVariable Long registrationId,
                                                       @Parameter(description = "New registration status")
                                                       @RequestParam RegistrationStatus newStatus,
                                                       @Parameter(description = "Registration credentials")
                                                       @RequestBody @Valid RegistrationCredentials registrationCredentials) {
        validateStatus(newStatus);
        log.debug("Updating status for registration with id '{}'", registrationId);
        return registrationService.updateRegistrationStatus(userId, registrationId, newStatus, registrationCredentials);
    }

    @Operation(summary = "Decline registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration is declined", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Wrong password", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping("/{registrationId}/status/decline")
    public RegistrationStatus declineRegistration(@RequestHeader("X-User-Id") Long userId,
                                                  @Parameter(description = "Registration id")
                                                  @PathVariable Long registrationId,
                                                  @Parameter(description = "Decline reason")
                                                  @RequestParam
                                                  @NotBlank(message = "Reason must be specified") String reason,
                                                  @Parameter(description = "Registration credentials")
                                                  @RequestBody @Valid RegistrationCredentials registrationCredentials) {
        log.debug("Updating status for registration with id '{}'", registrationId);
        return registrationService.declineRegistration(userId, registrationId, reason, registrationCredentials);
    }

    @Operation(summary = "Search registrations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registrations found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping("/search")
    public List<RegistrationResponseDto> searchRegistrations(@Parameter(description = "List of statuses")
                                                             @RequestParam List<RegistrationStatus> statuses,
                                                             @Parameter(description = "Event id")
                                                             @RequestParam Long eventId) {
        log.debug("Requesting registrations for event with id '{}', statuses: {}", eventId, statuses);
        return registrationService.searchRegistrations(statuses, eventId);
    }

    @Operation(summary = "Get registrations count for event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registrations count for event found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(
                            implementation = UpdatedRegistrationResponseDto.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping("/count")
    public RegistrationCount getRegistrationsCountByEventId(@Parameter(description = "Event id")
                                                            @RequestParam Long eventId) {
        log.debug("Requesting registrations count for event with id '{}'", eventId);
        return registrationService.getRegistrationsCountByEventId(eventId);
    }

    private void validateStatus(RegistrationStatus newStatus) {
        if (newStatus.equals(RegistrationStatus.DECLINED)) {
            throw new IllegalArgumentException("Illegal status. To decline registration use different endpoint. " +
                    "Status: " + newStatus);
        }
    }
}
