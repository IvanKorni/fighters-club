package net.proselyte.gameservice.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.service.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/game")
public class GameRestControllerV1 {
    
    private final MatchService matchService;
    
    @PostMapping("/match")
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        log.info("Received request to create match for players: {} and {}", 
                request.getPlayer1Id(), request.getPlayer2Id());
        
        MatchResponse response = matchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}



