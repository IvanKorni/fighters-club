package net.proselyte.gameservice.service;

import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.entity.Match;
import net.proselyte.gameservice.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    private CreateMatchRequest createMatchRequest;
    private Match savedMatch;

    @BeforeEach
    void setUp() {
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        createMatchRequest = new CreateMatchRequest();
        createMatchRequest.setPlayer1Id(player1Id);
        createMatchRequest.setPlayer2Id(player2Id);

        savedMatch = new Match();
        savedMatch.setId(matchId);
        savedMatch.setPlayer1Id(player1Id);
        savedMatch.setPlayer2Id(player2Id);
        savedMatch.setStatus(Match.MatchStatus.WAITING);
        savedMatch.setPlayer1HP(100);
        savedMatch.setPlayer2HP(100);
        savedMatch.setTurnNumber(1);
        savedMatch.setCreated(Instant.now());
        savedMatch.setUpdated(Instant.now());
    }

    @Test
    void shouldCreateMatchWithCorrectInitialValues() {
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(savedMatch.getId());
            match.setCreated(savedMatch.getCreated());
            match.setUpdated(savedMatch.getUpdated());
            return match;
        });

        MatchResponse response = matchService.createMatch(createMatchRequest);

        assertNotNull(response);
        assertEquals(createMatchRequest.getPlayer1Id(), response.getPlayer1Id());
        assertEquals(createMatchRequest.getPlayer2Id(), response.getPlayer2Id());
        assertEquals("WAITING", response.getStatus());
        assertEquals(100, response.getPlayer1HP());
        assertEquals(100, response.getPlayer2HP());
        assertEquals(1, response.getTurnNumber());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdated());

        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void shouldSetCreatedAndUpdatedTimestamps() {
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(savedMatch.getId());
            return match;
        });

        MatchResponse response = matchService.createMatch(createMatchRequest);

        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdated());
        assertEquals(response.getCreatedAt(), response.getUpdated());

        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void shouldCallRepositorySaveOnce() {
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);

        matchService.createMatch(createMatchRequest);

        verify(matchRepository, times(1)).save(any(Match.class));
        verifyNoMoreInteractions(matchRepository);
    }
}

