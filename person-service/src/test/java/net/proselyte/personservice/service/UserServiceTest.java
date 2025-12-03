package net.proselyte.personservice.service;

import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.person.dto.IndividualWriteResponseDto;
import net.proselyte.personservice.entity.User;
import net.proselyte.personservice.exception.PersonException;
import net.proselyte.personservice.mapper.UserMapper;
import net.proselyte.personservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private IndividualWriteDto writeDto;
    private IndividualDto dto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setNickname("testuser");
        testUser.setCreated(Instant.now());
        testUser.setUpdated(Instant.now());

        writeDto = new IndividualWriteDto();
        writeDto.setEmail("test@example.com");
        writeDto.setNickname("testuser");

        dto = new IndividualDto();
        dto.setEmail("test@example.com");
        dto.setNickname("testuser");
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        when(userMapper.to(writeDto)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);

        IndividualWriteResponseDto result = userService.register(writeDto);

        assertNotNull(result);
        assertEquals(userId.toString(), result.getId());
        verify(userMapper).to(writeDto);
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldFindByEmailsReturnEmptyListWhenNoEmailsProvided() {
        List<String> emails = null;

        IndividualPageDto result = userService.findByEmails(emails);

        assertNotNull(result);
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
        verify(userRepository).findAllByEmails(emails);
    }

    @Test
    void shouldFindByEmailsReturnUsers() {
        List<String> emails = List.of("test@example.com");
        List<User> users = List.of(testUser);
        when(userRepository.findAllByEmails(emails)).thenReturn(users);
        when(userMapper.from(testUser)).thenReturn(dto);

        IndividualPageDto result = userService.findByEmails(emails);

        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        assertEquals("test@example.com", result.getItems().get(0).getEmail());
        verify(userRepository).findAllByEmails(emails);
        verify(userMapper).from(testUser);
    }

    @Test
    void shouldFindByIdReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.from(testUser)).thenReturn(dto);

        IndividualDto result = userService.findById(userId);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getNickname());
        verify(userRepository).findById(userId);
        verify(userMapper).from(testUser);
    }

    @Test
    void shouldFindByIdThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PersonException exception = assertThrows(PersonException.class, () -> {
            userService.findById(userId);
        });

        assertEquals("User not found by id=[" + userId + "]", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.delete(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void shouldDeleteThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PersonException exception = assertThrows(PersonException.class, () -> {
            userService.delete(userId);
        });

        assertEquals("User not found by id=[" + userId + "]", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // given
        IndividualWriteDto updateDto = new IndividualWriteDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setNickname("updateduser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userMapper).update(testUser, updateDto);
        when(userRepository.save(testUser)).thenReturn(testUser);

        IndividualWriteResponseDto result = userService.update(userId, updateDto);

        assertNotNull(result);
        assertEquals(userId.toString(), result.getId());
        verify(userRepository).findById(userId);
        verify(userMapper).update(testUser, updateDto);
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldUpdateThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        PersonException exception = assertThrows(PersonException.class, () -> {
            userService.update(userId, writeDto);
        });

        assertEquals("User not found by id=[" + userId + "]", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userMapper, never()).update(any(), any());
        verify(userRepository, never()).save(any());
    }
}



