package ru.ms.second.team.registration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationServiceImpl implements RegistrationService {
    private final JpaRegistrationRepository registrationRepository;
    private final RegistrationMapper mapper;

    @Transactional
    @Override
    public CreatedRegistrationResponseDto create(NewRegistrationDto creationDto) {
        log.info("RegistrationService: executing create method. Username {}, email {}, phone {}, eventId {}",
                creationDto.username(), creationDto.email(), creationDto.phone(), creationDto.eventId());
        Registration registration = mapper.toRegistration(creationDto, generatePassword());
        return mapper.toCreatedDto(registrationRepository.save(registration));
    }

    @Transactional
    @Override
    public UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing update method. Updating registration with id {}", updateDto.id());
        Registration registration = findRegistrationOrThrow(updateDto.id());
        checkPasswordOrThrow(registration.getPassword(), updateDto.password());
        return mapper.toUpdatedDto(registrationRepository.save(doTheUpdate(registration, updateDto)));
    }

    @Override
    public RegistrationResponseDto findById(Long id) {
        log.info("RegistrationService: executing findById method. Id={}", id);
        return mapper.toRegistrationResponseDto(findRegistrationOrThrow(id));
    }

    @Override
    public List<RegistrationResponseDto> findAllByEventId(int page, int size, Long eventId) {
        log.info("RegistrationService: executing findAllByEventId method. Page={}, size={}, eventId={}",
                page, size, eventId);
        Page<Registration> registrations = registrationRepository.findAllByEventId(eventId, PageRequest.of(page, size));

        if (!registrations.hasContent()) {
            return new ArrayList<>();
        }

        return registrations.getContent().stream()
                .map(mapper::toRegistrationResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(DeleteRegistrationDto deleteDto) {
        log.info("RegistrationService: executing delete method. Deleting registration id={}",
                deleteDto.id());
        Registration registration = findRegistrationOrThrow(deleteDto.id());
        checkPasswordOrThrow(registration.getPassword(), deleteDto.password());
        registrationRepository.deleteById(deleteDto.id());
    }

    private Registration doTheUpdate(Registration registration, UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing doTheUpdate method");
        if (updateDto.username() != null) {
            registration.setUsername(updateDto.username());
        }
        if (updateDto.email() != null) {
            registration.setEmail(updateDto.email());
        }
        if (updateDto.phone() != null) {
            registration.setPhone(updateDto.phone());
        }
        return registration;
    }

    private void checkPasswordOrThrow(String registrationPassword, String dtoPassword) {
        log.info("RegistrationService: checking if the password is correct");
        if (!registrationPassword.equals(dtoPassword)) {
            throw new PasswordIncorrectException(String.format("Password %s is not correct", dtoPassword));
        }
    }

    private Registration findRegistrationOrThrow(Long registrationId) {
        log.info("RegistrationService: Looking for registration with id {}", registrationId);
        return registrationRepository.findById(registrationId).orElseThrow(() -> new NotFoundException(String.format(
                "Registration with id=%d was not found", registrationId)));
    }

    private String generatePassword() {
        log.info("RegistrationService: executing method generatePassword");
        StringBuilder sb = new StringBuilder();
        String chars = "0123456789";
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
