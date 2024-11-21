package ru.ms.second.team.registration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationCount;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.mapper.RegistrationMapper;
import ru.ms.second.team.registration.model.DeclinedRegistration;
import ru.ms.second.team.registration.model.Registration;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.repository.DeclinedRegistrationRepository;
import ru.ms.second.team.registration.repository.JpaRegistrationRepository;
import ru.ms.second.team.registration.service.RegistrationService;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final JpaRegistrationRepository registrationRepository;
    private final DeclinedRegistrationRepository declinedRegistrationRepository;
    private final RegistrationMapper registrationMapper;

    @Override
    public CreatedRegistrationResponseDto create(NewRegistrationDto creationDto) {
        log.info("RegistrationService: executing create method. Username {}, email {}, phone {}, eventId {}",
                creationDto.username(), creationDto.email(), creationDto.phone(), creationDto.eventId());

        Registration registration = registrationMapper.toModel(creationDto);
        registration.setPassword(generatePassword());
        registration = registrationRepository.save(registration);

        return registrationMapper.toCreatedDto(registration);
    }

    @Override
    public UpdatedRegistrationResponseDto update(UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing update method. Updating registration with id {}, updateDto {}",
                updateDto.id(), updateDto);

        Registration registration = findRegistrationOrThrow(updateDto.id());
        checkPasswordOrThrow(registration.getPassword(), updateDto.password(), updateDto.id());
        registrationMapper.updateRegistration(updateDto, registration);
        registration = registrationRepository.save(registration);
        return registrationMapper.toUpdatedDto(registration);
    }

    @Override
    public RegistrationResponseDto findById(Long id) {
        log.debug("RegistrationService: executing findById method. Id={}", id);
        Registration registration = findRegistrationOrThrow(id);
        return registrationMapper.toRegistrationDto(registration);
    }

    @Override
    public List<RegistrationResponseDto> findAllByEventId(int page, int size, Long eventId) {
        log.debug("RegistrationService: executing findAllByEventId method. Page={}, size={}, eventId={}",
                page, size, eventId);

        Page<Registration> registrations = registrationRepository.findAllByEventId(eventId, PageRequest.of(page, size));

        return registrationMapper.toRegistraionDtoList(registrations.getContent());
    }

    @Override
    @Transactional
    public void delete(RegistrationCredentials registrationCredentials) {
        log.info("RegistrationService: executing delete method. Deleting registration id={}",
                registrationCredentials.id());
        Registration registration = findRegistrationOrThrow(registrationCredentials.id());
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());
        registrationRepository.deleteById(registrationCredentials.id());
        declinedRegistrationRepository.deleteAllByRegistrationId(registrationCredentials.id());
        updateStatusOfClosestWaitingRegistration(registration);
    }

    @Override
    public RegistrationStatus updateRegistrationStatus(Long registrationId, RegistrationStatus newStatus,
                                                       RegistrationCredentials registrationCredentials) {
        final Registration registration = findRegistrationOrThrow(registrationId);
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());
        registration.setStatus(newStatus);
        final Registration updatedRegistration = registrationRepository.save(registration);
        log.debug("New status '{}' for registration with id '{}'", newStatus, registrationId);
        return updatedRegistration.getStatus();
    }

    @Override
    public RegistrationStatus declineRegistration(Long registrationId, String reason,
                                                  RegistrationCredentials registrationCredentials) {
        final Registration registration = findRegistrationOrThrow(registrationId);
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());
        registration.setStatus(RegistrationStatus.DECLINED);
        final Registration updatedRegistration = registrationRepository.save(registration);
        saveDeclineReason(reason, updatedRegistration);
        log.debug("Registration with id '{}' was declined. Reason: {}", registrationId, reason);
        return updatedRegistration.getStatus();
    }

    @Override
    public List<RegistrationResponseDto> searchRegistrations(List<RegistrationStatus> statuses, Long eventId) {
        final List<Registration> registrations = registrationRepository.searchRegistrations(statuses, eventId);
        log.debug("Found '{}' registrations for event with id '{}' and statuses in '{}'", registrations.size(),
                eventId, statuses);
        return registrationMapper.toRegistraionDtoList(registrations);
    }

    @Override
    public RegistrationCount getRegistrationsCountByEventId(Long eventId) {
        final Map<RegistrationStatus, Long> statusToRegistrationsCount = new HashMap<>();
        for (RegistrationStatus status : RegistrationStatus.values()) {
            long numberOfRegistrations = registrationRepository.getRegistrationsCountByStatusAndEventId(status, eventId);
            statusToRegistrationsCount.put(status, numberOfRegistrations);
        }
        final RegistrationCount registrationCount = convertMapToRegistrationsCount(statusToRegistrationsCount);
        log.debug("Retrieved registrations count for event with id '{}'", eventId);
        return registrationCount;
    }

    private void checkPasswordOrThrow(String registrationPassword, String dtoPassword, Long registrationId) {
        if (!registrationPassword.equals(dtoPassword)) {
            throw new PasswordIncorrectException(String.format(
                    "Password=%s for registrationId=%d is not correct", dtoPassword, registrationId));
        }
    }

    private Registration findRegistrationOrThrow(Long registrationId) {
        return registrationRepository.findById(registrationId).orElseThrow(() -> new NotFoundException(String.format(
                "Registration with id=%d was not found", registrationId)));
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        return String.format("%04d", random.nextInt(10000));
    }

    private void updateStatusOfClosestWaitingRegistration(Registration registration) {
        if (registration.getStatus().equals(RegistrationStatus.APPROVED)) {
            Registration closestRegistration = registrationRepository.findEarliestWaitingRegistration();
            closestRegistration.setStatus(RegistrationStatus.PENDING);
            registrationRepository.save(closestRegistration);
        }
    }

    private void saveDeclineReason(String reason, Registration updatedRegistration) {
        DeclinedRegistration declinedRegistration = DeclinedRegistration.builder()
                .registration(updatedRegistration)
                .reason(reason)
                .build();
        declinedRegistrationRepository.save(declinedRegistration);
    }

    private RegistrationCount convertMapToRegistrationsCount(Map<RegistrationStatus, Long> statusToRegistrationsCount) {
        return RegistrationCount.builder()
                .numberOfPendingRegistrations(statusToRegistrationsCount.get(RegistrationStatus.PENDING))
                .numberOfApprovedRegistrations(statusToRegistrationsCount.get(RegistrationStatus.APPROVED))
                .numberOfWaitingRegistrations(statusToRegistrationsCount.get(RegistrationStatus.WAITING))
                .numberOfDeclinedRegistrations(statusToRegistrationsCount.get(RegistrationStatus.DECLINED))
                .build();
    }
}
