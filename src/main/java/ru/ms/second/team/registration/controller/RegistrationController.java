package ru.ms.second.team.registration.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.ms.second.team.registration.dto.request.DeleteRegistrationDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
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
        log.info("RegistrationController: POST /registrations");
        return registrationService.create(registrationDto);
    }

    @PatchMapping
    public UpdatedRegistrationResponseDto update(@RequestBody @Valid UpdateRegistrationDto updateDto) {
        log.info("RegistrationController: PATCH /registrations");
        return registrationService.update(updateDto);
    }

    @GetMapping("/{id}")
    public RegistrationResponseDto findCommentById(@PathVariable @Positive Long id) {
        log.info("RegistrationController: GET /registrations/{}", id);
        return registrationService.findById(id);
    }

    @GetMapping
    public List<RegistrationResponseDto> findAllByEventId(@RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                          @RequestParam(defaultValue = "10") @Min(1) Integer size,
                                                          @RequestParam @Positive Long eventId) {
        log.info("RegistrationController: GET /registrations, params page={}, size={}, eventId={}",
                page, size, eventId);
        return registrationService.findAllByEventId(page, size, eventId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestBody @Valid DeleteRegistrationDto deleteDto) {
        log.info("RegistrationController: DELETE /registrations");
        registrationService.delete(deleteDto);
    }
}
