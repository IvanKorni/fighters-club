package net.proselyte.gameservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class MatchResponse {
    private UUID id;
    private UUID player1Id;
    private UUID player2Id;
    private UUID winnerId;
    private String status;
    private Integer player1HP;
    private Integer player2HP;
    private Integer turnNumber;
    private Instant currentTurnStart;
    private Integer turnCount;
    private Instant createdAt;
    private Instant updated;
    private Instant finishedAt;
    private Integer duration;
}

