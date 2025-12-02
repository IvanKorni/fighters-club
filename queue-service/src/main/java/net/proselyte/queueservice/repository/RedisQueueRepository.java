package net.proselyte.queueservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

/**
 * Repository for managing queue in Redis using Sorted Set.
 * Uses timestamp as score to maintain chronological order of players.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueueRepository {

    private static final String QUEUE_KEY = "queue:waiting";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Adds a user to the queue with current timestamp as score.
     *
     * @param userId the user ID to add
     * @param timestamp the timestamp (score) in milliseconds
     * @return true if the user was added, false if already exists
     */
    public boolean addToQueue(UUID userId, long timestamp) {
        String userIdStr = userId.toString();
        Boolean added = redisTemplate.opsForZSet().add(QUEUE_KEY, userIdStr, timestamp);
        boolean result = Boolean.TRUE.equals(added);
        log.debug("User {} {} to queue with timestamp {}", userId, result ? "added" : "already exists", timestamp);
        return result;
    }

    /**
     * Removes a user from the queue.
     *
     * @param userId the user ID to remove
     * @return true if the user was removed, false if not found
     */
    public boolean removeFromQueue(UUID userId) {
        String userIdStr = userId.toString();
        Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, userIdStr);
        boolean result = removed != null && removed > 0;
        log.debug("User {} {} from queue", userId, result ? "removed" : "not found");
        return result;
    }

    /**
     * Checks if a user is in the queue.
     *
     * @param userId the user ID to check
     * @return true if the user is in the queue, false otherwise
     */
    public boolean isInQueue(UUID userId) {
        String userIdStr = userId.toString();
        Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, userIdStr);
        boolean result = score != null;
        log.debug("User {} {} in queue", userId, result ? "is" : "is not");
        return result;
    }

    /**
     * Gets the timestamp (score) when the user joined the queue.
     *
     * @param userId the user ID
     * @return the timestamp in milliseconds, or null if user is not in queue
     */
    public Long getJoinTimestamp(UUID userId) {
        String userIdStr = userId.toString();
        Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, userIdStr);
        return score != null ? score.longValue() : null;
    }

    /**
     * Gets the position (rank) of the user in the queue.
     * Position is 0-based (0 = first in queue).
     *
     * @param userId the user ID
     * @return the position (rank), or null if user is not in queue
     */
    public Long getPosition(UUID userId) {
        String userIdStr = userId.toString();
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userIdStr);
        if (rank != null) {
            log.debug("User {} position in queue: {}", userId, rank);
        }
        return rank;
    }

    /**
     * Gets the size of the queue.
     *
     * @return the number of users in the queue
     */
    public long getQueueSize() {
        Long size = redisTemplate.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    /**
     * Gets the oldest N players from the queue (lowest scores = earliest timestamps).
     * Useful for matchmaking to find players who have been waiting the longest.
     *
     * @param count the number of players to retrieve
     * @return set of user IDs (as strings) ordered by join time (oldest first)
     */
    public Set<String> getOldestPlayers(int count) {
        Set<String> players = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, count - 1);
        log.debug("Retrieved {} oldest players from queue", players != null ? players.size() : 0);
        return players;
    }

    /**
     * Gets players within a score range (timestamp range).
     * Useful for finding players who joined within a specific time window.
     *
     * @param minScore minimum timestamp (inclusive)
     * @param maxScore maximum timestamp (inclusive)
     * @return set of user IDs (as strings) within the score range
     */
    public Set<String> getPlayersByScoreRange(long minScore, long maxScore) {
        Set<String> players = redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, minScore, maxScore);
        log.debug("Retrieved {} players from queue with scores between {} and {}", 
                players != null ? players.size() : 0, minScore, maxScore);
        return players;
    }

    /**
     * Removes multiple users from the queue.
     *
     * @param userIds set of user IDs to remove
     * @return number of users removed
     */
    public long removeMultipleFromQueue(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0L;
        }
        Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, userIds.toArray(new Object[0]));
        long result = removed != null ? removed : 0L;
        log.debug("Removed {} users from queue", result);
        return result;
    }

    /**
     * Gets all players in the queue.
     *
     * @return set of all user IDs (as strings) in the queue
     */
    public Set<String> getAllPlayers() {
        Set<String> players = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, -1);
        return players != null ? players : Set.of();
    }

    /**
     * Gets players with their scores (timestamps).
     *
     * @param start start index (0-based)
     * @param end end index (-1 for all)
     * @return set of tuples containing user ID and score
     */
    public Set<ZSetOperations.TypedTuple<String>> getPlayersWithScores(long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> players = redisTemplate.opsForZSet()
                .rangeWithScores(QUEUE_KEY, start, end);
        return players != null ? players : Set.of();
    }
}

