package ru.ms.second.team.registration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import ru.ms.second.team.registration.service.impl.RegistrationServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceImplMockTest {
    @InjectMocks
    private RegistrationServiceImpl registrationService;
    @Mock
    private JpaRegistrationRepository repository;
    @Mock
    private RegistrationMapper mapper;

    private UpdateRegistrationDto updateRegistrationDto;
    private UpdatedRegistrationResponseDto updatedRegistrationResponseDto;
    private DeleteRegistrationDto deleteRegistrationDto;
    private RegistrationResponseDto registrationResponseDto;
    private Registration registration;

    @Test
    @DisplayName("Created registration")
    void createRegistration() {
        NewRegistrationDto newRegistrationDto = createNewRegistrationDto();
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535");
        CreatedRegistrationResponseDto createdRegistrationResponseDto = createNewRegistrationResponseDto(registration.getId());
        Registration registrationFromMapper = createRegistration(
                0L, "user1", "mail@mail.com", "78005553535");

        when(mapper.toRegistration(any(NewRegistrationDto.class), anyString())).thenReturn(registrationFromMapper);
        when(mapper.toCreatedDto(any(Registration.class))).thenReturn(createdRegistrationResponseDto);
        when(repository.save(any(Registration.class))).thenReturn(registration);

        CreatedRegistrationResponseDto result = registrationService.create(newRegistrationDto);

        assertEquals(result.id(), createdRegistrationResponseDto.id(), "id's must be same");
        assertEquals(result.password(), registration.getPassword(), "passwords must be same");

        verify(mapper, times(1)).toRegistration(any(NewRegistrationDto.class), anyString());
        verify(mapper, times(1)).toCreatedDto(any(Registration.class));
        verify(repository, times(1)).save(any(Registration.class));
    }

    @Test
    @DisplayName("Updated registration username successfully")
    void updateRegistrationUsername() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user2", null, null, "1234");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        Registration updatedRegistration = createRegistration(
                1L, "user2", "mail@mail.com", "78005553535"
        );
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user2", "mail@mail.com", "78005553535");

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));
        when(mapper.toUpdatedDto(any(Registration.class))).thenReturn(updatedRegistrationResponseDto);
        when(repository.save(any(Registration.class))).thenReturn(updatedRegistration);

        UpdatedRegistrationResponseDto result = registrationService.update(updateRegistrationDto);

        assertEquals(updateRegistrationDto.username(), result.username(), "usernames must be same");
        assertEquals(registration.getEmail(), result.email(), "emails must be same");
        assertEquals(registration.getPhone(), result.phone(), "phones must be same");

        verify(mapper, times(1)).toUpdatedDto(any(Registration.class));
        verify(repository, times(1)).findById(anyLong());
        verify(repository, times(1)).save(any(Registration.class));
    }

    @Test
    @DisplayName("Updated registration email successfully")
    void updateRegistrationEmail() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, "mail@gmail.com", null, "1234");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        Registration updatedRegistration = createRegistration(
                1L, "user1", "mail@gmail.com", "78005553535"
        );
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "mail@gmail.com", "78005553535");

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));
        when(mapper.toUpdatedDto(any(Registration.class))).thenReturn(updatedRegistrationResponseDto);
        when(repository.save(any(Registration.class))).thenReturn(updatedRegistration);

        UpdatedRegistrationResponseDto result = registrationService.update(updateRegistrationDto);

        assertEquals(registration.getUsername(), result.username(), "usernames must be same");
        assertEquals(updateRegistrationDto.email(), result.email(), "emails must be same");
        assertEquals(registration.getPhone(), result.phone(), "phones must be same");

        verify(mapper, times(1)).toUpdatedDto(any(Registration.class));
        verify(repository, times(1)).findById(anyLong());
        verify(repository, times(1)).save(any(Registration.class));
    }

    @Test
    @DisplayName("Updated registration phone successfully")
    void updateRegistrationPhone() {
        updateRegistrationDto =
                createUpdateRegistrationDto(null, null, "70123456789", "1234");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        Registration updatedRegistration = createRegistration(
                1L, "user1", "mail@mail.com", "70123456789"
        );
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user1", "mail@mail.com", "70123456789");

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));
        when(mapper.toUpdatedDto(any(Registration.class))).thenReturn(updatedRegistrationResponseDto);
        when(repository.save(any(Registration.class))).thenReturn(updatedRegistration);

        UpdatedRegistrationResponseDto result = registrationService.update(updateRegistrationDto);

        assertEquals(registration.getUsername(), result.username(), "usernames must be same");
        assertEquals(registration.getEmail(), result.email(), "emails must be same");
        assertEquals(updateRegistrationDto.phone(), result.phone(), "phones must be same");

        verify(mapper, times(1)).toUpdatedDto(any(Registration.class));
        verify(repository, times(1)).findById(anyLong());
        verify(repository, times(1)).save(any(Registration.class));
    }

    @Test
    @DisplayName("Updated registration username, email and phone successfully")
    void updateRegistrationUsernameEmailPhone() {
        updateRegistrationDto = createUpdateRegistrationDto(
                "user2", "mail@gmail.com", "70123456789", "1234"
        );
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        Registration updatedRegistration = createRegistration(
                1L, "user2", "mail@gmail.com", "70123456789"
        );
        updatedRegistrationResponseDto =
                createUpdateResponseDto("user2", "mail@gmail.com", "70123456789");

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));
        when(mapper.toUpdatedDto(any(Registration.class))).thenReturn(updatedRegistrationResponseDto);
        when(repository.save(any(Registration.class))).thenReturn(updatedRegistration);

        UpdatedRegistrationResponseDto result = registrationService.update(updateRegistrationDto);

        assertEquals(updateRegistrationDto.username(), result.username(), "usernames must be same");
        assertEquals(updateRegistrationDto.email(), result.email(), "emails must be same");
        assertEquals(updateRegistrationDto.phone(), result.phone(), "phones must be same");

        verify(mapper, times(1)).toUpdatedDto(any(Registration.class));
        verify(repository, times(1)).findById(anyLong());
        verify(repository, times(1)).save(any(Registration.class));
    }

    @Test
    @DisplayName("Update registration failed due to incorrect password")
    void updateRegistrationFailIncorrectPassword() {
        updateRegistrationDto =
                createUpdateRegistrationDto("user2", null, null, "4321");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));

        assertThrows(PasswordIncorrectException.class, () -> registrationService.update(updateRegistrationDto));

        verify(repository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("Registration successfully retrieved")
    void findRegistrationById() {
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        registrationResponseDto =
                createResponseDto(registration.getUsername(), registration.getEmail(), registration.getPhone());

        when(mapper.toRegistrationResponseDto(any(Registration.class))).thenReturn(registrationResponseDto);
        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));

        RegistrationResponseDto result = registrationService.findById(1L);

        assertEquals(registration.getUsername(), result.username(), "usernames must be same");
        assertEquals(registration.getEmail(), result.email(), "emails must be same");
        assertEquals(registration.getPhone(), result.phone(), "phones must be same");

        verify(mapper, times(1)).toRegistrationResponseDto(any(Registration.class));
        verify(repository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("Registration retrieval failed because object was not found")
    void getRegistrationFailNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> registrationService.findById(1L));

        verify(repository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("Retrieve all registrations by event id successfully")
    void getAllRegistrationsByEventId() {
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );
        registrationResponseDto =
                createResponseDto(registration.getUsername(), registration.getEmail(), registration.getPhone());
        Page<Registration> page = new PageImpl(List.of(registration));


        when(repository.findAllByEventId(1L, PageRequest.of(0, 10))).thenReturn(page);
        when(mapper.toRegistrationResponseDto(any(Registration.class))).thenReturn(registrationResponseDto);

        List<RegistrationResponseDto> result = registrationService.findAllByEventId(0, 10, 1L);

        assertEquals(registration.getUsername(), result.get(0).username(), "usernames must be same");
        assertEquals(registration.getEmail(), result.get(0).email(), "emails must be same");
        assertEquals(registration.getPhone(), result.get(0).phone(), "phones must be same");

        verify(repository, times(1)).findAllByEventId(anyLong(), any(Pageable.class));
        verify(mapper, times(1)).toRegistrationResponseDto(any(Registration.class));
    }

    @Test
    @DisplayName("Retrieve all registrations by event id. Successful even when empty")
    void getAllRegistrationsByEventIdEmpty() {
        when(repository.findAllByEventId(1L, PageRequest.of(0, 10))).thenReturn(Page.empty());

        List<RegistrationResponseDto> result = registrationService.findAllByEventId(0, 10, 1L);

        assertEquals(0, result.size());

        verify(repository, times(1)).findAllByEventId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Delete registration successfully")
    void deleteRegistrationById() {
        deleteRegistrationDto = createDeleteRegistrationDto("1234");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));

        registrationService.delete(deleteRegistrationDto);

        verify(repository, times(1)).findById(anyLong());
        verify(repository, times(1)).deleteById(deleteRegistrationDto.id());
    }

    @Test
    @DisplayName("Deletion failed due to incorrect password")
    void deleteFailIncorrectPassword() {
        deleteRegistrationDto = createDeleteRegistrationDto("4321");
        registration = createRegistration(
                1L, "user1", "mail@mail.com", "78005553535"
        );

        when(repository.findById(anyLong())).thenReturn(Optional.of(registration));

        assertThrows(PasswordIncorrectException.class, () -> registrationService.delete(deleteRegistrationDto));

        verify(repository, times(1)).findById(anyLong());
        verify(repository, never()).deleteById(deleteRegistrationDto.id());
    }

    @Test
    @DisplayName("Deletion failed due to object was not found")
    void deleteFailNotFound() {
        deleteRegistrationDto = createDeleteRegistrationDto("4321");

        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> registrationService.delete(deleteRegistrationDto));

        verify(repository, times(1)).findById(anyLong());
        verify(repository, never()).deleteById(deleteRegistrationDto.id());
    }

    private NewRegistrationDto createNewRegistrationDto() {
        return NewRegistrationDto.builder()
                .email("mail@mail.com")
                .eventId(1L)
                .phone("78005553535")
                .username("user1")
                .build();
    }

    private CreatedRegistrationResponseDto createNewRegistrationResponseDto(Long id) {
        return CreatedRegistrationResponseDto.builder()
                .password("1234")
                .id(id)
                .build();
    }

    private UpdateRegistrationDto createUpdateRegistrationDto(
            String username, String email, String phone, String password) {
        return UpdateRegistrationDto.builder()
                .id(1L)
                .password(password)
                .username(username)
                .email(email)
                .phone(phone)
                .build();
    }

    private UpdatedRegistrationResponseDto createUpdateResponseDto(String username, String email, String phone) {
        return UpdatedRegistrationResponseDto.builder()
                .username(username)
                .email(email)
                .phone(phone)
                .build();
    }

    private DeleteRegistrationDto createDeleteRegistrationDto(String password) {
        return DeleteRegistrationDto.builder()
                .id(1L)
                .password(password).build();
    }

    private RegistrationResponseDto createResponseDto(String username, String email, String phone) {
        return RegistrationResponseDto.builder()
                .username(username)
                .phone(phone)
                .eventId(1L)
                .email(email)
                .build();
    }

    private Registration createRegistration(Long id,
                                            String userName,
                                            String email,
                                            String phone) {
        return Registration.builder()
                .id(id)
                .username(userName)
                .email(email)
                .phone(phone)
                .eventId(1L)
                .password("1234")
                .build();
    }

}
