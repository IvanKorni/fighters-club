package net.proselyte.queueservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;

/**
 * Репозиторий для управления очередью в Redis с использованием Sorted Set.
 * Использует timestamp в качестве score для поддержания хронологического порядка игроков.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueueRepository {

    private static final String QUEUE_KEY = "queue:waiting";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Добавляет пользователя в очередь с текущим timestamp в качестве score.
     *
     * @param userId идентификатор пользователя для добавления
     * @param timestamp timestamp (score) в миллисекундах
     * @return true если пользователь был добавлен, false если уже существует
     */
    public boolean addToQueue(UUID userId, long timestamp) {
        try {
            String userIdStr = userId.toString();
            Boolean added = redisTemplate.opsForZSet().add(QUEUE_KEY, userIdStr, timestamp);
            boolean result = Boolean.TRUE.equals(added);
            log.debug("User {} {} to queue with timestamp {}", userId, result ? "added" : "already exists", timestamp);
            return result;
        } catch (DataAccessException | CancellationException e) {
            log.warn("Failed to add user {} to queue (likely during shutdown): {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Удаляет пользователя из очереди.
     *
     * @param userId идентификатор пользователя для удаления
     * @return true если пользователь был удален, false если не найден
     */
    public boolean removeFromQueue(UUID userId) {
        try {
            String userIdStr = userId.toString();
            Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, userIdStr);
            boolean result = removed != null && removed > 0;
            log.debug("User {} {} from queue", userId, result ? "removed" : "not found");
            return result;
        } catch (DataAccessException | CancellationException e) {
            log.debug("Failed to remove user {} from queue (likely during shutdown): {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, находится ли пользователь в очереди.
     *
     * @param userId идентификатор пользователя для проверки
     * @return true если пользователь в очереди, false в противном случае
     */
    public boolean isInQueue(UUID userId) {
        String userIdStr = userId.toString();
        Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, userIdStr);
        boolean result = score != null;
        log.debug("User {} {} in queue", userId, result ? "is" : "is not");
        return result;
    }

    /**
     * Получает timestamp (score) когда пользователь присоединился к очереди.
     *
     * @param userId идентификатор пользователя
     * @return timestamp в миллисекундах, или null если пользователь не в очереди
     */
    public Long getJoinTimestamp(UUID userId) {
        String userIdStr = userId.toString();
        Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, userIdStr);
        return score != null ? score.longValue() : null;
    }

    /**
     * Получает позицию (ранг) пользователя в очереди.
     * Позиция начинается с 0 (0 = первый в очереди).
     *
     * @param userId идентификатор пользователя
     * @return позиция (ранг), или null если пользователь не в очереди
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
     * Получает размер очереди.
     *
     * @return количество пользователей в очереди
     */
    public long getQueueSize() {
        try {
            Long size = redisTemplate.opsForZSet().zCard(QUEUE_KEY);
            return size != null ? size : 0L;
        } catch (DataAccessException | CancellationException e) {
            // During shutdown, Redis operations may be cancelled
            log.debug("Redis operation cancelled (likely during shutdown): {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Получает N самых старых игроков из очереди (низшие score = самые ранние timestamp).
     * Полезно для подбора соперников, чтобы найти игроков, которые ждут дольше всего.
     *
     * @param count количество игроков для получения
     * @return множество идентификаторов пользователей (в виде строк), упорядоченных по времени присоединения (самые старые первыми)
     */
    public Set<String> getOldestPlayers(int count) {
        try {
            Set<String> players = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, count - 1);
            log.debug("Retrieved {} oldest players from queue", players != null ? players.size() : 0);
            return players != null ? players : Set.of();
        } catch (DataAccessException | CancellationException e) {
            log.debug("Failed to get oldest players (likely during shutdown): {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Удаляет нескольких пользователей из очереди.
     *
     * @param userIds множество идентификаторов пользователей для удаления
     * @return количество удаленных пользователей
     */
    public long removeMultipleFromQueue(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0L;
        }
        try {
            Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, userIds.toArray(new Object[0]));
            long result = removed != null ? removed : 0L;
            log.debug("Removed {} users from queue", result);
            return result;
        } catch (DataAccessException | CancellationException e) {
            log.debug("Failed to remove users from queue (likely during shutdown): {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Получает игроков с их score (timestamp).
     *
     * @param start начальный индекс (начиная с 0)
     * @param end конечный индекс (-1 для всех)
     * @return множество кортежей, содержащих идентификатор пользователя и score
     */
    public Set<ZSetOperations.TypedTuple<String>> getPlayersWithScores(long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> players = redisTemplate.opsForZSet()
                .rangeWithScores(QUEUE_KEY, start, end);
        return players != null ? players : Set.of();
    }

    public Set<String> findPlayersByScoreRange(long minScore, long maxScore, int limit) {
        Set<String> players = redisTemplate.opsForZSet()
                .rangeByScore(QUEUE_KEY, minScore, maxScore, 0, limit);
        return players != null ? players : Set.of();
    }
}

