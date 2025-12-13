package net.proselyte.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.gameservice.dto.CreateMatchRequest;
import net.proselyte.gameservice.dto.MatchResponse;
import net.proselyte.gameservice.entity.Match;
import net.proselyte.gameservice.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {
    
    private final MatchRepository matchRepository;
    
    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request) {
        log.info("Creating match for players: {} and {}", request.getPlayer1Id(), request.getPlayer2Id());
        
        Match match = new Match();
        match.setPlayer1Id(request.getPlayer1Id());
        match.setPlayer2Id(request.getPlayer2Id());
        match.setStatus(Match.MatchStatus.WAITING);
        match.setPlayer1HP(100);
        match.setPlayer2HP(100);
        match.setTurnNumber(1);
        
        Instant now = Instant.now();
        match.setCreated(now);
        match.setUpdated(now);
        
        Match savedMatch = matchRepository.save(match);
        log.info("Match created successfully with id: {}", savedMatch.getId());
        
        return toMatchResponse(savedMatch);
    }
    
    private MatchResponse toMatchResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setPlayer1Id(match.getPlayer1Id());
        response.setPlayer2Id(match.getPlayer2Id());
        response.setWinnerId(match.getWinnerId());
        response.setStatus(match.getStatus().name());
        response.setPlayer1HP(match.getPlayer1HP());
        response.setPlayer2HP(match.getPlayer2HP());
        response.setTurnNumber(match.getTurnNumber());
        response.setCurrentTurnStart(match.getCurrentTurnStart());
        response.setTurnCount(match.getTurnCount());
        response.setCreatedAt(match.getCreated());
        response.setUpdated(match.getUpdated());
        response.setFinishedAt(match.getFinishedAt());
        response.setDuration(match.getDuration());
        return response;
    }
}

