package ru.ms.second.team.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.ms.second.team.registration.dto.registration.NewRegistrationDto;
import ru.ms.second.team.registration.dto.registration.RegistrationCredentials;
import ru.ms.second.team.registration.dto.registration.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.registration.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.RegistrationCount;
import ru.ms.second.team.registration.dto.registration.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.registration.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.exception.exceptions.NotFoundException;
import ru.ms.second.team.registration.exception.exceptions.PasswordIncorrectException;
import ru.ms.second.team.registration.model.RegistrationStatus;
import ru.ms.second.team.registration.service.RegistrationService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegistrationController.class)
public class RegistrationControllerTest {
    @Autowired
    ObjectMapper mapper;
    @Autowired
    MockMvc mvc;
    @MockBean
    RegistrationService registrationService;

    private NewRegistrationDto newRegistrationDto;
    private UpdateRegistrationDto updateRegistrationDto;
    private UpdatedRegistrationResponseDto updatedRegistrationResponseDto;
    private RegistrationCredentials registrationCredentials;
    private RegistrationResponseDto registrationResponseDto;
    private Long userId;

    @BeforeEach
    void init() {
        userId = 4L;
    }

    @Test
    @SneakyThrows
    @DisplayName("New registration created successfully")
    void createRegistrationOk() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "email@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistrationResponseDto = createNewRegistrationResponseDto();
        when(registrationService.createRegistration(newRegistrationDto, userId))
                .thenReturn(createdRegistrationResponseDto);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", userId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.password", is(createdRegistrationResponseDto.password())));
        verify(registrationService, times(1)).createRegistration(newRegistrationDto, userId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to blank username")
    void createNewRegistrationBlankUsername() {
        newRegistrationDto =
                createNewRegistrationDto("    ", "email@mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", userId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to invalid phone number")
    void createNewRegistrationInvalidPhoneNumber() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "email@mail.com", "7123456", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", userId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to invalid email")
    void createNewRegistrationInvalidEmail() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", userId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to event id is not positive")
    void createNewRegistrationEventIdIsZero() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 0L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", userId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to user id is null")
    void createNewRegistrationUserIdIsNull() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to user id is not positive")
    void createNewRegistrationUserIdIsNonPositive() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .header("X-User-Id", 0L)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).createRegistration(any(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Username updated successfully")
    void updateRegistrationOnlyUsername() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user2", null, null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user2", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Email updated successfully")
    void updateRegistrationOnlyEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, "mail@mail.com", null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "mail@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Phone updated successfully")
    void updateRegistrationOnlyPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, null, "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username and email updated successfully")
    void updateRegistrationUsernameAndEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user1", "email@mail.com", null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username and phone updated successfully")
    void updateRegistrationUsernameAndPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user1", null, "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Phone and email updated successfully")
    void updateRegistrationEmailAndPhone() {
        updateRegistrationDto = createUpdateRegistrationDto(
                null, "email@mail.com", "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username, Phone and email updated successfully")
    void updateRegistrationUsernameEmailPhone() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user1", "email@mail.com", "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.updateRegistration(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to non positive registration id")
    void updateRegistrationUsernameFailNonPositiveId() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user1", "email@mail.com", "78005553535", 0L, "1234");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to too short password")
    void updateRegistrationUsernameFailShortPassword() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user1", "email@mail.com", "78005553535", 1L, "123");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to too long password")
    void updateRegistrationUsernameFailLongPassword() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user1", "email@mail.com", "78005553535", 1L, "12345");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to blank username")
    void updateRegistrationFailBlankUsername() {
        updateRegistrationDto =
                createUpdateRegistrationDto("   ", null, null, 1L, "1234");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to invalid email")
    void updateRegistrationFailInvalidEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, "mail.com", null, 1L, "1234");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update failed due to invalid phone number")
    void updateRegistrationFailInvalidPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, null, "712345678910", 1L, "1234");
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).updateRegistration(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration with id=1 retrieved successfully")
    void getRegistrationById() {
        registrationResponseDto =
                createResponseDto();
        when(registrationService.findRegistrationById(registrationResponseDto.eventId())).thenReturn(registrationResponseDto);
        mvc.perform(get("/registrations/" + registrationResponseDto.eventId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(registrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(registrationResponseDto.email())))
                .andExpect(jsonPath("$.eventId", is(1)))
                .andExpect(jsonPath("$.phone", is(registrationResponseDto.phone())));
        verify(registrationService, times(1)).findRegistrationById(registrationResponseDto.eventId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Retrieving registration failed due to non positive event id")
    void getRegistrationEventIdZero() {
        mvc.perform(get("/registrations/0")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).findRegistrationById(any());
    }


    @Test
    @SneakyThrows
    @DisplayName("Registrations for Event retrieved successfully")
    void getRegistrationsForEvent() {
        registrationResponseDto =
                createResponseDto();
        when(registrationService.findAllRegistrationsByEventId(0, 10, registrationResponseDto.eventId()))
                .thenReturn(List.of(registrationResponseDto));
        mvc.perform(get("/registrations?eventId=" + registrationResponseDto.eventId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is(registrationResponseDto.username())))
                .andExpect(jsonPath("$[0].email", is(registrationResponseDto.email())))
                .andExpect(jsonPath("$[0].eventId", is(1)))
                .andExpect(jsonPath("$[0].phone", is(registrationResponseDto.phone())));
        verify(registrationService, times(1))
                .findAllRegistrationsByEventId(0, 10, registrationResponseDto.eventId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Retrieving registrations failed due to non positive event id")
    void getRegistrationsForEventIdZero() {
        mvc.perform(get("/registrations?eventId=0")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).findAllRegistrationsByEventId(anyInt(), anyInt(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Retrieving registrations failed due to negative page")
    void getRegistrationsForEventPageNegative() {
        mvc.perform(get("/registrations?page=-1&eventId=1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).findAllRegistrationsByEventId(anyInt(), anyInt(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Retrieving registrations failed due to non positive page size")
    void getRegistrationsForEventSizeNonPositive() {
        mvc.perform(get("/registrations?size=0&eventId=1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).findAllRegistrationsByEventId(anyInt(), anyInt(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration deleted successfully")
    void deleteRegistrationById() {
        registrationCredentials = createRegistrationCredentials(1L, "1234");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(registrationService, times(1)).deleteRegistration(registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to deleteRegistration due to non positive id")
    void deleteRegistrationByIdNonPositiveId() {
        registrationCredentials = createRegistrationCredentials(0L, "1234");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).deleteRegistration(registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to deleteRegistration due to too short password")
    void deleteRegistrationFailShortPassword() {
        registrationCredentials = createRegistrationCredentials(1L, "123");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).deleteRegistration(registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to deleteRegistration due to too long password")
    void deleteRegistrationFailLongPassword() {
        registrationCredentials = createRegistrationCredentials(1L, "12345");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).deleteRegistration(registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, valid status")
    void updateRegistrationStatus_whenValidStatus_shouldReturn200() {
        RegistrationStatus status = RegistrationStatus.APPROVED;
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1234");

        when(registrationService.updateRegistrationStatus(userId, registrationId, status, registrationCredentials))
                .thenReturn(status);

        mvc.perform(patch("/registrations/{registrationId}/status", registrationId)
                        .param("newStatus", String.valueOf(status))
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(status)));

        verify(registrationService, times(1)).updateRegistrationStatus(userId, registrationId, status,
                registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, invalid status")
    void updateRegistrationStatus_whenDeclinedStatus_shouldReturn400() {
        RegistrationStatus status = RegistrationStatus.DECLINED;
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1234");

        mvc.perform(patch("/registrations/{registrationId}/status", registrationId)
                        .header("X-User-Id", userId)
                        .param("newStatus", String.valueOf(status))
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).updateRegistrationStatus(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, registration not found")
    void updateRegistrationStatus_whenRegistrationNotFound_shouldReturn400() {
        RegistrationStatus status = RegistrationStatus.WAITING;
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1234");


        when(registrationService.updateRegistrationStatus(userId, registrationId, status, registrationCredentials))
                .thenThrow(NotFoundException.class);

        mvc.perform(patch("/registrations/{registrationId}/status", registrationId)
                        .param("newStatus", String.valueOf(status))
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(registrationService, times(1)).updateRegistrationStatus(userId, registrationId,
                status, registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update registration, wrong password")
    void updateRegistrationStatus_whenWrongPassword_shouldReturn400() {
        RegistrationStatus status = RegistrationStatus.WAITING;
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1234");


        when(registrationService.updateRegistrationStatus(userId, registrationId, status, registrationCredentials))
                .thenThrow(PasswordIncorrectException.class);

        mvc.perform(patch("/registrations/{registrationId}/status", registrationId)
                        .param("newStatus", String.valueOf(status))
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(registrationService, times(1)).updateRegistrationStatus(userId, registrationId,
                status, registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Search registration by one status")
    void searchRegistrations_whenSearchByOneStatus_shouldReturn200Status() {
        List<RegistrationStatus> statuses = List.of(RegistrationStatus.WAITING, RegistrationStatus.PENDING);
        Long eventId = 43L;
        RegistrationResponseDto responseDto = createResponseDto();

        when(registrationService.searchRegistrations(statuses, eventId))
                .thenReturn(Collections.singletonList(responseDto));

        mvc.perform(get("/registrations/search")
                        .param("statuses", RegistrationStatus.WAITING.name())
                        .param("statuses", RegistrationStatus.PENDING.name())
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$.[0].username", is(responseDto.username())));

        verify(registrationService, times(1)).searchRegistrations(statuses, eventId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Search registration by one status")
    void getRegistrationCountByStatus_shouldReturn200Status() {
        RegistrationStatus status = RegistrationStatus.WAITING;
        RegistrationCount count = RegistrationCount.builder()
                .numberOfWaitingRegistrations(1)
                .numberOfDeclinedRegistrations(2)
                .numberOfApprovedRegistrations(3)
                .numberOfPendingRegistrations(4)
                .build();
        Long eventId = 434L;

        when(registrationService.getRegistrationsCountByEventId(eventId))
                .thenReturn(count);

        mvc.perform(get("/registrations/count")
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfWaitingRegistrations", is(count.numberOfWaitingRegistrations()), Long.class))
                .andExpect(jsonPath("$.numberOfDeclinedRegistrations", is(count.numberOfDeclinedRegistrations()), Long.class))
                .andExpect(jsonPath("$.numberOfApprovedRegistrations", is(count.numberOfApprovedRegistrations()), Long.class))
                .andExpect(jsonPath("$.numberOfPendingRegistrations", is(count.numberOfPendingRegistrations()), Long.class));

        verify(registrationService, times(1)).getRegistrationsCountByEventId(eventId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Decline registration, registration exists")
    void declineRegistration_whenRegistrationExists_shouldReturn200Status() {
        String reason = "reason";
        Long registrationId = 34L;
        RegistrationStatus status = RegistrationStatus.DECLINED;
        registrationCredentials = createRegistrationCredentials(1L, "1234");

        when(registrationService.declineRegistration(userId, registrationId, reason, registrationCredentials))
                .thenReturn(status);

        mvc.perform(patch("/registrations/{registrationId}/status/decline", registrationId)
                        .param("reason", reason)
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(status)));

        verify(registrationService, times(1)).declineRegistration(userId, registrationId,
                reason, registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Decline registration, registration not exists")
    void declineRegistration_whenRegistrationNotExists_shouldReturn200Status() {
        String reason = "reason";
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1234");

        when(registrationService.declineRegistration(userId, registrationId, reason, registrationCredentials))
                .thenThrow(NotFoundException.class);

        mvc.perform(patch("/registrations/{registrationId}/status/decline", registrationId)
                        .param("reason", reason)
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(registrationService, times(1)).declineRegistration(userId,
                registrationId, reason, registrationCredentials);
    }

    @Test
    @SneakyThrows
    @DisplayName("Decline registration, wrong password")
    void declineRegistration_whenWrongPassword_shouldReturn200Status() {
        String reason = "reason";
        Long registrationId = 34L;
        registrationCredentials = createRegistrationCredentials(1L, "1235");

        when(registrationService.declineRegistration(userId, registrationId, reason, registrationCredentials))
                .thenThrow(PasswordIncorrectException.class);

        mvc.perform(patch("/registrations/{registrationId}/status/decline", registrationId)
                        .param("reason", reason)
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(registrationCredentials))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(registrationService, times(1)).declineRegistration(userId, registrationId,
                reason, registrationCredentials);
    }

    private NewRegistrationDto createNewRegistrationDto(String username, String email, String phone, Long eventId) {
        return NewRegistrationDto.builder()
                .email(email)
                .eventId(eventId)
                .phone(phone)
                .username(username)
                .build();
    }

    private CreatedRegistrationResponseDto createNewRegistrationResponseDto() {
        return CreatedRegistrationResponseDto.builder()
                .password("1234")
                .id(1L)
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

    private UpdatedRegistrationResponseDto createUpdateResponseDto(String username, String email) {
        return UpdatedRegistrationResponseDto.builder()
                .username(username)
                .email(email)
                .phone("78005553535")
                .build();
    }

    private RegistrationCredentials createRegistrationCredentials(Long id, String password) {
        return RegistrationCredentials.builder()
                .id(id)
                .password(password)
                .build();
    }

    private RegistrationResponseDto createResponseDto() {
        return RegistrationResponseDto.builder()
                .username("user1")
                .phone("78005553535")
                .eventId(1L)
                .email("email@mail.com")
                .build();
    }
}
