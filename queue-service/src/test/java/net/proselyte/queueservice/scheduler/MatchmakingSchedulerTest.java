package net.proselyte.queueservice.scheduler;

import net.proselyte.queueservice.service.MatchmakingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingScheduler Unit Tests")
class MatchmakingSchedulerTest {

    @Mock
    private MatchmakingService matchmakingService;

    @InjectMocks
    private MatchmakingScheduler matchmakingScheduler;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(matchmakingService);
    }

    @Test
    @DisplayName("Should call tryMatchPlayers once when no matches can be created")
    void shouldCallTryMatchPlayersOnceWhenNoMatchesCanBeCreated() {
        // given
        when(matchmakingService.tryMatchPlayers()).thenReturn(false);

        // when
        matchmakingScheduler.processQueue();

        // then
        verify(matchmakingService, times(1)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should call tryMatchPlayers multiple times when matches are successfully created")
    void shouldCallTryMatchPlayersMultipleTimesWhenMatchesAreSuccessfullyCreated() {
        // given
        // Первые 3 вызова успешны, 4-й возвращает false (нет больше игроков)
        when(matchmakingService.tryMatchPlayers())
                .thenReturn(true)   // 1-й матч создан
                .thenReturn(true)   // 2-й матч создан
                .thenReturn(true)   // 3-й матч создан
                .thenReturn(false); // Больше нет игроков

        // when
        matchmakingScheduler.processQueue();

        // then
        verify(matchmakingService, times(4)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should stop after maxAttempts even if matches are still being created")
    void shouldStopAfterMaxAttemptsEvenIfMatchesAreStillBeingCreated() {
        // given
        // Всегда возвращаем true, чтобы проверить защиту от бесконечного цикла
        when(matchmakingService.tryMatchPlayers()).thenReturn(true);

        // when
        matchmakingScheduler.processQueue();

        // then
        // maxAttempts = 10, но цикл продолжается пока matched == true
        // После 10 попыток цикл должен остановиться
        verify(matchmakingService, times(10)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should handle single successful match creation")
    void shouldHandleSingleSuccessfulMatchCreation() {
        // given
        when(matchmakingService.tryMatchPlayers())
                .thenReturn(true)   // 1-й матч создан
                .thenReturn(false);  // Больше нет игроков

        // when
        matchmakingScheduler.processQueue();

        // then
        verify(matchmakingService, times(2)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should handle empty queue scenario")
    void shouldHandleEmptyQueueScenario() {
        // given
        when(matchmakingService.tryMatchPlayers()).thenReturn(false);

        // when
        matchmakingScheduler.processQueue();

        // then
        verify(matchmakingService, times(1)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should handle exactly 2 matches creation")
    void shouldHandleExactlyTwoMatchesCreation() {
        // given
        when(matchmakingService.tryMatchPlayers())
                .thenReturn(true)   // 1-й матч создан (2 игрока)
                .thenReturn(true)   // 2-й матч создан (еще 2 игрока)
                .thenReturn(false); // Больше нет игроков

        // when
        matchmakingScheduler.processQueue();

        // then
        verify(matchmakingService, times(3)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should stop when tryMatchPlayers returns false")
    void shouldStopWhenTryMatchPlayersReturnsFalse() {
        // given
        when(matchmakingService.tryMatchPlayers())
                .thenReturn(true)   // 1-й матч создан
                .thenReturn(false); // Недостаточно игроков, цикл останавливается

        // when
        matchmakingScheduler.processQueue();

        // then
        // Цикл продолжается пока matched == true, останавливается на false
        verify(matchmakingService, times(2)).tryMatchPlayers();
    }

    @Test
    @DisplayName("Should respect maxAttempts limit when creating many matches")
    void shouldRespectMaxAttemptsLimitWhenCreatingManyMatches() {
        // given
        // Симулируем ситуацию, когда в очереди очень много игроков
        when(matchmakingService.tryMatchPlayers()).thenReturn(true);

        // when
        matchmakingScheduler.processQueue();

        // then
        // Должно быть ровно 10 попыток (maxAttempts), даже если все успешны
        verify(matchmakingService, times(10)).tryMatchPlayers();
        verifyNoMoreInteractions(matchmakingService);
    }
}

