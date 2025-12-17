package net.proselyte.queueservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.queueservice.service.MatchmakingService;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchmakingScheduler implements SmartLifecycle {
    
    private final MatchmakingService matchmakingService;
    private final AtomicBoolean running = new AtomicBoolean(true); // Start as true for tests; Spring will manage lifecycle
    
    @Override
    public void start() {
        running.set(true);
        log.info("MatchmakingScheduler started");
    }
    
    @Override
    public void stop() {
        running.set(false);
        log.info("MatchmakingScheduler stopped");
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public int getPhase() {
        // Ensure scheduler stops before Redis connections (default phase is 0)
        // Redis connections typically stop at phase Integer.MAX_VALUE
        // So we use a lower phase to stop earlier
        return 0;
    }
    
    /**
     * Проверяет очередь каждые 5 секунд и пытается найти пары игроков
     */
    @Scheduled(fixedDelay = 5_000) // 5 секунд
    public void processQueue() {
        // Skip processing if scheduler is stopping/stopped
        if (!running.get()) {
            log.debug("Skipping matchmaking check - scheduler is stopping");
            return;
        }
        
        try {
            log.debug("Running matchmaking check");
            
            // Пытаемся создать столько матчей, сколько возможно
            // (если в очереди 4 игрока, создадим 2 матча)
            boolean matched = true;
            int attempts = 0;
            int maxAttempts = 10; // Защита от бесконечного цикла
            
            while (matched && attempts < maxAttempts && running.get()) {
                matched = matchmakingService.tryMatchPlayers();
                attempts++;
            }
        } catch (Exception e) {
            // During shutdown, Redis operations may fail
            if (running.get()) {
                log.error("Error during matchmaking check", e);
            } else {
                log.debug("Matchmaking check interrupted during shutdown");
            }
        }
    }
}

