package net.proselyte.gameservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "matches", schema = "game")
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "player1_id", nullable = false)
    private UUID player1Id;

    @NotNull
    @Column(name = "player2_id", nullable = false)
    private UUID player2Id;

    @Column(name = "winner_id")
    private UUID winnerId;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @NotNull
    @Column(name = "player1_hp", nullable = false)
    private Integer player1HP;

    @NotNull
    @Column(name = "player2_hp", nullable = false)
    private Integer player2HP;

    @NotNull
    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Column(name = "current_turn_start")
    private Instant currentTurnStart;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @NotNull
    @Column(name = "updated", nullable = false)
    private Instant updated;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "turn_count")
    private Integer turnCount;

    public enum MatchStatus {
        WAITING,
        IN_PROGRESS,
        FINISHED
    }
}


