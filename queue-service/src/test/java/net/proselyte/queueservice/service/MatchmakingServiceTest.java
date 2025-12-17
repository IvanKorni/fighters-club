package net.proselyte.queueservice.service;

import net.proselyte.game.dto.CreateMatchRequest;
import net.proselyte.game.dto.MatchResponse;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.queueservice.client.GameServiceClient;
import net.proselyte.queueservice.client.PersonServiceClient;
import net.proselyte.queueservice.repository.RedisQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingService Unit Tests")
class MatchmakingServiceTest {

    @Mock
    private RedisQueueRepository redisQueueRepository;

    @Mock
    private GameServiceClient gameServiceClient;

    @Mock
    private PersonServiceClient personServiceClient;

    @Mock
    private WebSocketNotificationService notificationService;

    @InjectMocks
    private MatchmakingService matchmakingService;

    private UUID player1Id;
    private UUID player2Id;
    private UUID matchId;
    private String player1Nickname;
    private String player2Nickname;
    private Set<String> oldestPlayers;

    @BeforeEach
    void setUp() {
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        matchId = UUID.randomUUID();
        player1Nickname = "Player1";
        player2Nickname = "Player2";
        oldestPlayers = Set.of(player1Id.toString(), player2Id.toString());
    }

//    @Test
//    @DisplayName("Should successfully match players and create match when queue has 2+ players")
//    void shouldSuccessfullyMatchPlayersWhenQueueHasEnoughPlayers() {
//        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
//        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(oldestPlayers);
//
//        MatchResponse matchResponse = new MatchResponse();
//        matchResponse.setId(matchId);
//
//        IndividualDto player1Dto = new IndividualDto();
//        player1Dto.setNickname(player1Nickname);
//        player1Dto.setEmail("player1@test.com");
//
//        IndividualDto player2Dto = new IndividualDto();
//        player2Dto.setNickname(player2Nickname);
//        player2Dto.setEmail("player2@test.com");
//
//        when(gameServiceClient.createMatch(any(CreateMatchRequest.class))).thenReturn(matchResponse);
//        when(personServiceClient.getPersonById(player1Id)).thenReturn(player1Dto);
//        when(personServiceClient.getPersonById(player2Id)).thenReturn(player2Dto);
//        when(redisQueueRepository.removeMultipleFromQueue(oldestPlayers)).thenReturn(2L);
//
//        boolean result = matchmakingService.tryMatchPlayers();
//
//        assertTrue(result);
//
//        ArgumentCaptor<CreateMatchRequest> requestCaptor = ArgumentCaptor.forClass(CreateMatchRequest.class);
//        verify(gameServiceClient, times(1)).createMatch(requestCaptor.capture());
//
//        CreateMatchRequest capturedRequest = requestCaptor.getValue();
//        assertNotNull(capturedRequest);
//        // Order is not guaranteed due to Set iteration, so check that both IDs are present
//        assertTrue(capturedRequest.getPlayer1Id().equals(player1Id) || capturedRequest.getPlayer1Id().equals(player2Id));
//        assertTrue(capturedRequest.getPlayer2Id().equals(player1Id) || capturedRequest.getPlayer2Id().equals(player2Id));
//        assertNotEquals(capturedRequest.getPlayer1Id(), capturedRequest.getPlayer2Id());
//
//        verify(personServiceClient, times(1)).getPersonById(player1Id);
//        verify(personServiceClient, times(1)).getPersonById(player2Id);
//        verify(redisQueueRepository, times(1)).removeMultipleFromQueue(oldestPlayers);
//        // The order of player IDs depends on Set iteration, so we need to verify with any() for IDs
//        // but we can verify the nicknames match
//        verify(notificationService, times(1)).notifyBothPlayers(
//                any(UUID.class),
//                any(UUID.class),
//                eq(matchId),
//                eq(player1Nickname),
//                eq(player2Nickname)
//        );
//    }

