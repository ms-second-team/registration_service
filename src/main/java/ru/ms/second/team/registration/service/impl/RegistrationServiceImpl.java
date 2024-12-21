package ru.ms.second.team.registration.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ms.second.team.registration.client.event.EventClient;
import ru.ms.second.team.registration.client.user.UserClient;
import ru.ms.second.team.registration.dto.event.EventDto;
import ru.ms.second.team.registration.dto.event.TeamMemberDto;
import ru.ms.second.team.registration.dto.event.TeamMemberRole;
import ru.ms.second.team.registration.dto.registration.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.registration.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.registration.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.registration.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.response.RegistrationCount;
import ru.ms.second.team.registration.dto.registration.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.user.NewUserRequest;
import ru.ms.second.team.registration.dto.user.UserCredentials;
import ru.ms.second.team.registration.dto.user.UserDto;
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

import java.util.Arrays;
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
    private final UserClient userClient;

    @Override
    @Transactional
    public CreatedRegistrationResponseDto createRegistration(NewRegistrationDto creationDto) {
        log.info("RegistrationService: executing createRegistration method. Username {}, email {}, phone {}, eventId {}",
                creationDto.username(), creationDto.email(), creationDto.phone(), creationDto.eventId());
        UserDto author;
        String password;
        if (creationDto.userPassword() != null) {
            password = creationDto.userPassword();
            author = findUserByEmail(creationDto.email(), password);
        } else {
            password = createPassword();
            NewUserRequest newUserRequest = generateNewUserRequest(creationDto, password);
            author = userClient.createUser(newUserRequest);
        }
        findEventOrThrow(author.id(), creationDto.eventId());
        Registration registration = registrationMapper.toModel(creationDto);
        registration.setPassword(password);
        registration.setAuthorId(author.id());
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
        userClient.deleteUser(registration.getAuthorId(), registrationCredentials.password());
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
        if (teamMemberDtoList == null) {
            throw new NotFoundException(String.format("Teams for event with id=%d were not found", eventId));
        }
        return teamMemberDtoList.stream()
                .anyMatch(tm -> tm.userId().equals(userId) && tm.role().equals(TeamMemberRole.MANAGER));
    }

    private NewUserRequest generateNewUserRequest(NewRegistrationDto newRegistrationDto, String password) {
        return NewUserRequest.builder()
                .name(newRegistrationDto.username())
                .email(newRegistrationDto.email())
                .password(password)
                .build();
    }

    private String createPassword() {
        CharacterRule specialCharacterRule = new CharacterRule(new CharacterData() {
            @Override
            public String getErrorCode() {
                return "Error occurred while generating password special char";
            }

            @Override
            public String getCharacters() {
                return "!@#$%^&*()-+_";
            }
        });

        List<CharacterRule> rules = Arrays.asList(
                new CharacterRule(EnglishCharacterData.LowerCase),
                new CharacterRule(EnglishCharacterData.Digit),
                new CharacterRule(EnglishCharacterData.UpperCase),
                specialCharacterRule
        );
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        return passwordGenerator.generatePassword(8, rules);
    }

    private UserDto findUserByEmail(String email, String password) {
        UserCredentials credentials = UserCredentials.builder()
                .email(email)
                .password(password)
                .build();
        return userClient.findUserByEmail(credentials);
    }
}
