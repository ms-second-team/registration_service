package ru.ms.second.team.registration.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ms.second.team.registration.dto.request.DeleteRegistrationDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.service.impl.RegistrationServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class RegistrationServiceImplIntegrateTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14-alpine");

    @Autowired
    RegistrationServiceImpl registrationService;

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

        DeleteRegistrationDto registrationNotExistDeleteDto =
                createDeleteRegistrationDto(registration.id() + 1, registration.password());

        assertThrows(NotFoundException.class, () -> registrationService.delete(registrationNotExistDeleteDto));
    }

    @Test
    void deleteRegistrationByIdFailWrongPassword() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        DeleteRegistrationDto wrongPasswordDeleteDto =
                createDeleteRegistrationDto(registration.id(), "fail");

        assertThrows(PasswordIncorrectException.class, () -> registrationService.delete(wrongPasswordDeleteDto));
    }

    @Test
    void deleteRegistrationByIdSuccess() {
        NewRegistrationDto registrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);

        CreatedRegistrationResponseDto registration = registrationService.create(registrationDto);

        DeleteRegistrationDto deleteDto =
                createDeleteRegistrationDto(registration.id(), registration.password());

        registrationService.delete(deleteDto);

        assertThrows(NotFoundException.class, () -> registrationService.findById(registration.id()));
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

    private DeleteRegistrationDto createDeleteRegistrationDto(Long id, String password) {
        return DeleteRegistrationDto.builder()
                .id(id)
                .password(password).build();
    }
}
