package net.proselyte.queueservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketNotificationService Unit Tests")
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService notificationService;

    private UUID testUserId1;
    private UUID testUserId2;
    private UUID testMatchId;
    private String testNickname1;
    private String testNickname2;

    @BeforeEach
    void setUp() {
        testUserId1 = UUID.randomUUID();
        testUserId2 = UUID.randomUUID();
        testMatchId = UUID.randomUUID();
        testNickname1 = "Player1";
        testNickname2 = "Player2";
    }

    @Test
    @DisplayName("Should send match found notification to user with correct destination and event")
    void shouldSendMatchFoundNotificationToUser() {
        // given
        String expectedDestination = "/topic/queue/" + testUserId1;

        // when
        notificationService.sendMatchFound(testUserId1, testMatchId, testNickname2);

        // then
        ArgumentCaptor<WebSocketNotificationService.MatchFoundEvent> eventCaptor =
                ArgumentCaptor.forClass(WebSocketNotificationService.MatchFoundEvent.class);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination), eventCaptor.capture());

        WebSocketNotificationService.MatchFoundEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals("match_found", capturedEvent.getType());
        assertEquals(testMatchId, capturedEvent.getMatchId());
        assertEquals(testNickname2, capturedEvent.getOpponentNickname());
    }

    @Test
    @DisplayName("Should notify both players with correct opponent nicknames")
    void shouldNotifyBothPlayersWithCorrectOpponentNicknames() {
        // given
        String expectedDestination1 = "/topic/queue/" + testUserId1;
        String expectedDestination2 = "/topic/queue/" + testUserId2;

        // when
        notificationService.notifyBothPlayers(
                testUserId1,
                testUserId2,
                testMatchId,
                testNickname1,
                testNickname2
        );

        // then
        ArgumentCaptor<WebSocketNotificationService.MatchFoundEvent> eventCaptor =
                ArgumentCaptor.forClass(WebSocketNotificationService.MatchFoundEvent.class);

        // Verify first player notification
        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination1), eventCaptor.capture());

        WebSocketNotificationService.MatchFoundEvent event1 = eventCaptor.getValue();
        assertNotNull(event1);
        assertEquals("match_found", event1.getType());
        assertEquals(testMatchId, event1.getMatchId());
        assertEquals(testNickname2, event1.getOpponentNickname()); // Player1 sees Player2

        // Verify second player notification
        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination2), eventCaptor.capture());

        WebSocketNotificationService.MatchFoundEvent event2 = eventCaptor.getValue();
        assertNotNull(event2);
        assertEquals("match_found", event2.getType());
        assertEquals(testMatchId, event2.getMatchId());
        assertEquals(testNickname1, event2.getOpponentNickname()); // Player2 sees Player1

        // Verify total calls
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(WebSocketNotificationService.MatchFoundEvent.class));
    }

    @Test
    @DisplayName("Should handle null opponent nickname gracefully")
    void shouldHandleNullOpponentNickname() {
        // given
        String expectedDestination = "/topic/queue/" + testUserId1;

        // when
        notificationService.sendMatchFound(testUserId1, testMatchId, null);

        // then
        ArgumentCaptor<WebSocketNotificationService.MatchFoundEvent> eventCaptor =
                ArgumentCaptor.forClass(WebSocketNotificationService.MatchFoundEvent.class);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination), eventCaptor.capture());

        WebSocketNotificationService.MatchFoundEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals("match_found", capturedEvent.getType());
        assertEquals(testMatchId, capturedEvent.getMatchId());
        assertNull(capturedEvent.getOpponentNickname());
    }

    @Test
    @DisplayName("Should send notification with empty opponent nickname")
    void shouldSendNotificationWithEmptyOpponentNickname() {
        // given
        String expectedDestination = "/topic/queue/" + testUserId1;
        String emptyNickname = "";

        // when
        notificationService.sendMatchFound(testUserId1, testMatchId, emptyNickname);

        // then
        ArgumentCaptor<WebSocketNotificationService.MatchFoundEvent> eventCaptor =
                ArgumentCaptor.forClass(WebSocketNotificationService.MatchFoundEvent.class);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination), eventCaptor.capture());

        WebSocketNotificationService.MatchFoundEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals("match_found", capturedEvent.getType());
        assertEquals(testMatchId, capturedEvent.getMatchId());
        assertEquals(emptyNickname, capturedEvent.getOpponentNickname());
    }

    @Test
    @DisplayName("Should create correct destination path for different user IDs")
    void shouldCreateCorrectDestinationPathForDifferentUserIds() {
        // given
        UUID user1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID user2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // when
        notificationService.sendMatchFound(user1, testMatchId, testNickname2);
        notificationService.sendMatchFound(user2, testMatchId, testNickname1);

        // then
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/queue/" + user1), any(WebSocketNotificationService.MatchFoundEvent.class));
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/queue/" + user2), any(WebSocketNotificationService.MatchFoundEvent.class));
    }
}

