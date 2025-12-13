package net.proselyte.queueservice.rest;

import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueRestControllerV1.class)
@DisplayName("QueueRestControllerV1 Unit Tests")
class QueueRestControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    private UUID testUserId;
    private Authentication mockAuthentication;
    private SecurityContext mockSecurityContext;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Setup SecurityContext mock
        mockAuthentication = mock(Authentication.class);
        mockSecurityContext = mock(SecurityContext.class);
        
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn(testUserId.toString());
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        
        SecurityContextHolder.setContext(mockSecurityContext);
    }

    @Test
    @DisplayName("Should successfully join queue and return waiting message")
    void shouldJoinQueueSuccessfully() throws Exception {
        // given
        doNothing().when(queueService).joinQueue(testUserId);

        // when & then
        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("waiting"));

        verify(queueService, times(1)).joinQueue(testUserId);
    }

    @Test
    @DisplayName("Should successfully leave queue and return left message")
    void shouldLeaveQueueSuccessfully() throws Exception {
        // given
        doNothing().when(queueService).leaveQueue(testUserId);

        // when & then
        mockMvc.perform(post("/v1/queue/leave")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("left"));

        verify(queueService, times(1)).leaveQueue(testUserId);
    }

    @Test
    @DisplayName("Should return queue status successfully")
    void shouldReturnQueueStatusSuccessfully() throws Exception {
        // given
        QueueStatusResponseDto statusResponse = new QueueStatusResponseDto();
        statusResponse.setStatus(QueueStatusResponseDto.StatusEnum.WAITING);
        statusResponse.setJoinedAt(OffsetDateTime.ofInstant(Instant.now().minusSeconds(30), ZoneOffset.UTC));
        statusResponse.setWaitingTime(30);

        when(queueService.getQueueStatus(testUserId)).thenReturn(statusResponse);

        // when & then
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
        // given
        when(mockSecurityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(mockSecurityContext);

        // when & then
        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(queueService, never()).joinQueue(any());
    }

    @Test
    @DisplayName("Should handle authentication with non-UUID principal")
    void shouldHandleNonUuidPrincipal() throws Exception {
        // given
        UUID generatedUserId = UUID.nameUUIDFromBytes("testuser".getBytes());
        when(mockAuthentication.getName()).thenReturn("testuser");
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        doNothing().when(queueService).joinQueue(generatedUserId);

        // when & then
        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("waiting"));

        verify(queueService, times(1)).joinQueue(generatedUserId);
    }

    @Test
    @DisplayName("Should extract UUID from principal correctly")
    void shouldExtractUuidFromPrincipal() throws Exception {
        // given
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(mockAuthentication.getName()).thenReturn(specificUserId.toString());
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        doNothing().when(queueService).joinQueue(specificUserId);

        // when & then
        mockMvc.perform(post("/v1/queue/join")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(queueService, times(1)).joinQueue(specificUserId);
    }

    @Test
    @DisplayName("Should handle service exception when getting queue status")
    void shouldHandleServiceExceptionWhenGettingQueueStatus() throws Exception {
        // given
        when(queueService.getQueueStatus(testUserId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found in queue"));

        // when & then
        mockMvc.perform(get("/v1/queue/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(queueService, times(1)).getQueueStatus(testUserId);
    }

    @Test
    @DisplayName("Should call service methods with correct user ID for all endpoints")
    void shouldCallServiceMethodsWithCorrectUserId() throws Exception {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        when(mockAuthentication.getName())
                .thenReturn(user1.toString())
                .thenReturn(user2.toString())
                .thenReturn(user3.toString());

        doNothing().when(queueService).joinQueue(any());
        doNothing().when(queueService).leaveQueue(any());
        
        QueueStatusResponseDto statusResponse = new QueueStatusResponseDto();
        statusResponse.setStatus(QueueStatusResponseDto.StatusEnum.WAITING);
        statusResponse.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
        statusResponse.setWaitingTime(0);
        when(queueService.getQueueStatus(any())).thenReturn(statusResponse);

        // when & then
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

