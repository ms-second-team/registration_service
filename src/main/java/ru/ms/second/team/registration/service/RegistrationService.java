package ru.ms.second.team.registration.service;

import ru.ms.second.team.registration.dto.request.DeleteRegistrationDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;

import java.util.List;

public interface RegistrationService {
    CreatedRegistrationResponseDto create(NewRegistrationDto creationDto);

    UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto);

    RegistrationResponseDto findById(Long id);

    List<RegistrationResponseDto> findAllByEventId(int page, int size, Long id);

    void delete(DeleteRegistrationDto deleteDto);
}
