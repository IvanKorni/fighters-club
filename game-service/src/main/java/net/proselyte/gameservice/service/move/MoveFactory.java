package net.proselyte.gameservice.service.move;

import lombok.RequiredArgsConstructor;
import net.proselyte.game.dto.MoveRequest;
import net.proselyte.gameservice.entity.Move;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MoveFactory {

    private final MoveTargetMapper targetMapper;

    public Move create(MoveRequest request, UUID playerId, Instant now) {
        Move move = new Move();
        move.setMatchId(request.getMatchId());
        move.setPlayerId(playerId);
        // Явный маппинг из OpenAPI DTO enum в доменный enum (устойчивее, чем valueOf/toString)
        move.setAttackTarget(targetMapper.mapTarget("attackTarget", request.getAttackTarget().getValue()));
        move.setDefenseTarget(targetMapper.mapTarget("defenseTarget", request.getDefenseTarget().getValue()));
        move.setTurnNumber(request.getTurnNumber());
        move.setCreated(now);
        return move;
    }
}


