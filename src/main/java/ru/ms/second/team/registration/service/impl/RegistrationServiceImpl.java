package ru.ms.second.team.registration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.ms.second.team.registration.dto.request.DeleteRegistrationDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.mapper.RegistrationMapper;
import ru.ms.second.team.registration.model.Registration;
import ru.ms.second.team.registration.repository.JpaRegistrationRepository;
import ru.ms.second.team.registration.service.RegistrationService;

import java.security.SecureRandom;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final JpaRegistrationRepository registrationRepository;
    private final RegistrationMapper mapper;

    @Override
    public CreatedRegistrationResponseDto create(NewRegistrationDto creationDto) {
        log.info("RegistrationService: executing create method. Username {}, email {}, phone {}, eventId {}",
                creationDto.username(), creationDto.email(), creationDto.phone(), creationDto.eventId());

        Registration registration = mapper.toRegistration(creationDto, generatePassword());
        registration = registrationRepository.save(registration);

        return mapper.toCreatedDto(registration);
    }

    @Override
    public UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing update method. Updating registration with id {}, updateDto {}",
                updateDto.id(), updateDto);

        Registration registration = findRegistrationOrThrow(updateDto.id());
        checkPasswordOrThrow(registration.getPassword(), updateDto.password());
        doTheUpdate(registration, updateDto);
        registration = registrationRepository.save(registration);
        return mapper.toUpdatedDto(registration);
    }

    @Override
    public RegistrationResponseDto findById(Long id) {
        log.debug("RegistrationService: executing findById method. Id={}", id);
        return mapper.toRegistrationResponseDto(findRegistrationOrThrow(id));
    }

    @Override
    public List<RegistrationResponseDto> findAllByEventId(int page, int size, Long eventId) {
        log.debug("RegistrationService: executing findAllByEventId method. Page={}, size={}, eventId={}",
                page, size, eventId);

        Page<Registration> registrations = registrationRepository.findAllByEventId(eventId, PageRequest.of(page, size));

        return mapper.toRegistrationResponseDtoList(registrations);
    }

    @Override
    public void delete(DeleteRegistrationDto deleteDto) {
        log.info("RegistrationService: executing delete method. Deleting registration id={}",
                deleteDto.id());
        Registration registration = findRegistrationOrThrow(deleteDto.id());
        checkPasswordOrThrow(registration.getPassword(), deleteDto.password());
        registrationRepository.deleteById(deleteDto.id());
    }

    private void doTheUpdate(Registration registration, UpdateRegistrationDto updateDto) {
        log.debug("RegistrationService: executing doTheUpdate method for registration {}, updateDto {}", registration,
                updateDto);
        if (updateDto.username() != null) {
            registration.setUsername(updateDto.username());
        }
        if (updateDto.email() != null) {
            registration.setEmail(updateDto.email());
        }
        if (updateDto.phone() != null) {
            registration.setPhone(updateDto.phone());
        }
    }

    private void checkPasswordOrThrow(String registrationPassword, String dtoPassword) {
        if (!registrationPassword.equals(dtoPassword)) {
            throw new PasswordIncorrectException(String.format("Password %s is not correct", dtoPassword));
        }
    }

    private Registration findRegistrationOrThrow(Long registrationId) {
        return registrationRepository.findById(registrationId).orElseThrow(() -> new NotFoundException(String.format(
                "Registration with id=%d was not found", registrationId)));
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        int password = random.nextInt(1000, 10000);
        return Integer.toString(password);
    }
}
