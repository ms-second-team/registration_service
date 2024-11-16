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

    @Transactional
    @Override
    public CreatedRegistrationResponseDto create(NewRegistrationDto creationDto) {
        log.info("RegistrationService: executing create method. Username {}, email {}, phone {}, eventId {}",
                creationDto.getUsername(), creationDto.getEmail(), creationDto.getPhone(), creationDto.getEventId());
        Registration registration = RegistrationMapper.toRegistration(creationDto, generatePassword());
        return RegistrationMapper.toCreatedDto(registrationRepository.save(registration));
    }

    @Transactional
    @Override
    public UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing update method. Updating registration with id {}", updateDto.getId());
        Registration registration = findRegistrationOrThrow(updateDto.getId());
        checkPasswordOrThrow(registration.getPassword(), updateDto.getPassword());
        return RegistrationMapper.toUpdatedDto(registrationRepository.save(doTheUpdate(registration, updateDto)));
    }

    @Override
    public RegistrationResponseDto findById(Long id) {
        log.info("RegistrationService: executing findById method. Id={}", id);
        return RegistrationMapper.toRegistrationResponseDto(findRegistrationOrThrow(id));
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
                .map(RegistrationMapper::toRegistrationResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(DeleteRegistrationDto deleteDto) {
        log.info("RegistrationService: executing delete method. Deleting registration id={}",
                deleteDto.getId());
        Registration registration = findRegistrationOrThrow(deleteDto.getId());
        checkPasswordOrThrow(registration.getPassword(), deleteDto.getPassword());
        registrationRepository.deleteById(deleteDto.getId());
    }

    private Registration doTheUpdate(Registration registration, UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing doTheUpdate method");
        if (updateDto.getUsername() != null) {
            registration.setUsername(updateDto.getUsername());
        }
        if (updateDto.getEmail() != null) {
            registration.setEmail(updateDto.getEmail());
        }
        if (updateDto.getPhone() != null) {
            registration.setPhone(updateDto.getPhone());
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
