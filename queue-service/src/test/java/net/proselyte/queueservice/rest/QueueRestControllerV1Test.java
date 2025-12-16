package net.proselyte.queueservice.rest;

import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.client.PersonServiceClient;
import net.proselyte.queueservice.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueRestControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("QueueRestControllerV1 Unit Tests")
class QueueRestControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private PersonServiceClient personServiceClient;

    private UUID testUserId;
    private String testEmail;
    private Authentication mockAuthentication;
    private SecurityContext mockSecurityContext;
    private Jwt mockJwt;
    private IndividualPageDto mockPersonPage;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        
        // Setup JWT mock
        mockJwt = mock(Jwt.class);
        when(mockJwt.getClaimAsString("email")).thenReturn(testEmail);
        
        // Setup SecurityContext mock
        mockAuthentication = mock(Authentication.class);
        mockSecurityContext = mock(SecurityContext.class);
        
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(mockJwt);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        
        SecurityContextHolder.setContext(mockSecurityContext);
        
        // Setup PersonServiceClient mock - default response
        mockPersonPage = new IndividualPageDto();
        IndividualDto personDto = new IndividualDto();
        personDto.setId(testUserId);
        personDto.setEmail(testEmail);
        mockPersonPage.setItems(Collections.singletonList(personDto));
        when(personServiceClient.findByEmail(anyList())).thenReturn(mockPersonPage);
    }

    @Test
    @DisplayName("Should successfully join queue and return waiting message")
    void shouldJoinQueueSuccessfully() throws Exception {
        doNothing().when(queueService).joinQueue(testUserId);

        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("waiting"));

        verify(queueService, times(1)).joinQueue(testUserId);
    }

    @Test
    @DisplayName("Should successfully leave queue and return left message")
    void shouldLeaveQueueSuccessfully() throws Exception {
        doNothing().when(queueService).leaveQueue(testUserId);

        mockMvc.perform(post("/v1/queue/leave")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("left"));

        verify(queueService, times(1)).leaveQueue(testUserId);
    }

    @Test
    @DisplayName("Should return queue status successfully")
    void shouldReturnQueueStatusSuccessfully() throws Exception {
        QueueStatusResponseDto statusResponse = new QueueStatusResponseDto();
        statusResponse.setStatus(QueueStatusResponseDto.StatusEnum.WAITING);
        statusResponse.setJoinedAt(OffsetDateTime.ofInstant(Instant.now().minusSeconds(30), ZoneOffset.UTC));
        statusResponse.setWaitingTime(30);

        when(queueService.getQueueStatus(testUserId)).thenReturn(statusResponse);

        mockMvc.perform(get("/v1/queue/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.waitingTime").value(30))
                .andExpect(jsonPath("$.joinedAt").exists());

        verify(queueService, times(1)).getQueueStatus(testUserId);
    }

    @Test
    @DisplayName("Should handle unauthenticated user")
    void shouldHandleUnauthenticatedUser() throws Exception {
        when(mockSecurityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(mockSecurityContext);

        // Without authentication, controller throws ResponseStatusException with 401 UNAUTHORIZED
        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    assertNotNull(result.getResolvedException(),
                            "Exception should be thrown when user is not authenticated");
                    assertTrue(result.getResolvedException() instanceof org.springframework.web.server.ResponseStatusException,
                            "Should throw ResponseStatusException when user is not authenticated");
                });

        verify(queueService, never()).joinQueue(any());
    }

    @Test
    @DisplayName("Should handle authentication with non-UUID principal")
    void shouldHandleNonUuidPrincipal() throws Exception {
        String nonStandardEmail = "testuser@example.com";
        UUID generatedUserId = UUID.nameUUIDFromBytes("testuser".getBytes());
        when(mockJwt.getClaimAsString("email")).thenReturn(nonStandardEmail);

        IndividualPageDto personPage = new IndividualPageDto();
        IndividualDto personDto = new IndividualDto();
        personDto.setId(generatedUserId);
        personDto.setEmail(nonStandardEmail);
        personPage.setItems(Collections.singletonList(personDto));
        when(personServiceClient.findByEmail(anyList())).thenReturn(personPage);

        doNothing().when(queueService).joinQueue(generatedUserId);

        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("waiting"));

        verify(queueService, times(1)).joinQueue(generatedUserId);
    }

    @Test
    @DisplayName("Should extract UUID from principal correctly")
    void shouldExtractUuidFromPrincipal() throws Exception {
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String specificEmail = "specific@example.com";
        when(mockJwt.getClaimAsString("email")).thenReturn(specificEmail);

        IndividualPageDto personPage = new IndividualPageDto();
        IndividualDto personDto = new IndividualDto();
        personDto.setId(specificUserId);
        personDto.setEmail(specificEmail);
        personPage.setItems(Collections.singletonList(personDto));
        when(personServiceClient.findByEmail(anyList())).thenReturn(personPage);

        doNothing().when(queueService).joinQueue(specificUserId);

        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(queueService, times(1)).joinQueue(specificUserId);
    }

    @Test
    @DisplayName("Should handle service exception when getting queue status")
    void shouldHandleServiceExceptionWhenGettingQueueStatus() throws Exception {
        when(queueService.getQueueStatus(testUserId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found in queue"));

        mockMvc.perform(get("/v1/queue/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(queueService, times(1)).getQueueStatus(testUserId);
    }

    @Test
    @DisplayName("Should call service methods with correct user ID for all endpoints")
    void shouldCallServiceMethodsWithCorrectUserId() throws Exception {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String email3 = "user3@example.com";

        when(mockJwt.getClaimAsString("email"))
                .thenReturn(email1)
                .thenReturn(email2)
                .thenReturn(email3);

        IndividualPageDto page1 = new IndividualPageDto();
        IndividualDto dto1 = new IndividualDto();
        dto1.setId(user1);
        dto1.setEmail(email1);
        page1.setItems(Collections.singletonList(dto1));

        IndividualPageDto page2 = new IndividualPageDto();
        IndividualDto dto2 = new IndividualDto();
        dto2.setId(user2);
        dto2.setEmail(email2);
        page2.setItems(Collections.singletonList(dto2));

        IndividualPageDto page3 = new IndividualPageDto();
        IndividualDto dto3 = new IndividualDto();
        dto3.setId(user3);
        dto3.setEmail(email3);
        page3.setItems(Collections.singletonList(dto3));

        when(personServiceClient.findByEmail(anyList()))
                .thenReturn(page1)
                .thenReturn(page2)
                .thenReturn(page3);

        doNothing().when(queueService).joinQueue(any());
        doNothing().when(queueService).leaveQueue(any());

        QueueStatusResponseDto statusResponse = new QueueStatusResponseDto();
        statusResponse.setStatus(QueueStatusResponseDto.StatusEnum.WAITING);
        statusResponse.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
        statusResponse.setWaitingTime(0);
        when(queueService.getQueueStatus(any())).thenReturn(statusResponse);

        mockMvc.perform(post("/v1/queue/join"))
                .andExpect(status().isOk());
        verify(queueService, times(1)).joinQueue(user1);

        mockMvc.perform(post("/v1/queue/leave"))
                .andExpect(status().isOk());
        verify(queueService, times(1)).leaveQueue(user2);

        mockMvc.perform(get("/v1/queue/status"))
                .andExpect(status().isOk());
        verify(queueService, times(1)).getQueueStatus(user3);
    }
}

