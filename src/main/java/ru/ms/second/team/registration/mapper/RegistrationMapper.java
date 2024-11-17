package ru.ms.second.team.registration.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.model.Registration;

@Slf4j
@Component
public class RegistrationMapper {
    public Registration toRegistration(NewRegistrationDto registrationDto, String password) {
        log.info("RegistrationMapper: mapping NewRegistrationDto to Registration");
        return Registration.builder()
                .email(registrationDto.email())
                .phone(registrationDto.phone())
                .eventId(registrationDto.eventId())
                .password(password)
                .username(registrationDto.username())
                .build();
    }

    public UpdatedRegistrationResponseDto toUpdatedDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to UpdatedRegistrationResponseDto");
        return UpdatedRegistrationResponseDto.builder()
                .id(registration.getId())
                .email(registration.getEmail())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }

    public CreatedRegistrationResponseDto toCreatedDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to CreatedRegistrationResponseDto");
        return CreatedRegistrationResponseDto.builder()
                .username(registration.getUsername())
                .password(registration.getPassword()).build();
    }

    public RegistrationResponseDto toRegistrationResponseDto(Registration registration) {
        log.info("RegistrationMapper: mapping Registration to RegistrationResponseDto");
        return RegistrationResponseDto.builder()
                .email(registration.getEmail())
                .eventId(registration.getEventId())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }
}