    @Test
    @DisplayName("Should return false when queue has less than 2 players")
    void shouldReturnFalseWhenQueueHasLessThanTwoPlayers() {
        when(redisQueueRepository.getQueueSize()).thenReturn(1L);

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(redisQueueRepository, times(1)).getQueueSize();
        verify(redisQueueRepository, never()).getOldestPlayers(anyInt());
        verify(gameServiceClient, never()).createMatch(any());
        verify(personServiceClient, never()).getPersonById(any());
        verify(notificationService, never()).notifyBothPlayers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return false when queue is empty")
    void shouldReturnFalseWhenQueueIsEmpty() {
        when(redisQueueRepository.getQueueSize()).thenReturn(0L);

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(redisQueueRepository, times(1)).getQueueSize();
        verify(redisQueueRepository, never()).getOldestPlayers(anyInt());
        verify(gameServiceClient, never()).createMatch(any());
    }

    @Test
    @DisplayName("Should return false when cannot get 2 players from queue")
    void shouldReturnFalseWhenCannotGetTwoPlayersFromQueue() {
        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(Set.of(player1Id.toString()));

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(redisQueueRepository, times(1)).getQueueSize();
        verify(redisQueueRepository, times(1)).getOldestPlayers(2);
        verify(gameServiceClient, never()).createMatch(any());
        verify(personServiceClient, never()).getPersonById(any());
    }

    @Test
    @DisplayName("Should return false and keep players in queue when match creation fails")
    void shouldReturnFalseAndKeepPlayersInQueueWhenMatchCreationFails() {
        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(oldestPlayers);

        IndividualDto player1Dto = new IndividualDto();
        player1Dto.setNickname(player1Nickname);
        player1Dto.setEmail("player1@test.com");

        IndividualDto player2Dto = new IndividualDto();
        player2Dto.setNickname(player2Nickname);
        player2Dto.setEmail("player2@test.com");

        when(personServiceClient.getPersonById(player1Id)).thenReturn(player1Dto);
        when(personServiceClient.getPersonById(player2Id)).thenReturn(player2Dto);
        when(gameServiceClient.createMatch(any(CreateMatchRequest.class)))
                .thenThrow(new RuntimeException("Game service unavailable"));

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(personServiceClient, times(1)).getPersonById(player1Id);
        verify(personServiceClient, times(1)).getPersonById(player2Id);
        verify(gameServiceClient, times(1)).createMatch(any(CreateMatchRequest.class));
        verify(redisQueueRepository, never()).removeMultipleFromQueue(any());
        verify(notificationService, never()).notifyBothPlayers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return false and remove player1 from queue when player1 not found")
    void shouldReturnFalseAndRemovePlayer1FromQueueWhenPlayer1NotFound() {
        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(oldestPlayers);
        when(redisQueueRepository.removeFromQueue(any(UUID.class))).thenReturn(true);

        // Make player1Id fail when looked up
        when(personServiceClient.getPersonById(player1Id))
                .thenThrow(new RuntimeException("Person service unavailable"));
        // Stub player2Id with lenient to avoid unnecessary stubbing exception
        // (it won't be called if player1Id is processed first and fails)
        lenient().when(personServiceClient.getPersonById(player2Id))
                .thenReturn(new IndividualDto());

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        // Verify that getPersonById was called at least once (for the first player processed)
        verify(personServiceClient, atLeastOnce()).getPersonById(any(UUID.class));
        verify(gameServiceClient, never()).createMatch(any(CreateMatchRequest.class));
        // Verify that removeFromQueue was called with the UUID that failed
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(redisQueueRepository, times(1)).removeFromQueue(uuidCaptor.capture());
        UUID removedPlayerId = uuidCaptor.getValue();
        // The removed player should be player1Id (the one that failed)
        assertEquals(player1Id, removedPlayerId);
        verify(redisQueueRepository, never()).removeMultipleFromQueue(any());
        verify(notificationService, never()).notifyBothPlayers(any(), any(), any(), any(), any());
    }

//    @Test
//    @DisplayName("Should return false and remove player2 from queue when player2 not found")
//    void shouldReturnFalseAndRemovePlayer2FromQueueWhenPlayer2NotFound() {
//        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
//        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(oldestPlayers);
//        when(redisQueueRepository.removeFromQueue(any(UUID.class))).thenReturn(true);
//
//        IndividualDto player1Dto = new IndividualDto();
//        player1Dto.setNickname(player1Nickname);
//        player1Dto.setEmail("player1@test.com");
//
//        when(personServiceClient.getPersonById(player1Id)).thenReturn(player1Dto);
//        when(personServiceClient.getPersonById(player2Id))
//                .thenThrow(new RuntimeException("Person service unavailable"));
//
//        boolean result = matchmakingService.tryMatchPlayers();
//
//        assertFalse(result);
//        verify(personServiceClient, times(1)).getPersonById(player1Id);
//        verify(personServiceClient, times(1)).getPersonById(player2Id);
//        verify(gameServiceClient, never()).createMatch(any(CreateMatchRequest.class));
//        // Verify that removeFromQueue was called with the UUID that failed
//        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
//        verify(redisQueueRepository, times(1)).removeFromQueue(uuidCaptor.capture());
//        UUID removedPlayerId = uuidCaptor.getValue();
//        // The removed player should be player2Id (the one that failed)
//        assertEquals(player2Id, removedPlayerId);
//        verify(redisQueueRepository, never()).removeMultipleFromQueue(any());
//        verify(notificationService, never()).notifyBothPlayers(any(), any(), any(), any(), any());
//    }

//    @Test
//    @DisplayName("Should successfully match players when queue has more than 2 players")
//    void shouldSuccessfullyMatchPlayersWhenQueueHasMoreThanTwoPlayers() {
//        when(redisQueueRepository.getQueueSize()).thenReturn(5L);
//        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(oldestPlayers);
//
//        MatchResponse matchResponse = new MatchResponse();
//        matchResponse.setId(matchId);
//
//        IndividualDto player1Dto = new IndividualDto();
//        player1Dto.setNickname(player1Nickname);
//        player1Dto.setEmail("player1@test.com");
//
//        IndividualDto player2Dto = new IndividualDto();
//        player2Dto.setNickname(player2Nickname);
//        player2Dto.setEmail("player2@test.com");
//
//        when(gameServiceClient.createMatch(any(CreateMatchRequest.class))).thenReturn(matchResponse);
//        when(personServiceClient.getPersonById(player1Id)).thenReturn(player1Dto);
//        when(personServiceClient.getPersonById(player2Id)).thenReturn(player2Dto);
//        when(redisQueueRepository.removeMultipleFromQueue(oldestPlayers)).thenReturn(2L);
//
//        boolean result = matchmakingService.tryMatchPlayers();
//
//        assertTrue(result);
//        verify(redisQueueRepository, times(1)).getQueueSize();
//        verify(redisQueueRepository, times(1)).getOldestPlayers(2);
//        verify(gameServiceClient, times(1)).createMatch(any(CreateMatchRequest.class));
//        verify(redisQueueRepository, times(1)).removeMultipleFromQueue(oldestPlayers);
//        // The order of player IDs depends on Set iteration, so we need to verify with any() for IDs
//        // but we can verify the nicknames match
//        verify(notificationService, times(1)).notifyBothPlayers(
//                any(UUID.class),
//                any(UUID.class),
//                eq(matchId),
//                eq(player1Nickname),
//                eq(player2Nickname)
//        );
//    }

    @Test
    @DisplayName("Should correctly parse player IDs from string set")
    void shouldCorrectlyParsePlayerIdsFromStringSet() {
        // given
        UUID player3Id = UUID.randomUUID();
        UUID player4Id = UUID.randomUUID();
        Set<String> players = Set.of(player3Id.toString(), player4Id.toString());

        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(players);

        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setId(matchId);

        IndividualDto player3Dto = new IndividualDto();
        player3Dto.setNickname("Player3");
        player3Dto.setEmail("player3@test.com");

        IndividualDto player4Dto = new IndividualDto();
        player4Dto.setNickname("Player4");
        player4Dto.setEmail("player4@test.com");

        when(gameServiceClient.createMatch(any(CreateMatchRequest.class))).thenReturn(matchResponse);
        when(personServiceClient.getPersonById(player3Id)).thenReturn(player3Dto);
        when(personServiceClient.getPersonById(player4Id)).thenReturn(player4Dto);
        when(redisQueueRepository.removeMultipleFromQueue(players)).thenReturn(2L);

        boolean result = matchmakingService.tryMatchPlayers();

        assertTrue(result);

        ArgumentCaptor<CreateMatchRequest> requestCaptor = ArgumentCaptor.forClass(CreateMatchRequest.class);
        verify(gameServiceClient, times(1)).createMatch(requestCaptor.capture());

        CreateMatchRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        // Проверяем, что ID были правильно распарсены (порядок может быть разным из-за Set)
        assertTrue(capturedRequest.getPlayer1Id().equals(player3Id) || capturedRequest.getPlayer1Id().equals(player4Id));
        assertTrue(capturedRequest.getPlayer2Id().equals(player3Id) || capturedRequest.getPlayer2Id().equals(player4Id));
        assertNotEquals(capturedRequest.getPlayer1Id(), capturedRequest.getPlayer2Id());
    }

    @Test
    @DisplayName("Should handle empty set from getOldestPlayers")
    void shouldHandleEmptySetFromGetOldestPlayers() {
        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(Set.of());

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(redisQueueRepository, times(1)).getQueueSize();
        verify(redisQueueRepository, times(1)).getOldestPlayers(2);
        verify(gameServiceClient, never()).createMatch(any());
    }

    @Test
    @DisplayName("Should handle null set from getOldestPlayers")
    void shouldHandleNullSetFromGetOldestPlayers() {
        when(redisQueueRepository.getQueueSize()).thenReturn(2L);
        when(redisQueueRepository.getOldestPlayers(2)).thenReturn(null);

        boolean result = matchmakingService.tryMatchPlayers();

        assertFalse(result);
        verify(redisQueueRepository, times(1)).getQueueSize();
        verify(redisQueueRepository, times(1)).getOldestPlayers(2);
        verify(gameServiceClient, never()).createMatch(any());
    }
}

