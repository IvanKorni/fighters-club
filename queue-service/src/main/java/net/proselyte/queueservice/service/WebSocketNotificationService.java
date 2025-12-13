package net.proselyte.queueservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Отправляет уведомление о найденном матче пользователю
     */
    public void sendMatchFound(UUID userId, UUID matchId, String opponentNickname) {
        String destination = "/topic/queue/" + userId;
        
        MatchFoundEvent event = new MatchFoundEvent();
        event.setType("match_found");
        event.setMatchId(matchId);
        event.setOpponentNickname(opponentNickname);
        
        messagingTemplate.convertAndSend(destination, event);
        log.info("Sent match_found event to user {}: matchId={}, opponent={}", 
                userId, matchId, opponentNickname);
    }
    
    /**
     * Уведомляет обоих игроков о найденном матче
     */
    public void notifyBothPlayers(UUID player1Id, UUID player2Id, 
                                   UUID matchId, 
                                   String player1Nickname, 
                                   String player2Nickname) {
        sendMatchFound(player1Id, matchId, player2Nickname);
        sendMatchFound(player2Id, matchId, player1Nickname);
    }
    
    // DTO для события
    public static class MatchFoundEvent {
        private String type;
        private UUID matchId;
        private String opponentNickname;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public UUID getMatchId() { return matchId; }
        public void setMatchId(UUID matchId) { this.matchId = matchId; }
        
        public String getOpponentNickname() { return opponentNickname; }
        public void setOpponentNickname(String opponentNickname) { 
            this.opponentNickname = opponentNickname; 
        }
    }
}

