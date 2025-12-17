package net.proselyte.gameservice.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.service.MatchService;
import net.proselyte.gameservice.service.MoveService;
import net.proselyte.game.dto.MoveRequest;
import net.proselyte.game.dto.MoveResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({GameRestControllerV1.class, RestExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class GameRestControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private MoveService moveService;

    @Test
    void shouldCreateMatchSuccessfully() throws Exception {
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();

        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id(player1Id);
        request.setPlayer2Id(player2Id);

        MatchResponse response = new MatchResponse();
        response.setId(matchId);
        response.setPlayer1Id(player1Id);
        response.setPlayer2Id(player2Id);
        response.setStatus("WAITING");
        response.setPlayer1HP(100);
        response.setPlayer2HP(100);
        response.setTurnNumber(1);
        response.setCreatedAt(Instant.now());
        response.setUpdated(Instant.now());

        when(matchService.createMatch(any(CreateMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/game/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.player1Id").value(player1Id.toString()))
                .andExpect(jsonPath("$.player2Id").value(player2Id.toString()))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.player1HP").value(100))
                .andExpect(jsonPath("$.player2HP").value(100))
                .andExpect(jsonPath("$.turnNumber").value(1));

        verify(matchService).createMatch(any(CreateMatchRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenPlayer1IdIsMissing() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer2Id(UUID.randomUUID());
        // player1Id is null

        mockMvc.perform(post("/v1/game/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(matchService, never()).createMatch(any(CreateMatchRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenPlayer2IdIsMissing() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id(UUID.randomUUID());
        // player2Id is null

        mockMvc.perform(post("/v1/game/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(matchService, never()).createMatch(any(CreateMatchRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/v1/game/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(matchService, never()).createMatch(any(CreateMatchRequest.class));
    }

    /**
     * Тест: успешная отправка хода через REST API
     * Проверяет, что эндпоинт POST /v1/game/move корректно обрабатывает запрос
     */
    @Test
    void shouldAcceptMoveSuccessfully() throws Exception {
        // Подготовка тестовых данных
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        
        // Создаем запрос на ход через JSON строку (работает до и после генерации OpenAPI)
        String requestJson = String.format(
            "{\"matchId\":\"%s\",\"attackTarget\":\"HEAD\",\"defenseTarget\":\"BODY\",\"turnNumber\":1}",
            matchId
        );

        // Создаем ожидаемый ответ
        MoveResponse response = new MoveResponse();
        response.setMessage("Move accepted");
        response.setTurnNumber(1);
        response.setMatchId(matchId);

        // Мокируем SecurityContext для извлечения playerId из JWT токена
        mockSecurityContext(playerId);
        
        // Мокируем сервис
        when(moveService.makeMove(any(MoveRequest.class), eq(playerId))).thenReturn(response);

        // Выполняем запрос
        mockMvc.perform(post("/v1/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted()) // 202 Accepted согласно спецификации
                .andExpect(jsonPath("$.message").value("Move accepted"))
                .andExpect(jsonPath("$.turnNumber").value(1))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()));

        // Проверяем, что сервис был вызван с правильными параметрами
        verify(moveService, times(1)).makeMove(any(MoveRequest.class), eq(playerId));
    }

    /**
     * Тест: ошибка валидации при отсутствии обязательных полей
     * Проверяет, что валидация работает корректно
     */
    /**
     * Тест: ошибка валидации при отсутствии обязательных полей
     */
    @Test
    void shouldReturnBadRequestWhenMoveRequestIsInvalid() throws Exception {
        // Мокируем SecurityContext
        UUID playerId = UUID.randomUUID();
        mockSecurityContext(playerId);

        // Создаем невалидный запрос (пустой JSON)
        String invalidJson = "{}";

        // Выполняем запрос и ожидаем ошибку валидации
        mockMvc.perform(post("/v1/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Проверяем, что сервис не был вызван
        verify(moveService, never()).makeMove(any(MoveRequest.class), any(UUID.class));
    }

    /**
     * Тест: ошибка валидации при отсутствии matchId
     */
    @Test
    void shouldReturnBadRequestWhenMatchIdIsMissing() throws Exception {
        UUID playerId = UUID.randomUUID();
        mockSecurityContext(playerId);

        // Создаем запрос без matchId
        String requestJson = "{\"attackTarget\":\"HEAD\",\"defenseTarget\":\"BODY\",\"turnNumber\":1}";

        mockMvc.perform(post("/v1/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(moveService, never()).makeMove(any(MoveRequest.class), any(UUID.class));
    }

    /**
     * Тест: ошибка валидации при отсутствии attackTarget
     */
    @Test
    void shouldReturnBadRequestWhenAttackTargetIsMissing() throws Exception {
        UUID playerId = UUID.randomUUID();
        mockSecurityContext(playerId);

        // Создаем запрос без attackTarget
        String requestJson = String.format(
            "{\"matchId\":\"%s\",\"defenseTarget\":\"BODY\",\"turnNumber\":1}",
            UUID.randomUUID()
        );

        mockMvc.perform(post("/v1/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(moveService, never()).makeMove(any(MoveRequest.class), any(UUID.class));
    }

    /**
     * Тест: ошибка валидации при неверном формате JSON
     */
    @Test
    void shouldReturnBadRequestWhenJsonIsInvalid() throws Exception {
        UUID playerId = UUID.randomUUID();
        mockSecurityContext(playerId);

        // Отправляем невалидный JSON
        mockMvc.perform(post("/v1/game/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(moveService, never()).makeMove(any(MoveRequest.class), any(UUID.class));
    }

    /**
     * Вспомогательный метод для мокирования SecurityContext и JWT токена
     * Создает мок аутентификации с указанным playerId
     */
    private void mockSecurityContext(UUID playerId) {
        // Создаем мок JWT токена
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(playerId.toString());
        when(jwt.getClaimAsString("playerId")).thenReturn(playerId.toString());

        // Создаем мок аутентификации
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        // Создаем мок SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Устанавливаем мок в SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
    }
}



