package net.proselyte.gameservice.service.move;

import lombok.RequiredArgsConstructor;
import net.proselyte.game.dto.MoveRequest;
import net.proselyte.gameservice.entity.Match;
import net.proselyte.gameservice.exception.InvalidTurnNumberException;
import net.proselyte.gameservice.exception.MatchFinishedException;
import net.proselyte.gameservice.exception.MoveAlreadyExistsException;
import net.proselyte.gameservice.exception.PlayerNotParticipantException;
import net.proselyte.gameservice.repository.MoveRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MoveValidator {

    private final MoveRepository moveRepository;

    public void validate(Match match, MoveRequest request, UUID playerId) {
        // Проверяем, что игрок является участником матча
        if (!match.getPlayer1Id().equals(playerId) && !match.getPlayer2Id().equals(playerId)) {
            throw new PlayerNotParticipantException(
                    "Player " + playerId + " is not a participant of match " + request.getMatchId()
            );
        }

        // Проверяем, что матч не завершен
        if (match.getStatus() == Match.MatchStatus.FINISHED) {
            throw new MatchFinishedException("Match " + request.getMatchId() + " is already finished");
        }

        // Проверяем, что номер хода соответствует текущему ходу матча
        Integer requestTurn = request.getTurnNumber();
        Integer matchTurn = match.getTurnNumber();
        if (requestTurn == null || matchTurn == null || !requestTurn.equals(matchTurn)) {
            throw new InvalidTurnNumberException(
                    String.format("Invalid turn number. Expected: %d, got: %d", matchTurn, requestTurn)
            );
        }

        // Проверяем, что игрок еще не сделал ход в этом раунде
        boolean moveExists = moveRepository.findByMatchIdAndPlayerIdAndTurnNumber(
                request.getMatchId(), playerId, requestTurn
        ).isPresent();

        if (moveExists) {
            throw new MoveAlreadyExistsException(
                    String.format("Player %s has already made a move for turn %d", playerId, requestTurn)
            );
        }
    }
}


