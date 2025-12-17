package net.proselyte.gameservice.service;

import net.proselyte.game.dto.MoveRequest;
import net.proselyte.game.dto.MoveResponse;
import net.proselyte.gameservice.entity.Match;
import net.proselyte.gameservice.entity.Move;
import net.proselyte.gameservice.exception.InvalidTurnNumberException;
import net.proselyte.gameservice.exception.MatchNotFoundException;
import net.proselyte.gameservice.exception.MatchFinishedException;
import net.proselyte.gameservice.exception.MoveAlreadyExistsException;
import net.proselyte.gameservice.exception.PlayerNotParticipantException;
import net.proselyte.gameservice.repository.MatchRepository;
import net.proselyte.gameservice.repository.MoveRepository;
import net.proselyte.gameservice.service.move.MoveFactory;
import net.proselyte.gameservice.service.move.MoveTargetMapper;
import net.proselyte.gameservice.service.move.MoveValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты для MoveService - сервиса обработки ходов игроков
 */
@ExtendWith(MockitoExtension.class)
class MoveServiceTest {

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private MatchRepository matchRepository;

    private MoveService moveService;

    // Тестовые данные
    private UUID matchId;
    private UUID player1Id;
    private UUID player2Id;
    private UUID moveId;
    private Match match;
    private MoveRequest moveRequest;

    /**
     * Подготовка тестовых данных перед каждым тестом
     */
    @BeforeEach
    void setUp() {
        // Генерируем UUID для тестовых сущностей
        matchId = UUID.randomUUID();
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        moveId = UUID.randomUUID();

        // Создаем матч в статусе WAITING
        match = new Match();
        match.setId(matchId);
        match.setPlayer1Id(player1Id);
        match.setPlayer2Id(player2Id);
        match.setStatus(Match.MatchStatus.WAITING);
        match.setPlayer1HP(100);
        match.setPlayer2HP(100);
        match.setTurnNumber(1);
        match.setCreated(Instant.now());
        match.setUpdated(Instant.now());

        // Создаем запрос на ход
        // Используем рефлексию для установки enum значений (работает до и после генерации OpenAPI)
        moveRequest = new MoveRequest();
        moveRequest.setMatchId(matchId);
        setEnumValue(moveRequest, "setAttackTarget", "HEAD");
        setEnumValue(moveRequest, "setDefenseTarget", "BODY");
        moveRequest.setTurnNumber(1);

        // Собираем MoveService с реальными валидатором/маппером (репозитории замоканы)
        MoveTargetMapper targetMapper = new MoveTargetMapper();
        MoveFactory moveFactory = new MoveFactory(targetMapper);
        MoveValidator moveValidator = new MoveValidator(moveRepository);
        moveService = new MoveService(moveRepository, matchRepository, moveValidator, moveFactory);
    }

    /**
     * Вспомогательный метод для установки enum значений через рефлексию
     * Работает как до, так и после генерации OpenAPI классов
     */
    private void setEnumValue(Object obj, String methodName, String enumValue) {
        try {
            // Пытаемся найти метод setter
            Method[] methods = obj.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    // Если это enum, используем valueOf
                    if (paramType.isEnum()) {
                        Object enumVal = valueOfEnum(paramType, enumValue);
                        method.invoke(obj, enumVal);
                        return;
                    }
                }
            }
            throw new RuntimeException("Не удалось найти метод " + methodName + " с enum параметром");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при установке enum значения: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E valueOfEnum(Class<?> enumType, String enumValue) {
        return Enum.valueOf((Class<E>) enumType, enumValue);
    }

