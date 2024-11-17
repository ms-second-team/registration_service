package ru.ms.second.team.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.ms.second.team.registration.dto.request.DeleteRegistrationDto;
import ru.ms.second.team.registration.dto.request.NewRegistrationDto;
import ru.ms.second.team.registration.dto.request.UpdateRegistrationDto;
import ru.ms.second.team.registration.dto.response.CreatedRegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.RegistrationResponseDto;
import ru.ms.second.team.registration.dto.response.UpdatedRegistrationResponseDto;
import ru.ms.second.team.registration.service.RegistrationService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private DeleteRegistrationDto deleteRegistrationDto;
    private RegistrationResponseDto registrationResponseDto;

    @Test
    @SneakyThrows
    @DisplayName("New registration created successfully")
    void createRegistrationOk() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "email@mail.com", "78005553535", 1L);
        CreatedRegistrationResponseDto createdRegistrationResponseDto = createNewRegistrationResponseDto();
        when(registrationService.create(newRegistrationDto)).thenReturn(createdRegistrationResponseDto);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.password", is(createdRegistrationResponseDto.password())));
        verify(registrationService, times(1)).create(newRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to blank username")
    void createNewRegistrationBlankUsername() {
        newRegistrationDto =
                createNewRegistrationDto("    ", "email@mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).create(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to invalid phone number")
    void createNewRegistrationInvalidPhoneNumber() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "email@mail.com", "7123456", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).create(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to invalid email")
    void createNewRegistrationInvalidEmail() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail.com", "78005553535", 1L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).create(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Creation Failed due to event id is not positive")
    void createNewRegistrationEventIdIsZero() {
        newRegistrationDto =
                createNewRegistrationDto("user1", "mail@mail.com", "78005553535", 0L);
        mvc.perform(post("/registrations")
                        .content(mapper.writeValueAsString(newRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).create(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Username updated successfully")
    void updateRegistrationOnlyUsername() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user2", null, null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user2", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Email updated successfully")
    void updateRegistrationOnlyEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, "mail@mail.com", null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "mail@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Phone updated successfully")
    void updateRegistrationOnlyPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, null, "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username and email updated successfully")
    void updateRegistrationUsernameAndEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user1", "email@mail.com", null, 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updatedRegistrationResponseDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username and phone updated successfully")
    void updateRegistrationUsernameAndPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user1", null, "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updatedRegistrationResponseDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Phone and email updated successfully")
    void updateRegistrationEmailAndPhone() {
        updateRegistrationDto = createUpdateRegistrationDto(
                null, "email@mail.com", "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updatedRegistrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Username, Phone and email updated successfully")
    void updateRegistrationUsernameEmailPhone() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user1", "email@mail.com", "78005553535", 1L, "1234");
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "email@mail.com");
        when(registrationService.update(updateRegistrationDto)).thenReturn(updatedRegistrationResponseDto);
        mvc.perform(patch("/registrations")
                        .content(mapper.writeValueAsString(updateRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(updateRegistrationDto.username())))
                .andExpect(jsonPath("$.email", is(updateRegistrationDto.email())))
                .andExpect(jsonPath("$.phone", is(updateRegistrationDto.phone())));
        verify(registrationService, times(1)).update(updateRegistrationDto);
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
        verify(registrationService, never()).update(updateRegistrationDto);
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
        verify(registrationService, never()).update(updateRegistrationDto);
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
        verify(registrationService, never()).update(updateRegistrationDto);
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
        verify(registrationService, never()).update(any());
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
        verify(registrationService, never()).update(any());
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
        verify(registrationService, never()).update(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration with id=1 retrieved successfully")
    void getRegistrationById() {
        registrationResponseDto =
                createResponseDto();
        when(registrationService.findById(any())).thenReturn(registrationResponseDto);
        mvc.perform(get("/registrations/1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(registrationResponseDto.username())))
                .andExpect(jsonPath("$.email", is(registrationResponseDto.email())))
                .andExpect(jsonPath("$.eventId", is(1)))
                .andExpect(jsonPath("$.phone", is(registrationResponseDto.phone())));
        verify(registrationService, times(1)).findById(registrationResponseDto.eventId());
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
        verify(registrationService, never()).findById(any());
    }


    @Test
    @SneakyThrows
    @DisplayName("Registrations for Event retrieved successfully")
    void getRegistrationsForEvent() {
        registrationResponseDto =
                createResponseDto();
        when(registrationService.findAllByEventId(anyInt(), anyInt(), anyLong())).thenReturn(List.of(registrationResponseDto));
        mvc.perform(get("/registrations?eventId=1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is(registrationResponseDto.username())))
                .andExpect(jsonPath("$[0].email", is(registrationResponseDto.email())))
                .andExpect(jsonPath("$[0].eventId", is(1)))
                .andExpect(jsonPath("$[0].phone", is(registrationResponseDto.phone())));
        verify(registrationService, times(1)).findAllByEventId(anyInt(), anyInt(), anyLong());
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
        verify(registrationService, never()).findAllByEventId(anyInt(), anyInt(), anyLong());
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
        verify(registrationService, never()).findAllByEventId(anyInt(), anyInt(), anyLong());
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
        verify(registrationService, never()).findAllByEventId(anyInt(), anyInt(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration deleted successfully")
    void deleteRegistrationById() {
        deleteRegistrationDto = createDeleteRegistrationDto(1L, "1234");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(deleteRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(registrationService, times(1)).delete(deleteRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to delete due to non positive id")
    void deleteRegistrationByIdNonPositiveId() {
        deleteRegistrationDto = createDeleteRegistrationDto(0L, "1234");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(deleteRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).delete(deleteRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to delete due to too short password")
    void deleteRegistrationFailShortPassword() {
        deleteRegistrationDto = createDeleteRegistrationDto(1L, "123");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(deleteRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).delete(deleteRegistrationDto);
    }

    @Test
    @SneakyThrows
    @DisplayName("Registration failed to delete due to too long password")
    void deleteRegistrationFailLongPassword() {
        deleteRegistrationDto = createDeleteRegistrationDto(1L, "12345");
        mvc.perform(delete("/registrations")
                        .content(mapper.writeValueAsString(deleteRegistrationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(registrationService, never()).delete(deleteRegistrationDto);
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

    private DeleteRegistrationDto createDeleteRegistrationDto(Long id, String password) {
        return DeleteRegistrationDto.builder()
                .id(id)
                .password(password).build();
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
