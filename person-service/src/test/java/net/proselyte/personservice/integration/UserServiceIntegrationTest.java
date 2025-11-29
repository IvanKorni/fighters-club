package net.proselyte.personservice.integration;

import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.person.dto.IndividualWriteResponseDto;
import net.proselyte.personservice.PersonServiceApplication;
import net.proselyte.personservice.entity.User;
import net.proselyte.personservice.exception.PersonException;
import net.proselyte.personservice.repository.UserRepository;
import net.proselyte.personservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PersonServiceApplication.class)
@Testcontainers
@Transactional
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("person_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterAndFindUser() {
        // given
        IndividualWriteDto writeDto = new IndividualWriteDto();
        writeDto.setEmail("test@example.com");
        writeDto.setNickname("testuser");

        // when
        IndividualWriteResponseDto response = userService.register(writeDto);
        IndividualDto found = userService.findById(UUID.fromString(response.getId()));

        // then
        assertNotNull(response.getId());
        assertEquals("test@example.com", found.getEmail());
        assertEquals("testuser", found.getNickname());
    }

    @Test
    void shouldFindByEmails() {
        // given
        IndividualWriteDto dto1 = new IndividualWriteDto();
        dto1.setEmail("user1@example.com");
        dto1.setNickname("user1");
        userService.register(dto1);

        IndividualWriteDto dto2 = new IndividualWriteDto();
        dto2.setEmail("user2@example.com");
        dto2.setNickname("user2");
        userService.register(dto2);

        // when
        IndividualPageDto result = userService.findByEmails(List.of("user1@example.com", "user2@example.com"));

        // then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());
    }

    @Test
    void shouldUpdateUser() {
        // given
        IndividualWriteDto writeDto = new IndividualWriteDto();
        writeDto.setEmail("test@example.com");
        writeDto.setNickname("testuser");
        IndividualWriteResponseDto response = userService.register(writeDto);
        UUID userId = UUID.fromString(response.getId());

        IndividualWriteDto updateDto = new IndividualWriteDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setNickname("updateduser");

        // when
        IndividualWriteResponseDto updateResponse = userService.update(userId, updateDto);
        IndividualDto updated = userService.findById(userId);

        // then
        assertEquals(userId.toString(), updateResponse.getId());
        assertEquals("updated@example.com", updated.getEmail());
        assertEquals("updateduser", updated.getNickname());
    }

    @Test
    void shouldDeleteUser() {
        // given
        IndividualWriteDto writeDto = new IndividualWriteDto();
        writeDto.setEmail("test@example.com");
        writeDto.setNickname("testuser");
        IndividualWriteResponseDto response = userService.register(writeDto);
        UUID userId = UUID.fromString(response.getId());

        // when
        userService.delete(userId);

        // then
        assertThrows(PersonException.class, () -> userService.findById(userId));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // when & then
        assertThrows(PersonException.class, () -> userService.findById(nonExistentId));
        assertThrows(PersonException.class, () -> userService.delete(nonExistentId));
        assertThrows(PersonException.class, () -> {
            IndividualWriteDto dto = new IndividualWriteDto();
            dto.setEmail("test@example.com");
            dto.setNickname("testuser");
            userService.update(nonExistentId, dto);
        });
    }

    @Test
    void shouldFindByEmailsReturnEmptyWhenNoMatches() {
        // when
        IndividualPageDto result = userService.findByEmails(List.of("nonexistent@example.com"));

        // then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }
}

