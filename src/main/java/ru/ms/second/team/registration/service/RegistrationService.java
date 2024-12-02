package ru.ms.second.team.registration.service;

import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationCount;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.model.RegistrationStatus;

import java.util.List;

public interface RegistrationService {

    CreatedRegistrationResponseDto create(NewRegistrationDto creationDto);

    UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto);

    RegistrationResponseDto findById(Long id);

    List<RegistrationResponseDto> findAllByEventId(int page, int size, Long id);

    void delete(RegistrationCredentials deleteDto);

    RegistrationStatus updateRegistrationStatus(Long userId, Long registrationId, RegistrationStatus newStatus,
                                                RegistrationCredentials registrationCredentials);

    RegistrationStatus declineRegistration(Long userId, Long registrationId, String reason,
                                           RegistrationCredentials registrationCredentials);

    List<RegistrationResponseDto> searchRegistrations(List<RegistrationStatus> statuses, Long eventId);

    RegistrationCount getRegistrationsCountByEventId(Long eventId);
}
