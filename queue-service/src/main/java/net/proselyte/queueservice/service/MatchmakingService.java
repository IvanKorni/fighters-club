package net.proselyte.queueservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.game.dto.CreateMatchRequest;
import net.proselyte.game.dto.MatchResponse;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.queueservice.client.GameServiceClient;
import net.proselyte.queueservice.client.PersonServiceClient;
import net.proselyte.queueservice.repository.RedisQueueRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {
    
    private final RedisQueueRepository redisQueueRepository;
    private final GameServiceClient gameServiceClient;
    private final PersonServiceClient personServiceClient;
    private final WebSocketNotificationService notificationService;
    
    /**
     * Пытается найти пару игроков и создать матч
     * @return true если матч был создан, false если недостаточно игроков
     */
    public boolean tryMatchPlayers() {
        long queueSize = redisQueueRepository.getQueueSize();
        
        if (queueSize < 2) {
            log.debug("Not enough players in queue: {}", queueSize);
            return false;
        }
        
        // Получаем двух самых старых игроков (которые ждут дольше всего)
        Set<String> oldestPlayers = redisQueueRepository.getOldestPlayers(2);
        
        if (oldestPlayers == null || oldestPlayers.size() < 2) {
            log.debug("Could not get 2 players from queue");
            return false;
        }
        
        String[] playerIds = oldestPlayers.toArray(new String[0]);
        UUID player1Id = UUID.fromString(playerIds[0]);
        UUID player2Id = UUID.fromString(playerIds[1]);
        
        log.info("Attempting to match players: {} and {}", player1Id, player2Id);
        
        try {
            // ВАЖНО: Сначала проверяем, что оба игрока существуют в persons-api
            // Это предотвращает создание матчей с несуществующими игроками
            IndividualDto player1;
            IndividualDto player2;
            
            try {
                player1 = personServiceClient.getPersonById(player1Id);
            } catch (Exception e) {
                log.warn("Player {} not found in persons-api, removing from queue: {}", 
                        player1Id, e.getMessage());
                redisQueueRepository.removeFromQueue(player1Id);
                return false;
            }
            
            try {
                player2 = personServiceClient.getPersonById(player2Id);
            } catch (Exception e) {
                log.warn("Player {} not found in persons-api, removing from queue: {}", 
                        player2Id, e.getMessage());
                redisQueueRepository.removeFromQueue(player2Id);
                return false;
            }
            
            // Теперь создаем матч через game-service (оба игрока существуют)
            CreateMatchRequest request = new CreateMatchRequest();
            request.setPlayer1Id(player1Id);
            request.setPlayer2Id(player2Id);
            
            MatchResponse match = gameServiceClient.createMatch(request);
            log.info("Match created: {}", match.getId());
            
            // Удаляем игроков из очереди
            redisQueueRepository.removeMultipleFromQueue(oldestPlayers);
            log.info("Removed players {} and {} from queue", player1Id, player2Id);
            
            // Отправляем уведомления через WebSocket
            notificationService.notifyBothPlayers(
                player1Id, 
                player2Id, 
                match.getId(),
                player1.getNickname(),
                player2.getNickname()
            );
            
            return true;
            
        } catch (Exception e) {
            log.error("Error creating match for players {} and {}: {}", 
                     player1Id, player2Id, e.getMessage(), e);
            // В случае ошибки игроки остаются в очереди
            return false;
        }
    }
}