    /**
     * Тест: успешное создание хода игроком
     * Проверяет, что ход корректно сохраняется в БД
     */
    @Test
    void shouldCreateMoveSuccessfully() {
        // Настройка моков: матч существует и игрок является участником
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(moveRepository.findByMatchIdAndPlayerIdAndTurnNumber(matchId, player1Id, 1))
                .thenReturn(Optional.empty());
        
        // Мокируем сохранение хода и матча
        Move savedMove = new Move();
        savedMove.setId(moveId);
        savedMove.setMatchId(matchId);
        savedMove.setPlayerId(player1Id);
        savedMove.setAttackTarget(Move.Target.HEAD);
        savedMove.setDefenseTarget(Move.Target.BODY);
        savedMove.setTurnNumber(1);
        savedMove.setCreated(Instant.now());
        
        when(moveRepository.save(any(Move.class))).thenReturn(savedMove);
        when(matchRepository.save(any(Match.class))).thenReturn(match);

        // Выполняем метод
        MoveResponse response = moveService.makeMove(moveRequest, player1Id);

        // Проверяем результат
        assertNotNull(response);
        assertEquals("Move accepted", response.getMessage());
        assertEquals(1, response.getTurnNumber());
        assertEquals(matchId, response.getMatchId());

        // Проверяем, что ход был сохранен
        ArgumentCaptor<Move> moveCaptor = ArgumentCaptor.forClass(Move.class);
        verify(moveRepository, times(1)).save(moveCaptor.capture());
        
        Move capturedMove = moveCaptor.getValue();
        assertEquals(matchId, capturedMove.getMatchId());
        assertEquals(player1Id, capturedMove.getPlayerId());
        assertEquals(Move.Target.HEAD, capturedMove.getAttackTarget());
        assertEquals(Move.Target.BODY, capturedMove.getDefenseTarget());
        assertEquals(1, capturedMove.getTurnNumber());
        assertNotNull(capturedMove.getCreated());

        // Проверяем, что статус матча обновлен на IN_PROGRESS
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(1)).save(matchCaptor.capture());
        assertEquals(Match.MatchStatus.IN_PROGRESS, matchCaptor.getValue().getStatus());
    }

    /**
     * Тест: ошибка при попытке сделать ход для несуществующего матча
     */
    @Test
    void shouldThrowExceptionWhenMatchNotFound() {
        // Настройка мока: матч не найден
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        // Выполняем метод и ожидаем исключение
        assertThrows(MatchNotFoundException.class, () -> {
            moveService.makeMove(moveRequest, player1Id);
        });

        // Проверяем, что ход не был сохранен
        verify(moveRepository, never()).save(any(Move.class));
    }

    /**
     * Тест: ошибка при попытке сделать ход игроком, не участвующим в матче
     */
    @Test
    void shouldThrowExceptionWhenPlayerNotParticipant() {
        UUID otherPlayerId = UUID.randomUUID();
        
        // Настройка моков: матч существует, но игрок не является участником
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Выполняем метод и ожидаем исключение
        PlayerNotParticipantException exception = assertThrows(PlayerNotParticipantException.class, () -> {
            moveService.makeMove(moveRequest, otherPlayerId);
        });
        assertTrue(exception.getMessage().contains("not a participant"));

        // Проверяем, что ход не был сохранен
        verify(moveRepository, never()).save(any(Move.class));
    }

    /**
     * Тест: ошибка при попытке сделать ход в завершенном матче
     */
    @Test
    void shouldThrowExceptionWhenMatchFinished() {
        // Устанавливаем статус матча как FINISHED
        match.setStatus(Match.MatchStatus.FINISHED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Выполняем метод и ожидаем исключение
        MatchFinishedException exception = assertThrows(MatchFinishedException.class, () -> {
            moveService.makeMove(moveRequest, player1Id);
        });
        assertTrue(exception.getMessage().contains("already finished"));

        // Проверяем, что ход не был сохранен
        verify(moveRepository, never()).save(any(Move.class));
    }

    /**
     * Тест: ошибка при неверном номере хода
     */
    @Test
    void shouldThrowExceptionWhenTurnNumberMismatch() {
        // Устанавливаем неверный номер хода в запросе
        moveRequest.setTurnNumber(5);
        match.setTurnNumber(1); // Текущий ход матча - 1
        
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Выполняем метод и ожидаем исключение
        InvalidTurnNumberException exception = assertThrows(InvalidTurnNumberException.class, () -> {
            moveService.makeMove(moveRequest, player1Id);
        });
        assertTrue(exception.getMessage().contains("Invalid turn number"));

        // Проверяем, что ход не был сохранен
        verify(moveRepository, never()).save(any(Move.class));
    }

    /**
     * Тест: ошибка при попытке сделать повторный ход в том же раунде
     */
    @Test
    void shouldThrowExceptionWhenMoveAlreadyExists() {
        // Настройка моков: матч существует, но ход уже был сделан
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        
        Move existingMove = new Move();
        existingMove.setId(moveId);
        when(moveRepository.findByMatchIdAndPlayerIdAndTurnNumber(matchId, player1Id, 1))
                .thenReturn(Optional.of(existingMove));

        // Выполняем метод и ожидаем исключение
        MoveAlreadyExistsException exception = assertThrows(MoveAlreadyExistsException.class, () -> {
            moveService.makeMove(moveRequest, player1Id);
        });
        assertTrue(exception.getMessage().contains("already made a move"));

        // Проверяем, что новый ход не был сохранен
        verify(moveRepository, never()).save(any(Move.class));
    }

    /**
     * Тест: статус матча не обновляется, если матч уже в статусе IN_PROGRESS
     */
    @Test
    void shouldNotUpdateMatchStatusWhenAlreadyInProgress() {
        // Устанавливаем статус матча как IN_PROGRESS
        match.setStatus(Match.MatchStatus.IN_PROGRESS);
        
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(moveRepository.findByMatchIdAndPlayerIdAndTurnNumber(matchId, player1Id, 1))
                .thenReturn(Optional.empty());
        
        Move savedMove = new Move();
        savedMove.setId(moveId);
        when(moveRepository.save(any(Move.class))).thenReturn(savedMove);

        // Выполняем метод
        MoveResponse response = moveService.makeMove(moveRequest, player1Id);

        // Проверяем результат
        assertNotNull(response);

        // Проверяем, что статус матча НЕ был обновлен (матч уже был IN_PROGRESS)
        verify(matchRepository, never()).save(any(Match.class));
    }

    /**
     * Тест: второй игрок может сделать ход в том же раунде
     */
    @Test
    void shouldAllowSecondPlayerToMakeMove() {
        // Настройка моков: матч существует, первый игрок уже сделал ход
        // Статус матча уже IN_PROGRESS, так как первый игрок уже сделал ход
        match.setStatus(Match.MatchStatus.IN_PROGRESS);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        
        // Второй игрок еще не делал ход
        when(moveRepository.findByMatchIdAndPlayerIdAndTurnNumber(matchId, player2Id, 1))
                .thenReturn(Optional.empty());
        
        Move savedMove = new Move();
        savedMove.setId(moveId);
        savedMove.setPlayerId(player2Id);
        when(moveRepository.save(any(Move.class))).thenReturn(savedMove);

        // Выполняем метод для второго игрока
        MoveResponse response = moveService.makeMove(moveRequest, player2Id);

        // Проверяем результат
        assertNotNull(response);
        assertEquals("Move accepted", response.getMessage());
        
        // Проверяем, что ход второго игрока был сохранен
        ArgumentCaptor<Move> moveCaptor = ArgumentCaptor.forClass(Move.class);
        verify(moveRepository, times(1)).save(moveCaptor.capture());
        assertEquals(player2Id, moveCaptor.getValue().getPlayerId());
    }
}

