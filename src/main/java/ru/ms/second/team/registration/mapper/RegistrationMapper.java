package ru.ms.second.team.registration.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.model.Registration;

@Slf4j
public class RegistrationMapper {
    public static Registration toRegistration(NewRegistrationDto registrationDto, String password) {
        log.info("RegistrationMapper: mapping NewRegistrationDto to Registration");
        return Registration.builder()
                .email(registrationDto.getEmail())
                .phone(registrationDto.getPhone())
                .eventId(registrationDto.getEventId())
                .password(password)
                .username(registrationDto.getUsername())
                .build();
    }

    public static UpdatedRegistrationResponseDto toUpdatedDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to UpdatedRegistrationResponseDto");
        return UpdatedRegistrationResponseDto.builder()
                .id(registration.getId())
                .email(registration.getEmail())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }

    public static CreatedRegistrationResponseDto toCreatedDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to CreatedRegistrationResponseDto");
        return CreatedRegistrationResponseDto.builder()
                .username(registration.getUsername())
                .password(registration.getPassword()).build();
    }

    public static RegistrationResponseDto toRegistrationResponseDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to RegistrationResponseDto");
        return RegistrationResponseDto.builder()
                .email(registration.getEmail())
                .eventId(registration.getEventId())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }
}
