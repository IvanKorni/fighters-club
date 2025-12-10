package net.proselyte.queueservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.repository.RedisQueueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisQueueRepository redisQueueRepository;

    /**
     * Adds a user to the queue in Redis (sorted set with timestamp).
     *
     * @param userId the user ID to add to the queue
     */
    public void joinQueue(UUID userId) {
        log.info("User {} joining the queue", userId);
        
        // Check if user is already in queue
        if (redisQueueRepository.isInQueue(userId)) {
            log.warn("User {} is already in the queue", userId);
            return;
        }
        
        // Add to Redis sorted set with current timestamp as score
        long timestamp = Instant.now().toEpochMilli();
        boolean added = redisQueueRepository.addToQueue(userId, timestamp);
        
        if (added) {
            log.info("User {} successfully joined the queue at timestamp {}", userId, timestamp);
        } else {
            log.warn("Failed to add user {} to Redis queue", userId);
        }
    }

    /**
     * Removes a user from the queue in Redis.
     *
     * @param userId the user ID to remove from the queue
     */
    public void leaveQueue(UUID userId) {
        log.info("User {} leaving the queue", userId);
        
        // Remove from Redis sorted set
        boolean removed = redisQueueRepository.removeFromQueue(userId);
        
        if (removed) {
            log.info("User {} successfully left the queue", userId);
        } else {
            log.warn("User {} was not found in the queue", userId);
        }
    }

    /**
     * Gets the queue status for a user.
     * Retrieves data from Redis for real-time information.
     *
     * @param userId the user ID
     * @return queue status response with waiting time and position
     */
    public QueueStatusResponseDto getQueueStatus(UUID userId) {
        // Check if user is in Redis queue
        if (!redisQueueRepository.isInQueue(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in queue");
        }
        
        // Get data from Redis
        Long joinTimestamp = redisQueueRepository.getJoinTimestamp(userId);
        Long position = redisQueueRepository.getPosition(userId);
        
        if (joinTimestamp == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in queue");
        }
        
        QueueStatusResponseDto response = new QueueStatusResponseDto();
        response.setStatus(QueueStatusResponseDto.StatusEnum.WAITING);
        
        Instant joinedAt = Instant.ofEpochMilli(joinTimestamp);
        response.setJoinedAt(joinedAt.atOffset(ZoneOffset.UTC));
        
        // Calculate waiting time in seconds
        long waitingTimeSeconds = Duration.between(joinedAt, Instant.now()).getSeconds();
        response.setWaitingTime((int) waitingTimeSeconds);
        
        // Set position in queue (convert 0-based to 1-based for user-friendly display)
        if (position != null) {
            // Position is already 0-based from Redis, but we might want to show 1-based to users
            // For now, keeping it as-is (0-based)
            // response.setPosition(position.intValue() + 1);
        }
        
        return response;
    }
    
    /**
     * Gets the size of the queue.
     *
     * @return the number of users currently waiting in the queue
     */
    public long getQueueSize() {
        return redisQueueRepository.getQueueSize();
    }
    
    /**
     * Gets the oldest N players from the queue for matchmaking.
     *
     * @param count the number of players to retrieve
     * @return set of user IDs (as strings) ordered by join time (oldest first)
     */
    public java.util.Set<String> getOldestPlayers(int count) {
        return redisQueueRepository.getOldestPlayers(count);
    }
}
