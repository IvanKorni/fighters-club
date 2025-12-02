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
@Table(name = "moves", schema = "game")
public class Move {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @NotNull
    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @NotNull
    @Column(name = "attack_target", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Target attackTarget;

    @NotNull
    @Column(name = "defense_target", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Target defenseTarget;

    @NotNull
    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Column(name = "damage")
    private Integer damage;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    public enum Target {
        HEAD,
        BODY,
        LEGS
    }
}

