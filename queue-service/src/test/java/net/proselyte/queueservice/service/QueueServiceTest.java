package net.proselyte.queueservice.service;

import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.repository.RedisQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueService Unit Tests")
class QueueServiceTest {

    @Mock
    private RedisQueueRepository redisQueueRepository;

    @InjectMocks
    private QueueService queueService;

    private UUID testUserId;
    private long testTimestamp;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTimestamp = Instant.now().toEpochMilli();
    }

    @Test
    @DisplayName("Should successfully add user to queue when user is not in queue")
    void shouldAddUserToQueueWhenNotInQueue() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(false);
        when(redisQueueRepository.addToQueue(eq(testUserId), anyLong())).thenReturn(true);

        // when
        queueService.joinQueue(testUserId);

        // then
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, times(1)).addToQueue(eq(testUserId), anyLong());
    }

    @Test
    @DisplayName("Should not add user to queue when user is already in queue")
    void shouldNotAddUserToQueueWhenAlreadyInQueue() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(true);

        // when
        queueService.joinQueue(testUserId);

        // then
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, never()).addToQueue(any(UUID.class), anyLong());
    }

    @Test
    @DisplayName("Should handle failed add to queue gracefully")
    void shouldHandleFailedAddToQueue() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(false);
        when(redisQueueRepository.addToQueue(eq(testUserId), anyLong())).thenReturn(false);

        // when
        queueService.joinQueue(testUserId);

        // then
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, times(1)).addToQueue(eq(testUserId), anyLong());
        // Service should not throw exception, just log warning
    }

    @Test
    @DisplayName("Should successfully remove user from queue when user is in queue")
    void shouldRemoveUserFromQueueWhenInQueue() {
        // given
        when(redisQueueRepository.removeFromQueue(testUserId)).thenReturn(true);

        // when
        queueService.leaveQueue(testUserId);

        // then
        verify(redisQueueRepository, times(1)).removeFromQueue(testUserId);
    }

    @Test
    @DisplayName("Should handle removal when user is not in queue gracefully")
    void shouldHandleRemovalWhenUserNotInQueue() {
        // given
        when(redisQueueRepository.removeFromQueue(testUserId)).thenReturn(false);

        // when
        queueService.leaveQueue(testUserId);

        // then
        verify(redisQueueRepository, times(1)).removeFromQueue(testUserId);
        // Service should not throw exception, just log warning
    }

    @Test
    @DisplayName("Should return queue status when user is in queue")
    void shouldReturnQueueStatusWhenUserInQueue() {
        // given
        long joinTimestamp = Instant.now().minusSeconds(45).toEpochMilli();
        Long position = 2L;

        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(true);
        when(redisQueueRepository.getJoinTimestamp(testUserId)).thenReturn(joinTimestamp);
        when(redisQueueRepository.getPosition(testUserId)).thenReturn(position);

        // when
        QueueStatusResponseDto result = queueService.getQueueStatus(testUserId);

        // then
        assertNotNull(result);
        assertEquals(QueueStatusResponseDto.StatusEnum.WAITING, result.getStatus());
        assertNotNull(result.getJoinedAt());
        assertTrue(result.getWaitingTime() >= 45);
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, times(1)).getJoinTimestamp(testUserId);
        verify(redisQueueRepository, times(1)).getPosition(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when getting status for user not in queue")
    void shouldThrowExceptionWhenGettingStatusForUserNotInQueue() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(false);

        // when & then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queueService.getQueueStatus(testUserId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found in queue", exception.getReason());
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, never()).getJoinTimestamp(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw exception when join timestamp is null")
    void shouldThrowExceptionWhenJoinTimestampIsNull() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(true);
        when(redisQueueRepository.getJoinTimestamp(testUserId)).thenReturn(null);

        // when & then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> queueService.getQueueStatus(testUserId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found in queue", exception.getReason());
        verify(redisQueueRepository, times(1)).isInQueue(testUserId);
        verify(redisQueueRepository, times(1)).getJoinTimestamp(testUserId);
    }

    @Test
    @DisplayName("Should calculate waiting time correctly")
    void shouldCalculateWaitingTimeCorrectly() {
        // given
        long joinTimestamp = Instant.now().minusSeconds(30).toEpochMilli();
        Long position = 0L;

        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(true);
        when(redisQueueRepository.getJoinTimestamp(testUserId)).thenReturn(joinTimestamp);
        when(redisQueueRepository.getPosition(testUserId)).thenReturn(position);

        // when
        QueueStatusResponseDto result = queueService.getQueueStatus(testUserId);

        // then
        assertNotNull(result);
        assertTrue(result.getWaitingTime() >= 30);
        assertTrue(result.getWaitingTime() < 35); // Allow small time difference
    }

    @Test
    @DisplayName("Should return queue size")
    void shouldReturnQueueSize() {
        // given
        long expectedSize = 5L;
        when(redisQueueRepository.getQueueSize()).thenReturn(expectedSize);

        // when
        long result = queueService.getQueueSize();

        // then
        assertEquals(expectedSize, result);
        verify(redisQueueRepository, times(1)).getQueueSize();
    }

    @Test
    @DisplayName("Should return zero queue size when queue is empty")
    void shouldReturnZeroQueueSizeWhenEmpty() {
        // given
        when(redisQueueRepository.getQueueSize()).thenReturn(0L);

        // when
        long result = queueService.getQueueSize();

        // then
        assertEquals(0L, result);
        verify(redisQueueRepository, times(1)).getQueueSize();
    }

    @Test
    @DisplayName("Should return oldest players from queue")
    void shouldReturnOldestPlayers() {
        // given
        int count = 3;
        Set<String> expectedPlayers = Set.of(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        when(redisQueueRepository.getOldestPlayers(count)).thenReturn(expectedPlayers);

        // when
        Set<String> result = queueService.getOldestPlayers(count);

        // then
        assertNotNull(result);
        assertEquals(expectedPlayers, result);
        assertEquals(count, result.size());
        verify(redisQueueRepository, times(1)).getOldestPlayers(count);
    }

    @Test
    @DisplayName("Should return empty set when no players in queue")
    void shouldReturnEmptySetWhenNoPlayers() {
        // given
        int count = 5;
        when(redisQueueRepository.getOldestPlayers(count)).thenReturn(Set.of());

        // when
        Set<String> result = queueService.getOldestPlayers(count);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(redisQueueRepository, times(1)).getOldestPlayers(count);
    }

    @Test
    @DisplayName("Should handle null position in queue status")
    void shouldHandleNullPositionInQueueStatus() {
        // given
        long joinTimestamp = Instant.now().minusSeconds(10).toEpochMilli();

        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(true);
        when(redisQueueRepository.getJoinTimestamp(testUserId)).thenReturn(joinTimestamp);
        when(redisQueueRepository.getPosition(testUserId)).thenReturn(null);

        // when
        QueueStatusResponseDto result = queueService.getQueueStatus(testUserId);

        // then
        assertNotNull(result);
        assertEquals(QueueStatusResponseDto.StatusEnum.WAITING, result.getStatus());
        assertNotNull(result.getJoinedAt());
        assertTrue(result.getWaitingTime() >= 10);
        verify(redisQueueRepository, times(1)).getPosition(testUserId);
    }

    @Test
    @DisplayName("Should use current timestamp when adding user to queue")
    void shouldUseCurrentTimestampWhenAddingToQueue() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(false);
        when(redisQueueRepository.addToQueue(eq(testUserId), anyLong())).thenReturn(true);

        long beforeAdd = Instant.now().toEpochMilli();

        // when
        queueService.joinQueue(testUserId);

        // then
        long afterAdd = Instant.now().toEpochMilli();
        ArgumentCaptor<Long> timestampCaptor = ArgumentCaptor.forClass(Long.class);
        verify(redisQueueRepository).addToQueue(eq(testUserId), timestampCaptor.capture());
        
        Long capturedTimestamp = timestampCaptor.getValue();
        assertNotNull(capturedTimestamp);
        assertTrue(capturedTimestamp >= beforeAdd && capturedTimestamp <= afterAdd,
                "Timestamp should be between " + beforeAdd + " and " + afterAdd + ", but was " + capturedTimestamp);
    }

    @Test
    @DisplayName("Should handle multiple join attempts for same user")
    void shouldHandleMultipleJoinAttemptsForSameUser() {
        // given
        when(redisQueueRepository.isInQueue(testUserId)).thenReturn(false, true);
        when(redisQueueRepository.addToQueue(eq(testUserId), anyLong())).thenReturn(true);

        // when - first join
        queueService.joinQueue(testUserId);

        // when - second join attempt
        queueService.joinQueue(testUserId);

        // then
        verify(redisQueueRepository, times(2)).isInQueue(testUserId);
        verify(redisQueueRepository, times(1)).addToQueue(eq(testUserId), anyLong());
    }

    @Test
    @DisplayName("Should handle concurrent operations correctly")
    void shouldHandleConcurrentOperations() {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(redisQueueRepository.isInQueue(user1)).thenReturn(false);
        when(redisQueueRepository.isInQueue(user2)).thenReturn(false);
        when(redisQueueRepository.addToQueue(any(UUID.class), anyLong())).thenReturn(true);
        when(redisQueueRepository.removeFromQueue(any(UUID.class))).thenReturn(true);

        // when
        queueService.joinQueue(user1);
        queueService.joinQueue(user2);
        queueService.leaveQueue(user1);

        // then
        verify(redisQueueRepository, times(1)).isInQueue(user1);
        verify(redisQueueRepository, times(1)).isInQueue(user2);
        verify(redisQueueRepository, times(2)).addToQueue(any(UUID.class), anyLong());
        verify(redisQueueRepository, times(1)).removeFromQueue(user1);
    }
}

