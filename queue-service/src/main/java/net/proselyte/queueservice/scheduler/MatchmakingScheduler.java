package net.proselyte.queueservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.queueservice.service.MatchmakingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchmakingScheduler {
    
    private final MatchmakingService matchmakingService;
    
    /**
     * Проверяет очередь каждые 5 секунд и пытается найти пары игроков
     */
    @Scheduled(fixedDelay = 5_000) // 5 секунд
    public void processQueue() {
        log.debug("Running matchmaking check");
        
        // Пытаемся создать столько матчей, сколько возможно
        // (если в очереди 4 игрока, создадим 2 матча)
        boolean matched = true;
        int attempts = 0;
        int maxAttempts = 10; // Защита от бесконечного цикла
        
        while (matched && attempts < maxAttempts) {
            matched = matchmakingService.tryMatchPlayers();
            attempts++;
        }
    }
}

