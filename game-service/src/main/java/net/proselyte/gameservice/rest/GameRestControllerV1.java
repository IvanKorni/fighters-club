package net.proselyte.gameservice.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.service.MatchService;
import net.proselyte.gameservice.service.MoveService;
import net.proselyte.gameservice.util.PlayerIdExtractor;
import net.proselyte.game.dto.MoveRequest;
import net.proselyte.game.dto.MoveResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import net.proselyte.gameservice.exception.ValidationException;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/game")
public class GameRestControllerV1 {
    
    private final MatchService matchService;
    private final MoveService moveService;
    
    @PostMapping("/match")
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        log.info("Received request to create match for players: {} and {}", 
                request.getPlayer1Id(), request.getPlayer2Id());
        
        MatchResponse response = matchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable UUID matchId) {
        log.info("Received request to get match: {}", matchId);
        
        MatchResponse response = matchService.getMatch(matchId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/move")
    public ResponseEntity<MoveResponse> makeMove(@Valid @RequestBody MoveRequest request) {
        // Manual validation for required fields since generated DTO uses javax.validation
        // which may not work properly with Spring Boot 3.x jakarta.validation
        if (request.getMatchId() == null) {
            throw new ValidationException("matchId is required");
        }
        if (request.getAttackTarget() == null) {
            throw new ValidationException("attackTarget is required");
        }
        if (request.getDefenseTarget() == null) {
            throw new ValidationException("defenseTarget is required");
        }
        if (request.getTurnNumber() == null) {
            throw new ValidationException("turnNumber is required");
        }
        
        log.info("Received move request for match: {}, turn: {}", 
                request.getMatchId(), request.getTurnNumber());
        
        UUID playerId = PlayerIdExtractor.getCurrentPlayerId();
        MoveResponse response = moveService.makeMove(request, playerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}



