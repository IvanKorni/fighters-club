package net.proselyte.queueservice.repository;

import net.proselyte.queueservice.QueueServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = QueueServiceApplication.class)
@Testcontainers
class RedisQueueRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("queue_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "queue");
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private RedisQueueRepository redisQueueRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String QUEUE_KEY = "queue:waiting";

    @BeforeEach
    void setUp() {
        // Очистка Redis перед каждым тестом
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void shouldFindPlayersByScoreRangeWithLimit() {
        // given - создаем игроков с разными timestamp
        long baseTime = System.currentTimeMillis();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();
        UUID player4 = UUID.randomUUID();
        UUID player5 = UUID.randomUUID();

        // Добавляем игроков в очередь с разными timestamp
        // player1: baseTime (входит в диапазон)
        // player2: baseTime + 10000 (входит в диапазон)
        // player3: baseTime + 20000 (входит в диапазон)
        // player4: baseTime + 35000 (НЕ входит в диапазон, > maxScore)
        // player5: baseTime - 5000 (НЕ входит в диапазон, < minScore)
        redisQueueRepository.addToQueue(player1, baseTime);
        redisQueueRepository.addToQueue(player2, baseTime + 10000);
        redisQueueRepository.addToQueue(player3, baseTime + 20000);
        redisQueueRepository.addToQueue(player4, baseTime + 35000);
        redisQueueRepository.addToQueue(player5, baseTime - 5000);

        // when - ищем игроков в диапазоне [baseTime, baseTime + 30000] с лимитом 2
        long minScore = baseTime;
        long maxScore = baseTime + 30000;
        int limit = 2;
        Set<String> result = redisQueueRepository.findPlayersByScoreRange(minScore, maxScore, limit);

        // then - должны получить только 2 игрока (из-за лимита), хотя в диапазоне 3
        assertNotNull(result);
        assertEquals(2, result.size(), "Should return exactly 2 players due to limit");
        
        // Проверяем, что все возвращенные игроки действительно в диапазоне
        assertTrue(result.contains(player1.toString()) || result.contains(player2.toString()) || 
                   result.contains(player3.toString()), "Result should contain players from range");
        
        // Проверяем, что игроки вне диапазона не попали в результат
        assertFalse(result.contains(player4.toString()), "Player4 should not be in result (outside maxScore)");
        assertFalse(result.contains(player5.toString()), "Player5 should not be in result (outside minScore)");
    }

    @Test
    void shouldFindPlayersByScoreRangeWhenLimitIsGreaterThanAvailable() {
        // given - создаем 3 игрока в диапазоне
        long baseTime = System.currentTimeMillis();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();

        redisQueueRepository.addToQueue(player1, baseTime);
        redisQueueRepository.addToQueue(player2, baseTime + 10000);
        redisQueueRepository.addToQueue(player3, baseTime + 20000);

        // when - ищем с лимитом больше, чем доступно игроков
        long minScore = baseTime;
        long maxScore = baseTime + 30000;
        int limit = 10; // больше, чем доступно
        Set<String> result = redisQueueRepository.findPlayersByScoreRange(minScore, maxScore, limit);

        // then - должны получить все 3 игрока
        assertNotNull(result);
        assertEquals(3, result.size(), "Should return all available players when limit is greater");
        assertTrue(result.contains(player1.toString()));
        assertTrue(result.contains(player2.toString()));
        assertTrue(result.contains(player3.toString()));
    }

    @Test
    void shouldReturnEmptySetWhenNoPlayersInScoreRange() {
        // given - создаем игроков вне диапазона
        var baseTime = System.currentTimeMillis();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        redisQueueRepository.addToQueue(player1, baseTime + 20000);
        redisQueueRepository.addToQueue(player2, baseTime - 20000);

        // when - ищем в диапазоне, где нет игроков
        Set<String> result = redisQueueRepository.findPlayersByScoreRange(baseTime, baseTime , 10);

        // then - должен быть пустой результат
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty set when no players in range");
    }

    @Test
    void shouldReturnEmptySetWhenQueueIsEmpty() {
        // given - очередь пуста (уже очищена в setUp)

        // when - ищем в любом диапазоне
        long minScore = System.currentTimeMillis();
        long maxScore = System.currentTimeMillis() + 10000;
        Set<String> result = redisQueueRepository.findPlayersByScoreRange(minScore, maxScore, 10);

        // then - должен быть пустой результат
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty set when queue is empty");
    }

    @Test
    void shouldRespectLimitWhenManyPlayersInRange() {
        // given - создаем много игроков в диапазоне
        long baseTime = System.currentTimeMillis();
        int playersCount = 20;
        
        for (int i = 0; i < playersCount; i++) {
            UUID player = UUID.randomUUID();
            redisQueueRepository.addToQueue(player, baseTime + (i * 1000));
        }

        // when - ищем с лимитом меньше, чем доступно
        long minScore = baseTime;
        long maxScore = baseTime + (playersCount * 1000);
        int limit = 5;
        Set<String> result = redisQueueRepository.findPlayersByScoreRange(minScore, maxScore, limit);

        // then - должны получить ровно limit игроков
        assertNotNull(result);
        assertEquals(limit, result.size(), "Should return exactly limit players");
    }
}

