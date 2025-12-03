package net.proselyte.statsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "matches", schema = "stats")
public class Match {
    
    @Id
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

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "turn_count")
    private Integer turnCount;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public enum MatchStatus {
        WAITING,
        IN_PROGRESS,
        FINISHED
    }
}


