package ru.ms.second.team.registration.controller;

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
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.service.RegistrationService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@RequestMapping("/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedRegistrationResponseDto create(@RequestBody @Valid NewRegistrationDto registrationDto) {
        log.debug("RegistrationController: POST /registrations");
        return registrationService.create(registrationDto);
    }

    @PatchMapping
    public UpdatedRegistrationResponseDto update(@RequestBody @Valid UpdateRegistrationDto updateDto) {
        log.debug("RegistrationController: PATCH /registrations");
        return registrationService.update(updateDto);
    }

    @GetMapping("/{id}")
    public RegistrationResponseDto findRegistrationById(@PathVariable @Positive Long id) {
        log.debug("RegistrationController: GET /registrations/{}", id);
        return registrationService.findById(id);
    }

    @GetMapping
    public List<RegistrationResponseDto> findAllByEventId(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @RequestParam(defaultValue = "10") @Positive int size,
                                                          @RequestParam @Positive Long eventId) {
        log.info("RegistrationController: GET /registrations, params page={}, size={}, eventId={}",
                page, size, eventId);
        return registrationService.findAllByEventId(page, size, eventId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestBody @Valid RegistrationCredentials deleteDto) {
        log.debug("RegistrationController: DELETE /registrations");
        registrationService.delete(deleteDto);
    }

    @PatchMapping("/{registrationId}/status")
    public RegistrationStatus updateRegistrationStatus(@RequestHeader("X-User-Id") Long userId,
                                                       @PathVariable Long registrationId,
                                                       @RequestParam RegistrationStatus newStatus,
                                                       @RequestBody @Valid RegistrationCredentials registrationCredentials) {
        validateStatus(newStatus);
        log.debug("Updating status for registration with id '{}'", registrationId);
        return registrationService.updateRegistrationStatus(userId, registrationId, newStatus, registrationCredentials);
    }

    @PatchMapping("/{registrationId}/status/decline")
    public RegistrationStatus declineRegistration(@RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable Long registrationId,
                                                  @RequestParam
                                                  @NotBlank(message = "Reason must be specified") String reason,
                                                  @RequestBody @Valid RegistrationCredentials registrationCredentials) {
        log.debug("Updating status for registration with id '{}'", registrationId);
        return registrationService.declineRegistration(userId, registrationId, reason, registrationCredentials);
    }

    @GetMapping("/search")
    public List<RegistrationResponseDto> searchRegistrations(@RequestParam List<RegistrationStatus> statuses,
                                                             @RequestParam Long eventId) {
        log.debug("Requesting registrations for event with id '{}', statuses: {}", eventId, statuses);
        return registrationService.searchRegistrations(statuses, eventId);
    }

    @GetMapping("/count")
    public RegistrationCount getRegistrationsCountByEventId(@RequestParam Long eventId) {
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
