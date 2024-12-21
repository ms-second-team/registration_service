package ru.ms.second.team.registration.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.ms.second.team.registration.dto.registration.NewRegistrationDto;
import ru.ms.second.team.registration.dto.registration.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.registration.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.UpdatedRegistrationResponseDto;
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
