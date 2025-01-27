package ru.ms.second.team.registration.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.ms.second.team.registration.dto.registration.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.registration.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.registration.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.model.Registration;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    @Mapping(target = "status", constant = "PENDING")
    Registration toModel(NewRegistrationDto newRegistration);

    UpdatedRegistrationResponseDto toUpdatedDto(Registration registration);

    CreatedRegistrationResponseDto toCreatedDto(Registration registration);

    RegistrationResponseDto toRegistrationDto(Registration registration);

    List<RegistrationResponseDto> toRegistraionDtoList(List<Registration> registrations);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRegistration(UpdateRegistrationDto updateDto, @MappingTarget Registration registration);
}
