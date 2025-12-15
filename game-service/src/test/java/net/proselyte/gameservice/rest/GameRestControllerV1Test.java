package net.proselyte.gameservice.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameRestControllerV1.class)
class GameRestControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

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
}



