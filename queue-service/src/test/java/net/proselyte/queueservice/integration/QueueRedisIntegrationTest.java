package net.proselyte.queueservice.integration;

import net.proselyte.queueservice.QueueServiceApplication;
import net.proselyte.queueservice.entity.QueueItem;
import net.proselyte.queueservice.repository.QueueItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = QueueServiceApplication.class)
@Testcontainers
@Transactional
class QueueRedisIntegrationTest {

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
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private QueueItemRepository queueItemRepository;

    private static final String QUEUE_KEY = "queue:waiting";
    private static final String QUEUE_POSITION_KEY_PREFIX = "queue:position:";

    @BeforeEach
    void setUp() {
        // Очистка Redis
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        // Очистка PostgreSQL
        queueItemRepository.deleteAll();
    }

    @Test
    void shouldAddUserToQueueInRedisAndPostgreSQL() {
        // given
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        
        // when - добавляем в PostgreSQL
        QueueItem queueItem = new QueueItem();
        queueItem.setUserId(userId);
        queueItem.setJoinedAt(Instant.now());
        queueItem.setStatus(QueueItem.QueueStatus.WAITING);
        queueItemRepository.save(queueItem);
        
        // добавляем в Redis (симуляция логики очереди)
        redisTemplate.opsForZSet().add(QUEUE_KEY, userIdStr, System.currentTimeMillis());
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + userIdStr, "1");
        
        // then - проверяем PostgreSQL
        QueueItem saved = queueItemRepository.findByUserId(userId).orElseThrow();
        assertEquals(QueueItem.QueueStatus.WAITING, saved.getStatus());
        assertNotNull(saved.getJoinedAt());
        
        // проверяем Redis
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userIdStr);
        assertNotNull(rank);
        assertEquals(0L, rank); // первый в очереди
        
        String position = redisTemplate.opsForValue().get(QUEUE_POSITION_KEY_PREFIX + userIdStr);
        assertEquals("1", position);
    }

    @Test
    void shouldMaintainQueueOrderInRedis() {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        
        // when - добавляем пользователей в очередь с задержкой
        long time1 = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(QUEUE_KEY, user1.toString(), time1);
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + user1.toString(), "1");
        
        long time2 = time1 + 100;
        redisTemplate.opsForZSet().add(QUEUE_KEY, user2.toString(), time2);
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + user2.toString(), "2");
        
        long time3 = time2 + 100;
        redisTemplate.opsForZSet().add(QUEUE_KEY, user3.toString(), time3);
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + user3.toString(), "3");
        
        // then - проверяем порядок
        Long rank1 = redisTemplate.opsForZSet().rank(QUEUE_KEY, user1.toString());
        Long rank2 = redisTemplate.opsForZSet().rank(QUEUE_KEY, user2.toString());
        Long rank3 = redisTemplate.opsForZSet().rank(QUEUE_KEY, user3.toString());
        
        assertNotNull(rank1);
        assertNotNull(rank2);
        assertNotNull(rank3);
        assertTrue(rank1 < rank2);
        assertTrue(rank2 < rank3);
        
        // проверяем размер очереди
        Long queueSize = redisTemplate.opsForZSet().size(QUEUE_KEY);
        assertEquals(3L, queueSize);
    }

    @Test
    void shouldRemoveUserFromQueueInRedisAndPostgreSQL() {
        // given
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        
        // добавляем в PostgreSQL
        QueueItem queueItem = new QueueItem();
        queueItem.setUserId(userId);
        queueItem.setJoinedAt(Instant.now());
        queueItem.setStatus(QueueItem.QueueStatus.WAITING);
        queueItemRepository.save(queueItem);
        
        // добавляем в Redis
        redisTemplate.opsForZSet().add(QUEUE_KEY, userIdStr, System.currentTimeMillis());
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + userIdStr, "1");
        
        // when - удаляем из Redis
        redisTemplate.opsForZSet().remove(QUEUE_KEY, userIdStr);
        redisTemplate.delete(QUEUE_POSITION_KEY_PREFIX + userIdStr);
        
        // обновляем статус в PostgreSQL
        QueueItem item = queueItemRepository.findByUserId(userId).orElseThrow();
        item.setStatus(QueueItem.QueueStatus.LEFT);
        queueItemRepository.save(item);
        
        // then - проверяем Redis
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userIdStr);
        assertNull(rank);
        
        String position = redisTemplate.opsForValue().get(QUEUE_POSITION_KEY_PREFIX + userIdStr);
        assertNull(position);
        
        // проверяем PostgreSQL
        QueueItem updated = queueItemRepository.findByUserId(userId).orElseThrow();
        assertEquals(QueueItem.QueueStatus.LEFT, updated.getStatus());
    }

    @Test
    void shouldGetNextUserFromQueue() {
        // given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        
        long time1 = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(QUEUE_KEY, user1.toString(), time1);
        redisTemplate.opsForZSet().add(QUEUE_KEY, user2.toString(), time1 + 100);
        redisTemplate.opsForZSet().add(QUEUE_KEY, user3.toString(), time1 + 200);
        
        // when - получаем первого пользователя из очереди
        Set<String> firstUser = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, 0);
        
        // then
        assertNotNull(firstUser);
        assertEquals(1, firstUser.size());
        assertEquals(user1.toString(), firstUser.iterator().next());
    }

    @Test
    void shouldSynchronizeRedisAndPostgreSQL() {
        // given
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        
        // when - создаем запись в PostgreSQL
        QueueItem queueItem = new QueueItem();
        queueItem.setUserId(userId);
        queueItem.setJoinedAt(Instant.now());
        queueItem.setStatus(QueueItem.QueueStatus.WAITING);
        QueueItem saved = queueItemRepository.save(queueItem);
        
        // добавляем в Redis
        redisTemplate.opsForZSet().add(QUEUE_KEY, userIdStr, saved.getJoinedAt().toEpochMilli());
        redisTemplate.opsForValue().set(QUEUE_POSITION_KEY_PREFIX + userIdStr, "1");
        
        // then - проверяем синхронизацию
        QueueItem fromDb = queueItemRepository.findByUserId(userId).orElseThrow();
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userIdStr);
        
        assertNotNull(fromDb);
        assertNotNull(rank);
        assertEquals(QueueItem.QueueStatus.WAITING, fromDb.getStatus());
        
        // проверяем, что время совпадает (с небольшой погрешностью)
        long dbTime = fromDb.getJoinedAt().toEpochMilli();
        Double redisScore = redisTemplate.opsForZSet().score(QUEUE_KEY, userIdStr);
        assertNotNull(redisScore);
        long redisTime = redisScore.longValue();
        
        // разница не должна быть больше 1 секунды
        assertTrue(Math.abs(dbTime - redisTime) < 1000);
    }

    @Test
    void shouldHandleEmptyQueue() {
        // when & then
        Long queueSize = redisTemplate.opsForZSet().size(QUEUE_KEY);
        assertEquals(0L, queueSize);
        
        Set<String> users = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, -1);
        assertTrue(users == null || users.isEmpty());
    }
}
