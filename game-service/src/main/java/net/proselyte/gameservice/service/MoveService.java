package net.proselyte.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.game.dto.MoveRequest;
import net.proselyte.game.dto.MoveResponse;
import net.proselyte.gameservice.entity.Match;
import net.proselyte.gameservice.entity.Move;
import net.proselyte.gameservice.exception.MatchNotFoundException;
import net.proselyte.gameservice.repository.MatchRepository;
import net.proselyte.gameservice.repository.MoveRepository;
import net.proselyte.gameservice.service.move.MoveFactory;
import net.proselyte.gameservice.service.move.MoveValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoveService {
    
    private final MoveRepository moveRepository;
    private final MatchRepository matchRepository;
    private final MoveValidator moveValidator;
    private final MoveFactory moveFactory;
    
    @Transactional
    public MoveResponse makeMove(MoveRequest request, UUID playerId) {
        log.info("Processing move for match: {}, player: {}, turn: {}", 
                request.getMatchId(), playerId, request.getTurnNumber());
        
        // Проверяем существование матча
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + request.getMatchId()));

        // Валидация бизнес-правил хода
        moveValidator.validate(match, request, playerId);

        // Создаем и сохраняем ход
        Instant now = Instant.now();
        Move move = moveFactory.create(request, playerId, now);
        Move savedMove = moveRepository.save(move);
        log.info("Move saved with id: {} for match: {}, player: {}, turn: {}", 
                savedMove.getId(), request.getMatchId(), playerId, request.getTurnNumber());
        
        // Обновляем статус матча на IN_PROGRESS, если он был WAITING
        if (match.getStatus() == Match.MatchStatus.WAITING) {
            match.setStatus(Match.MatchStatus.IN_PROGRESS);
            match.setCurrentTurnStart(now);
            match.setUpdated(now);
            matchRepository.save(match);
        }
        
        // Формируем ответ
        MoveResponse response = new MoveResponse();
        response.setMessage("Move accepted");
        response.setTurnNumber(request.getTurnNumber());
        response.setMatchId(request.getMatchId());
        
        return response;
    }
}

