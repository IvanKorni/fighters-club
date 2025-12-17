package net.proselyte.gameservice.service.move;

import net.proselyte.gameservice.entity.Move;
import net.proselyte.gameservice.exception.InvalidMoveTargetException;
import org.springframework.stereotype.Component;

@Component
public class MoveTargetMapper {

    public Move.Target mapTarget(String fieldName, String value) {
        if (value == null) {
            throw new InvalidMoveTargetException(fieldName + " must not be null");
        }
        return switch (value) {
            case "HEAD" -> Move.Target.HEAD;
            case "BODY" -> Move.Target.BODY;
            case "LEGS" -> Move.Target.LEGS;
            default -> throw new InvalidMoveTargetException("Unexpected " + fieldName + " value: " + value);
        };
    }
}


