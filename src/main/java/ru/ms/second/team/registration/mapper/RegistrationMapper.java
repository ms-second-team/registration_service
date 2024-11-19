package ru.ms.second.team.registration.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.model.Registration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RegistrationMapper {
    public Registration toRegistration(NewRegistrationDto registrationDto, String password) {
        log.debug("RegistrationMapper: mapping NewRegistrationDto {} password={} to Registration class",
                registrationDto.toString(), password);
        return Registration.builder()
                .email(registrationDto.email())
                .phone(registrationDto.phone())
                .eventId(registrationDto.eventId())
                .password(password)
                .username(registrationDto.username())
                .build();
    }

    public UpdatedRegistrationResponseDto toUpdatedDto(Registration registration) {
        log.debug("RegistrationMapper: mapping Registration {} to UpdatedRegistrationResponseDto",
                registration.toString());
        return UpdatedRegistrationResponseDto.builder()
                .id(registration.getId())
                .email(registration.getEmail())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }

    public CreatedRegistrationResponseDto toCreatedDto(Registration registration) {
        log.debug("RegistrationMapper: mapping Registration {} to CreatedRegistrationResponseDto",
                registration.toString());
        return CreatedRegistrationResponseDto.builder()
                .id(registration.getId())
                .password(registration.getPassword()).build();
    }

    public RegistrationResponseDto toRegistrationResponseDto(Registration registration) {
        log.debug("RegistrationMapper: mapping Registration {} to RegistrationResponseDto", registration.toString());
        return RegistrationResponseDto.builder()
                .email(registration.getEmail())
                .eventId(registration.getEventId())
                .phone(registration.getPhone())
                .username(registration.getUsername())
                .build();
    }

    public List<RegistrationResponseDto> toRegistrationResponseDtoList(Page<Registration> registrations) {
        if (!registrations.hasContent()) {
            return new ArrayList<>();
        }
        return registrations.getContent().stream()
                .map(this::toRegistrationResponseDto)
                .toList();
    }
}
