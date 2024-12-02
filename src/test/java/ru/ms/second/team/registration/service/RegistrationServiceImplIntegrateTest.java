package ru.ms.second.team.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ms.second.team.registration.dto.event.EventDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.RegistrationCredentials;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationCount;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.service.impl.RegistrationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.ms.second.team.registration.model.RegistrationStatus.APPROVED;
import static ru.ms.second.team.registration.model.RegistrationStatus.DECLINED;
import static ru.ms.second.team.registration.model.RegistrationStatus.PENDING;
import static ru.ms.second.team.registration.model.RegistrationStatus.WAITING;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "app.event-service.url=localhost:${wiremock.server.port}"
})
public class RegistrationServiceImplIntegrateTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    RegistrationServiceImpl registrationService;

    private Long userId;

    private ObjectMapper objectMapper;

    @BeforeEach
    void init() {
        userId = 5L;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @Test
    void createRegistration() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto result = registrationService.create(registrationDto);

        assertNotNull(result.id(), "id can't be null");
        assertEquals(4, result.password().length());
    }

    @Test
    void updateRegistrationUsernameSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        UpdateRegistrationDto updateUsername = createUpdateRegistrationDto(
                "user2", null, null, registration.id(), registration.password());

        UpdatedRegistrationResponseDto usernameUpdated = registrationService.update(updateUsername);

        assertEquals(updateUsername.username(), usernameUpdated.username(), "usernames must be th same");
        assertEquals(registrationDto.email(), usernameUpdated.email(), "emails must be the same");
        assertEquals(registrationDto.phone(), usernameUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), usernameUpdated.id(), "ids must be the same");
    }

    @Test
    void updateRegistrationEmailSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        UpdateRegistrationDto updateEmail = createUpdateRegistrationDto(
                null, "mail@gmail.com", null, registration.id(), registration.password());

        UpdatedRegistrationResponseDto emailUpdated = registrationService.update(updateEmail);

        assertEquals(registrationDto.username(), emailUpdated.username(), "usernames must be the same");
        assertEquals(updateEmail.email(), emailUpdated.email(), "emails must be the same");
        assertEquals(registrationDto.phone(), emailUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), emailUpdated.id(), "ids must be the same");
    }

    @Test
    void updateRegistrationPhoneSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        UpdateRegistrationDto updatePhone = createUpdateRegistrationDto(
                null, null, "78887776655", registration.id(), registration.password());

        UpdatedRegistrationResponseDto phoneUpdated = registrationService.update(updatePhone);

        assertEquals(registrationDto.username(), phoneUpdated.username(), "usernames must be the same");
        assertEquals(registrationDto.email(), phoneUpdated.email(), "emails must be the same");
        assertEquals(updatePhone.phone(), phoneUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), phoneUpdated.id(), "ids must be the same");
    }

    @Test
    void updateRegistrationFailIncorrectPassword() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        UpdateRegistrationDto failPasswordUpdate = createUpdateRegistrationDto(
                "this gonna fail", null, null, registration.id(), "fake");

        assertThrows(PasswordIncorrectException.class, () -> registrationService.update(failPasswordUpdate));
    }

    @Test
    void updateRegistrationFailNotFound() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        UpdateRegistrationDto notFoundObject = createUpdateRegistrationDto(
                "this gonna fail", null, null, registration.id() + 1, registration.password());

        assertThrows(NotFoundException.class, () -> registrationService.update(notFoundObject));
    }

    @Test
    void findRegistrationById() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        registrationService.create(registrationDto);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78885553535", 1L);

        CreatedRegistrationResponseDto registration2 = registrationService.create(registrationDto2);

        RegistrationResponseDto retrievedRegistration = registrationService.findById(registration2.id());

        assertEquals(registrationDto2.username(), retrievedRegistration.username(), "usernames must be the same");
        assertEquals(registrationDto2.email(), retrievedRegistration.email(), "emails must be the same");
        assertEquals(registrationDto2.phone(), retrievedRegistration.phone(), "Phone numbers must be the same");

        assertThrows(NotFoundException.class, () -> registrationService.findById(registration2.id() + 1),
                "must throw not found exception if object not found");
    }

    @Test
    void findRegistrationsByEventIdSuccessWhenEmpty() {
        List<RegistrationResponseDto> emptyList =
                registrationService.findAllByEventId(0, 10, 9999999999999L);

        assertEquals(0, emptyList.size(), "List must be empty");
    }

    @Test
    void findRegistrationsByEventIdOneRegistrationOnly() {
        /*
        Retrieves all registrations for event. Only one registration exists
         */
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 2L);

        registrationService.create(registrationDto);

        List<RegistrationResponseDto> oneRegistrationList =
                registrationService.findAllByEventId(0, 10, 2L);

        assertEquals(1, oneRegistrationList.size(),
                "There is only 1 registration for that event");

        assertEquals(registrationDto.username(), oneRegistrationList.get(0).username(), "username must be the same");
        assertEquals(registrationDto.email(), oneRegistrationList.get(0).email(), "email must be the same");
        assertEquals(registrationDto.phone(), oneRegistrationList.get(0).phone(), "phone must be the same");
    }

    @Test
    void findRegistrationsByEventIdTwoRegistrations() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 2L);

        registrationService.create(registrationDto);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78885553535", 2L);

        registrationService.create(registrationDto2);

        List<RegistrationResponseDto> registrationsList =
                registrationService.findAllByEventId(0, 10, 2L);

        assertEquals(2, registrationsList.size(), "There are only 2 registrations for that event");
    }

    @Test
    void deleteNonExistingRegistrationByIdFail() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        RegistrationCredentials registrationNotExistDeleteDto =
                createRegistrationCredentials(registration.id() + 1, registration.password());

        assertThrows(NotFoundException.class, () -> registrationService.delete(registrationNotExistDeleteDto));
    }

    @Test
    void deleteRegistrationByIdFailWrongPassword() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        RegistrationCredentials wrongPasswordDeleteDto =
                createRegistrationCredentials(registration.id(), "fail");

        assertThrows(PasswordIncorrectException.class, () -> registrationService.delete(wrongPasswordDeleteDto));
    }

    @Test
    void deleteRegistrationByIdSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        RegistrationCredentials deleteDto =
                createRegistrationCredentials(registration.id(), registration.password());

        registrationService.delete(deleteDto);

        assertThrows(NotFoundException.class, () -> registrationService.findById(registration.id()));
    }

    @Test
    void createRegistration_whenCreated_statusShouldBePending() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);

        RegistrationResponseDto result = registrationService.findById(createdRegistration.id());

        assertEquals(PENDING, result.status());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, set APPROVE, no participant limit")
    void updateRegistrationStatus_whenValidPasswordAndRegistrationFound_shouldUpdateStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus newStatus = APPROVED;

        RegistrationStatus updatedStatus = registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                newStatus, credentials);

        assertEquals(newStatus, updatedStatus);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, set APPROVE, limit exceeded by one")
    void updateRegistrationStatus_whenParticipantLimitExceededByOne_shouldSetStatusToWaiting() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus approved = APPROVED;
        registrationService.updateRegistrationStatus(userId, createdRegistration.id(), approved, credentials);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        RegistrationCredentials credentials2 = createRegistrationCredentials(createdRegistration2.id(),
                createdRegistration2.password());

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus result = registrationService.updateRegistrationStatus(userId, createdRegistration2.id(),
                approved, credentials2);

        assertEquals(WAITING, result);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, set APPROVE, limit exceeded by two")
    void updateRegistrationStatus_whenParticipantLimitExceededByTwo_shouldSetStatusToWaiting() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus approved = APPROVED;
        registrationService.updateRegistrationStatus(userId, createdRegistration.id(), approved, credentials);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        RegistrationCredentials credentials2 = createRegistrationCredentials(createdRegistration2.id(),
                createdRegistration2.password());
        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(),
                approved, credentials2);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail2@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.create(registrationDto3);
        RegistrationCredentials credentials3 = createRegistrationCredentials(createdRegistration3.id(),
                createdRegistration3.password());

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus result = registrationService.updateRegistrationStatus(userId, createdRegistration3.id(),
                approved, credentials3);

        assertEquals(WAITING, result);
        assertEquals(WAITING, registrationService.findById(createdRegistration2.id()).status());
        assertEquals(APPROVED, registrationService.findById(createdRegistration.id()).status());
    }


    @SneakyThrows
    @Test
    void updateRegistrationStatus_whenEventNotFound_shouldThrowNotFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        RegistrationStatus newStatus = APPROVED;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(), newStatus,
                        credentials));

        assertEquals("Event was not found", ex.getLocalizedMessage());
    }

    @Test
    void updateRegistrationStatus_whenInvalidPassword_shouldThrowPasswordIncorrectException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        String incorrectPassword = "6666";
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), incorrectPassword);

        RegistrationStatus newStatus = APPROVED;

        PasswordIncorrectException ex = assertThrows(PasswordIncorrectException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(), newStatus, credentials));

        assertEquals("Password=" + incorrectPassword + " for registrationId=" +
                createdRegistration.id() + " is not correct", ex.getLocalizedMessage());
    }

    @Test
    void updateRegistrationStatus_whenRegistrationNotFound_shouldThrowNotFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        Long unknownId = 999L;
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        RegistrationStatus newStatus = APPROVED;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> registrationService.updateRegistrationStatus(userId, unknownId, newStatus, credentials));

        assertEquals("Registration with id=" + unknownId + " was not found", ex.getLocalizedMessage());
    }

    @Test
    void declineRegistration_whenValidPasswordAndRegistrationFound_shouldSetDeclineStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        RegistrationStatus updatedStatus = registrationService.declineRegistration(userId, createdRegistration.id(),
                reason, credentials);

        assertEquals(DECLINED, updatedStatus);
    }

    @Test
    void declineRegistration_whenInvalidPassword_shouldThrowPasswordIncorrectException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        String incorrectPassword = "6666";
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), incorrectPassword);
        String reason = "reason";

        PasswordIncorrectException ex = assertThrows(PasswordIncorrectException.class,
                () -> registrationService.declineRegistration(userId, createdRegistration.id(), reason, credentials));

        assertEquals("Password=" + incorrectPassword + " for registrationId=" +
                createdRegistration.id() + " is not correct", ex.getLocalizedMessage());
    }

    @Test
    void declineRegistration_whenNotFound_shouldThrowNofFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration = registrationService.create(registrationDto);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";
        Long unknownId = 999L;


        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> registrationService.declineRegistration(userId, unknownId, reason, credentials));

        assertEquals("Registration with id=" + unknownId + " was not found", ex.getLocalizedMessage());
    }

    @Test
    @SneakyThrows
    void searchRegistrations_whenSearchByMultipleStatuses_shouldReturnRegistrationWithTheseStatuses() {
        NewRegistrationDto registrationDto1 =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration1 = registrationService.create(registrationDto1);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.create(registrationDto3);

        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto1.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration3.id(), WAITING,
                new RegistrationCredentials(createdRegistration3.id(), createdRegistration3.password()));

        List<RegistrationResponseDto> result = registrationService
                .searchRegistrations(List.of(PENDING, WAITING), registrationDto1.eventId());

        assertEquals(2, result.size());
        assertEquals(PENDING, result.get(0).status());
        assertEquals(WAITING, result.get(1).status());
    }

    @Test
    @SneakyThrows
    void searchRegistrations_whenSearchBySingleStatus_shouldReturnRegistrationWithThisStatus() {
        NewRegistrationDto registrationDto1 =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration1 = registrationService.create(registrationDto1);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.create(registrationDto3);

        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto3.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration3.id(), WAITING,
                new RegistrationCredentials(createdRegistration3.id(), createdRegistration3.password()));

        List<RegistrationResponseDto> result = registrationService
                .searchRegistrations(List.of(APPROVED), registrationDto1.eventId());

        assertEquals(1, result.size());
        assertEquals(APPROVED, result.get(0).status());
    }

    @Test
    @SneakyThrows
    void searchRegistrations_whenSearchByMultipleStatusesWithDifferentEventIds_shouldReturnRegistrationWithTheseStatuses() {
        NewRegistrationDto registrationDto1 =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration1 = registrationService.create(registrationDto1);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 2L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.create(registrationDto3);

        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto3.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration3.id(), WAITING,
                new RegistrationCredentials(createdRegistration3.id(), createdRegistration3.password()));

        List<RegistrationResponseDto> result = registrationService
                .searchRegistrations(List.of(PENDING, WAITING), registrationDto1.eventId());

        assertEquals(1, result.size());
        assertEquals(PENDING, result.get(0).status());
    }

    @Test
    void searchRegistrations_whenNoRegistrationsExist_shouldReturnEmptyList() {
        List<RegistrationResponseDto> result = registrationService
                .searchRegistrations(List.of(PENDING, WAITING), 1L);

        assertEquals(0, result.size());
    }

    @Test
    @SneakyThrows
    void getRegistrationsCountByEventId_whenRegistrationsExists_shouldReturnRegistrationsCount() {
        NewRegistrationDto registrationDto1 =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration1 = registrationService.create(registrationDto1);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.create(registrationDto2);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.create(registrationDto3);
        NewRegistrationDto registrationDto4 =
                createNewRegistrationDto("user4", "mail@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration4 = registrationService.create(registrationDto4);

        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto3.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration3.id(), WAITING,
                new RegistrationCredentials(createdRegistration3.id(), createdRegistration3.password()));
        registrationService.findAllByEventId(0, 10, registrationDto1.eventId());

        RegistrationCount count = registrationService.getRegistrationsCountByEventId(registrationDto1.eventId());

        assertEquals(2, count.numberOfPendingRegistrations());
        assertEquals(1, count.numberOfWaitingRegistrations());
        assertEquals(1, count.numberOfApprovedRegistrations());
        assertEquals(0, count.numberOfDeclinedRegistrations());
    }

    private NewRegistrationDto createNewRegistrationDto(String username, String email, String phone, Long eventId) {
        return NewRegistrationDto.builder()
                .email(email)
                .eventId(eventId)
                .phone(phone)
                .username(username)
                .build();
    }

    private UpdateRegistrationDto createUpdateRegistrationDto(
            String username, String email, String phone, Long id, String password) {
        return UpdateRegistrationDto.builder()
                .id(id)
                .password(password)
                .username(username)
                .email(email)
                .phone(phone)
                .build();
    }

    private RegistrationCredentials createRegistrationCredentials(Long id, String password) {
        return RegistrationCredentials.builder()
                .id(id)
                .password(password).build();
    }

    private EventDto createEvent(long ownerId, int participantLimit) {
        return EventDto.builder()
                .id(1L)
                .name("event name " + ownerId)
                .description("event description " + ownerId)
                .ownerId(ownerId)
                .startDateTime(LocalDateTime.now().plusDays(ownerId))
                .endDateTime(LocalDateTime.now().plusMonths(ownerId))
                .participantLimit(participantLimit)
                .build();
    }
}
