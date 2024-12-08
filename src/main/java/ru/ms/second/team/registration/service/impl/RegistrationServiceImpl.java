package ru.ms.second.team.registration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ms.second.team.registration.client.EventClient;
import ru.ms.second.team.registration.dto.event.EventDto;
import ru.ms.second.team.registration.dto.event.TeamMemberDto;
import ru.ms.second.team.registration.dto.event.TeamMemberRole;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationCount;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotAuthorizedException;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.mapper.RegistrationMapper;
import ru.ms.second.team.registration.model.DeclinedRegistration;
import ru.ms.second.team.registration.model.Registration;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.repository.jpa.DeclinedRegistrationRepository;
import ru.ms.second.team.registration.repository.jpa.JpaRegistrationRepository;
import ru.ms.second.team.registration.service.RegistrationService;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import static ru.ms.second.team.registration.model.RegistrationStatus.APPROVED;
import static ru.ms.second.team.registration.model.RegistrationStatus.DECLINED;
import static ru.ms.second.team.registration.model.RegistrationStatus.PENDING;
import static ru.ms.second.team.registration.model.RegistrationStatus.WAITING;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final JpaRegistrationRepository registrationRepository;
    private final DeclinedRegistrationRepository declinedRegistrationRepository;
    private final RegistrationMapper registrationMapper;
    private final EventClient eventClient;

    @Override
    public CreatedRegistrationResponseDto createRegistration(NewRegistrationDto creationDto, Long userId) {
        log.info("RegistrationService: executing createRegistration method. Username {}, email {}, phone {}, eventId {}",
                creationDto.username(), creationDto.email(), creationDto.phone(), creationDto.eventId());

        findEventOrThrow(userId, creationDto.eventId());
        Registration registration = registrationMapper.toModel(creationDto);
        registration.setPassword(generatePassword());
        registration = registrationRepository.save(registration);

        return registrationMapper.toCreatedDto(registration);
    }

    @Override
    public UpdatedRegistrationResponseDto updateRegistration(UpdateRegistrationDto updateDto) {
        log.info("RegistrationService: executing updateRegistration method. Updating registration with id {}, updateDto {}",
                updateDto.id(), updateDto);

        Registration registration = findRegistrationOrThrow(updateDto.id());
        checkPasswordOrThrow(registration.getPassword(), updateDto.password(), updateDto.id());
        registrationMapper.updateRegistration(updateDto, registration);
        registration = registrationRepository.save(registration);
        return registrationMapper.toUpdatedDto(registration);
    }

    @Override
    public RegistrationResponseDto findRegistrationById(Long id) {
        log.debug("RegistrationService: executing findRegistrationById method. Id={}", id);
        Registration registration = findRegistrationOrThrow(id);
        return registrationMapper.toRegistrationDto(registration);
    }

    @Override
    public List<RegistrationResponseDto> findAllRegistrationsByEventId(int page, int size, Long eventId) {
        log.debug("RegistrationService: executing findAllRegistrationsByEventId method. Page={}, size={}, eventId={}",
                page, size, eventId);

        Page<Registration> registrations = registrationRepository.findAllByEventId(eventId, PageRequest.of(page, size));

        return registrationMapper.toRegistraionDtoList(registrations.getContent());
    }

    @Override
    @Transactional
    public void deleteRegistration(RegistrationCredentials registrationCredentials) {
        log.info("RegistrationService: executing deleteRegistration method. Deleting registration id={}",
                registrationCredentials.id());
        Registration registration = findRegistrationOrThrow(registrationCredentials.id());
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());
        registrationRepository.deleteById(registrationCredentials.id());
        declinedRegistrationRepository.deleteAllByRegistrationId(registrationCredentials.id());
        updateStatusOfClosestWaitingRegistration(registration);
    }

    @Override
    @Transactional
    public RegistrationStatus updateRegistrationStatus(Long userId, Long registrationId, RegistrationStatus newStatus,
                                                       RegistrationCredentials registrationCredentials) {
        final Registration registration = findRegistrationOrThrow(registrationId);
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());

        if (!checkIfUserIsOwnerOrManagerOfEvent(userId, registration.getEventId())) {
            throw new NotAuthorizedException(String.format(
                    "User id=%d has no rights to change registration status for event id=%d",
                    userId, registration.getEventId()));
        }

        registration.setStatus(newStatus);
        if (newStatus.equals(APPROVED)) {
            checkEventParticipationLimit(userId, registration, newStatus);
        }
        final Registration updatedRegistration = registrationRepository.save(registration);
        log.info("New status '{}' for registration with id '{}'", newStatus, registrationId);
        return updatedRegistration.getStatus();
    }

    @Override
    public RegistrationStatus declineRegistration(Long userId, Long registrationId, String reason,
                                                  RegistrationCredentials registrationCredentials) {
        final Registration registration = findRegistrationOrThrow(registrationId);
        checkPasswordOrThrow(registration.getPassword(), registrationCredentials.password(), registrationCredentials.id());

        if (!checkIfUserIsOwnerOrManagerOfEvent(userId, registration.getEventId())) {
            throw new NotAuthorizedException(String.format(
                    "User id=%d has no rights to change registration status for event id=%d",
                    userId, registration.getEventId()));
        }

        registration.setStatus(DECLINED);
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
        Map<String, Long> statusToNumberOfRegistrations = registrationRepository.getStatusToNumberOfRegistrationsForEvent(eventId);
        final RegistrationCount registrationCount = convertMapToRegistrationsCount(statusToNumberOfRegistrations);
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
        if (registration.getStatus().equals(APPROVED)) {
            Registration closestRegistration = registrationRepository.findEarliestWaitingRegistration();
            closestRegistration.setStatus(PENDING);
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

    private RegistrationCount convertMapToRegistrationsCount(Map<String, Long> statusToRegistrationsCount) {
        return RegistrationCount.builder()
                .numberOfPendingRegistrations(statusToRegistrationsCount
                        .getOrDefault(PENDING.name(), 0L))
                .numberOfApprovedRegistrations(statusToRegistrationsCount
                        .getOrDefault(APPROVED.name(), 0L))
                .numberOfWaitingRegistrations(statusToRegistrationsCount
                        .getOrDefault(WAITING.name(), 0L))
                .numberOfDeclinedRegistrations(statusToRegistrationsCount
                        .getOrDefault(DECLINED.name(), 0L))
                .build();
    }

    private void checkEventParticipationLimit(Long userId, Registration registration, RegistrationStatus newStatus) {
        final EventDto event = eventClient.getEventById(userId, registration.getEventId()).getBody();
        final int eventParticipantLimit = event.participantLimit();
        final List<Registration> approvedRegistrations = registrationRepository.searchRegistrations(List.of(APPROVED),
                registration.getEventId());
        if (approvedRegistrations.size() > eventParticipantLimit && eventParticipantLimit != 0) {
            approvedRegistrations.stream()
                    .skip(eventParticipantLimit)
                    .forEach(reg -> reg.setStatus(WAITING));
            registrationRepository.saveAll(approvedRegistrations);
            registration.setStatus(WAITING);
        }
    }

    private EventDto findEventOrThrow(Long userId, Long eventId) {
        return eventClient.getEventById(userId, eventId).getBody();
    }

    private boolean checkIfUserIsOwnerOrManagerOfEvent(Long userId, Long eventId) {
        final EventDto event = findEventOrThrow(userId, eventId);
        if (event.ownerId().equals(userId)) return true;
        List<TeamMemberDto> teamMemberDtoList = eventClient.getTeamsByEventId(userId, eventId).getBody();
        return teamMemberDtoList.stream()
                .filter(t -> t.role().equals(TeamMemberRole.MANAGER))
                .map(TeamMemberDto::userId)
                .anyMatch(id -> id.equals(userId));
    }
}
