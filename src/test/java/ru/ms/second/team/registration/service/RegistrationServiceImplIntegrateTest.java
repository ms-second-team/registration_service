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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.ms.second.team.registration.dto.event.EventDto;
import ru.ms.second.team.registration.dto.event.TeamMemberDto;
import ru.ms.second.team.registration.dto.event.TeamMemberRole;
import ru.ms.second.team.registration.dto.registration.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.NewRegistrationDto;
import ru.ms.second.team.registration.dto.registration.RegistrationCount;
import ru.ms.second.team.registration.dto.registration.RegistrationCredentials;
import ru.ms.second.team.registration.dto.registration.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.registration.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotAuthorizedException;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.service.impl.RegistrationServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

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
    @SneakyThrows
    void createRegistration() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto result = registrationService.createRegistration(registrationDto, 1L);

        assertNotNull(result.id(), "id can't be null");
        assertEquals(4, result.password().length());
    }

    @Test
    @SneakyThrows
    void createRegistrationWhenEventNotFound_ShouldThrowNotFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> registrationService.createRegistration(registrationDto, userId));

        assertEquals("Event was not found", ex.getMessage());
    }

    @Test
    @SneakyThrows
    void updateRegistrationUsernameSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        UpdateRegistrationDto updateUsername = createUpdateRegistrationDto(
                "user2", null, null, registration.id(), registration.password());

        UpdatedRegistrationResponseDto usernameUpdated = registrationService.updateRegistration(updateUsername);

        assertEquals(updateUsername.username(), usernameUpdated.username(), "usernames must be th same");
        assertEquals(registrationDto.email(), usernameUpdated.email(), "emails must be the same");
        assertEquals(registrationDto.phone(), usernameUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), usernameUpdated.id(), "ids must be the same");
    }

    @Test
    @SneakyThrows
    void updateRegistrationEmailSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        UpdateRegistrationDto updateEmail = createUpdateRegistrationDto(
                null, "mail@gmail.com", null, registration.id(), registration.password());

        UpdatedRegistrationResponseDto emailUpdated = registrationService.updateRegistration(updateEmail);

        assertEquals(registrationDto.username(), emailUpdated.username(), "usernames must be the same");
        assertEquals(updateEmail.email(), emailUpdated.email(), "emails must be the same");
        assertEquals(registrationDto.phone(), emailUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), emailUpdated.id(), "ids must be the same");
    }

    @Test
    @SneakyThrows
    void updateRegistrationPhoneSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        UpdateRegistrationDto updatePhone = createUpdateRegistrationDto(
                null, null, "78887776655", registration.id(), registration.password());

        UpdatedRegistrationResponseDto phoneUpdated = registrationService.updateRegistration(updatePhone);

        assertEquals(registrationDto.username(), phoneUpdated.username(), "usernames must be the same");
        assertEquals(registrationDto.email(), phoneUpdated.email(), "emails must be the same");
        assertEquals(updatePhone.phone(), phoneUpdated.phone(), "Phone numbers must be the same");
        assertEquals(registration.id(), phoneUpdated.id(), "ids must be the same");
    }

    @Test
    @SneakyThrows
    void updateRegistrationFailIncorrectPassword() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        UpdateRegistrationDto failPasswordUpdate = createUpdateRegistrationDto(
                "this gonna fail", null, null, registration.id(), "fake");

        assertThrows(PasswordIncorrectException.class, () -> registrationService.updateRegistration(failPasswordUpdate));
    }

    @Test
    @SneakyThrows
    void updateRegistrationFailNotFound() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        UpdateRegistrationDto notFoundObject = createUpdateRegistrationDto(
                "this gonna fail", null, null, registration.id() + 1, registration.password());

        assertThrows(NotFoundException.class, () -> registrationService.updateRegistration(notFoundObject));
    }

    @Test
    @SneakyThrows
    void findRegistrationById() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));
        registrationService.createRegistration(registrationDto, 1L);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78885553535", 1L);

        CreatedRegistrationResponseDto registration2 = registrationService.createRegistration(registrationDto2, 1L);

        RegistrationResponseDto retrievedRegistration = registrationService.findRegistrationById(registration2.id());

        assertEquals(registrationDto2.username(), retrievedRegistration.username(), "usernames must be the same");
        assertEquals(registrationDto2.email(), retrievedRegistration.email(), "emails must be the same");
        assertEquals(registrationDto2.phone(), retrievedRegistration.phone(), "Phone numbers must be the same");

        assertThrows(NotFoundException.class, () -> registrationService.findRegistrationById(registration2.id() + 1),
                "must throw not found exception if object not found");
    }

    @Test
    void findRegistrationsByEventIdSuccessWhenEmpty() {
        List<RegistrationResponseDto> emptyList =
                registrationService.findAllRegistrationsByEventId(0, 10, 9999999999999L);

        assertEquals(0, emptyList.size(), "List must be empty");
    }

    @Test
    @SneakyThrows
    void findRegistrationsByEventIdOneRegistrationOnly() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 2L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.createRegistration(registrationDto, 1L);

        List<RegistrationResponseDto> oneRegistrationList =
                registrationService.findAllRegistrationsByEventId(0, 10, 2L);

        assertEquals(1, oneRegistrationList.size(),
                "There is only 1 registration for that event");

        assertEquals(registrationDto.username(), oneRegistrationList.get(0).username(), "username must be the same");
        assertEquals(registrationDto.email(), oneRegistrationList.get(0).email(), "email must be the same");
        assertEquals(registrationDto.phone(), oneRegistrationList.get(0).phone(), "phone must be the same");
    }

    @Test
    @SneakyThrows
    void findRegistrationsByEventIdTwoRegistrations() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 2L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.createRegistration(registrationDto, 1L);
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78885553535", 2L);

        registrationService.createRegistration(registrationDto2, 1L);

        List<RegistrationResponseDto> registrationsList =
                registrationService.findAllRegistrationsByEventId(0, 10, 2L);

        assertEquals(2, registrationsList.size(), "There are only 2 registrations for that event");
    }

    @Test
    @SneakyThrows
    void deleteNonExistingRegistrationByIdFail() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        RegistrationCredentials registrationNotExistDeleteDto =
                createRegistrationCredentials(registration.id() + 1, registration.password());

        assertThrows(NotFoundException.class, () -> registrationService.deleteRegistration(registrationNotExistDeleteDto));
    }

    @Test
    @SneakyThrows
    void deleteRegistrationByIdFailWrongPassword() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        RegistrationCredentials wrongPasswordDeleteDto =
                createRegistrationCredentials(registration.id(), "fail");

        assertThrows(PasswordIncorrectException.class, () -> registrationService.deleteRegistration(wrongPasswordDeleteDto));
    }

    @Test
    @SneakyThrows
    void deleteRegistrationByIdSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto registration = registrationService.createRegistration(registrationDto, 1L);

        RegistrationCredentials deleteDto =
                createRegistrationCredentials(registration.id(), registration.password());

        registrationService.deleteRegistration(deleteDto);

        assertThrows(NotFoundException.class, () -> registrationService.findRegistrationById(registration.id()));
    }

    @Test
    void createRegistration_whenCreated_statusShouldBePending() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);

        RegistrationResponseDto result = registrationService.findRegistrationById(createdRegistration.id());

        assertEquals(PENDING, result.status());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by event owner, set APPROVE, no participant limit")
    void updateRegistrationStatus_whenUserIsEventOwnerValidPasswordAndRegistrationFound_shouldUpdateStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        RegistrationStatus newStatus = APPROVED;

        RegistrationStatus updatedStatus = registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                newStatus, credentials);

        assertEquals(newStatus, updatedStatus);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by team manager, set APPROVE, no participant limit")
    void updateRegistrationStatus_whenUserIsManager_shouldUpdateStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MANAGER);
        RegistrationStatus newStatus = APPROVED;

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus updatedStatus = registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                newStatus, credentials);

        assertEquals(newStatus, updatedStatus);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by team manager when 2 users in the team, set APPROVE, no participant limit")
    void updateRegistrationStatus_whenUserIsManagerAndTwoTeamMembersWereFound_shouldUpdateStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MANAGER);
        TeamMemberDto teamMemberDto1 =
                createTeamMember(userId + 2L, registrationDto.eventId(), TeamMemberRole.MEMBER);
        RegistrationStatus newStatus = APPROVED;

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationStatus updatedStatus = registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                newStatus, credentials);

        assertEquals(newStatus, updatedStatus);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by team member when 1 user in the team and is not authorized")
    void updateRegistrationStatus_whenUserIsTeamMemberAndOneTeamMemberWasFound_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MEMBER);
        RegistrationStatus newStatus = APPROVED;

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(HttpStatus.OK.value())));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                        newStatus, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by team member when 2 users in the team and the user is not authorized")
    void updateRegistrationStatus_whenUserIsTeamMemberAndTwoTeamMembersWereFound_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MEMBER);
        TeamMemberDto teamMemberDto1 =
                createTeamMember(userId + 2L, registrationDto.eventId(), TeamMemberRole.MANAGER);
        RegistrationStatus newStatus = APPROVED;

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto, teamMemberDto1)))
                        .withStatus(HttpStatus.OK.value())));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                        newStatus, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration by team member when 2 users in the team and the user is not authorized")
    void updateRegistrationStatus_whenUserIsNotTeamMemberOrOwner_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        RegistrationStatus newStatus = APPROVED;

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(new ArrayList<>()))
                        .withStatus(HttpStatus.OK.value())));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(),
                        newStatus, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, set APPROVE, limit exceeded by one")
    void updateRegistrationStatus_whenParticipantLimitExceededByOne_shouldSetStatusToWaiting() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        RegistrationStatus approved = APPROVED;
        registrationService.updateRegistrationStatus(userId, createdRegistration.id(), approved, credentials);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);
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
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        RegistrationStatus approved = APPROVED;
        registrationService.updateRegistrationStatus(userId, createdRegistration.id(), approved, credentials);

        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail2@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);
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
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.createRegistration(registrationDto3, 1L);
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
        assertEquals(WAITING, registrationService.findRegistrationById(createdRegistration2.id()).status());
        assertEquals(APPROVED, registrationService.findRegistrationById(createdRegistration.id()).status());
    }

    @SneakyThrows
    @Test
    void updateRegistrationStatus_whenEventNotFound_shouldThrowNotFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
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
    @SneakyThrows
    void updateRegistrationStatus_whenInvalidPassword_shouldThrowPasswordIncorrectException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        String incorrectPassword = "6666";
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), incorrectPassword);

        RegistrationStatus newStatus = APPROVED;

        PasswordIncorrectException ex = assertThrows(PasswordIncorrectException.class,
                () -> registrationService.updateRegistrationStatus(userId, createdRegistration.id(), newStatus, credentials));

        assertEquals("Password=" + incorrectPassword + " for registrationId=" +
                createdRegistration.id() + " is not correct", ex.getLocalizedMessage());
    }

    @Test
    @SneakyThrows
    void updateRegistrationStatus_whenRegistrationNotFound_shouldThrowNotFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        Long unknownId = 999L;
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());

        RegistrationStatus newStatus = APPROVED;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> registrationService.updateRegistrationStatus(userId, unknownId, newStatus, credentials));

        assertEquals("Registration with id=" + unknownId + " was not found", ex.getLocalizedMessage());
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsEventOwnerAndValidPasswordAndRegistrationFound_shouldSetDeclineStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        RegistrationStatus updatedStatus = registrationService.declineRegistration(userId, createdRegistration.id(),
                reason, credentials);

        assertEquals(DECLINED, updatedStatus);
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsTeamManager_shouldSetDeclineStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MANAGER);

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        RegistrationStatus updatedStatus = registrationService.declineRegistration(userId, createdRegistration.id(),
                reason, credentials);

        assertEquals(DECLINED, updatedStatus);
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsTeamManagerAndTwoUsersInTeam_shouldSetDeclineStatus() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MANAGER);
        TeamMemberDto teamMemberDto1 =
                createTeamMember(userId + 2L, registrationDto.eventId(), TeamMemberRole.MEMBER);

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto, teamMemberDto1)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        RegistrationStatus updatedStatus = registrationService.declineRegistration(userId, createdRegistration.id(),
                reason, credentials);

        assertEquals(DECLINED, updatedStatus);
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsTeamMemberAndOneUserInTeam_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MEMBER);

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.declineRegistration(userId, createdRegistration.id(),
                        reason, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsNotTeamMemberOrOwner_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(new ArrayList<>()))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.declineRegistration(userId, createdRegistration.id(),
                        reason, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenUserIsTeamMemberAndTwoUsersInTeamAndUserNotAuthorized_shouldThrowNotAuthorized() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId + 1L, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        TeamMemberDto teamMemberDto = createTeamMember(userId, registrationDto.eventId(), TeamMemberRole.MEMBER);
        TeamMemberDto teamMemberDto1 =
                createTeamMember(userId + 2L, registrationDto.eventId(), TeamMemberRole.MANAGER);

        stubFor(get(urlEqualTo("/events/teams/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto, teamMemberDto1)))
                        .withStatus(HttpStatus.OK.value())));

        RegistrationCredentials credentials =
                createRegistrationCredentials(createdRegistration.id(), createdRegistration.password());
        String reason = "reason";

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> registrationService.declineRegistration(userId, createdRegistration.id(),
                        reason, credentials));

        assertEquals(String.format("User id=%d has no rights to change registration status for event id=%d",
                userId, registrationDto.eventId()), ex.getMessage());
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenInvalidPassword_shouldThrowPasswordIncorrectException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
        String incorrectPassword = "6666";
        RegistrationCredentials credentials = createRegistrationCredentials(createdRegistration.id(), incorrectPassword);
        String reason = "reason";

        PasswordIncorrectException ex = assertThrows(PasswordIncorrectException.class,
                () -> registrationService.declineRegistration(userId, createdRegistration.id(), reason, credentials));

        assertEquals("Password=" + incorrectPassword + " for registrationId=" +
                createdRegistration.id() + " is not correct", ex.getLocalizedMessage());
    }

    @Test
    @SneakyThrows
    void declineRegistration_whenNotFound_shouldThrowNofFoundException() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 1);

        stubFor(get(urlEqualTo("/events/" + registrationDto.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration = registrationService.createRegistration(registrationDto, 1L);
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
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto1.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration1 = registrationService.createRegistration(registrationDto1, 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.createRegistration(registrationDto3, 1L);

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
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration1 = registrationService.createRegistration(registrationDto1, 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.createRegistration(registrationDto3, 1L);

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
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 2L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration1 = registrationService.createRegistration(registrationDto1, 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto3.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration3 = registrationService.createRegistration(registrationDto3, 1L);
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
        NewRegistrationDto registrationDto2 =
                createNewRegistrationDto("user2", "mail@mail.com", "78005553535", 1L);
        NewRegistrationDto registrationDto3 =
                createNewRegistrationDto("user3", "mail@mail.com", "78005553535", 1L);
        NewRegistrationDto registrationDto4 =
                createNewRegistrationDto("user4", "mail@mail.com", "78005553535", 1L);
        EventDto eventDto = createEvent(userId, 0);

        stubFor(get(urlEqualTo("/events/" + registrationDto2.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        CreatedRegistrationResponseDto createdRegistration1 = registrationService.createRegistration(registrationDto1, 1L);
        CreatedRegistrationResponseDto createdRegistration2 = registrationService.createRegistration(registrationDto2, 1L);
        CreatedRegistrationResponseDto createdRegistration3 = registrationService.createRegistration(registrationDto3, 1L);
        CreatedRegistrationResponseDto createdRegistration4 = registrationService.createRegistration(registrationDto4, 1L);

        registrationService.updateRegistrationStatus(userId, createdRegistration2.id(), APPROVED,
                new RegistrationCredentials(createdRegistration2.id(), createdRegistration2.password()));

        stubFor(get(urlEqualTo("/events/" + registrationDto3.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(eventDto))
                        .withStatus(HttpStatus.OK.value())));

        registrationService.updateRegistrationStatus(userId, createdRegistration3.id(), WAITING,
                new RegistrationCredentials(createdRegistration3.id(), createdRegistration3.password()));
        registrationService.findAllRegistrationsByEventId(0, 10, registrationDto1.eventId());

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

    private TeamMemberDto createTeamMember(Long userId, Long eventId, TeamMemberRole role) {
        return TeamMemberDto.builder()
                .eventId(eventId)
                .userId(userId)
                .role(role)
                .build();
    }
}
