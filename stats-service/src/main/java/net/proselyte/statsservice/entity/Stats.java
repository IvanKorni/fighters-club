package net.proselyte.statsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "stats", schema = "stats")
public class Stats {
    
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "wins", nullable = false)
    private Integer wins;

    @NotNull
    @Column(name = "losses", nullable = false)
    private Integer losses;

    @NotNull
    @Column(name = "draws", nullable = false)
    private Integer draws;

    @NotNull
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches;

    @Column(name = "win_rate")
    private Double winRate;

    @Column(name = "average_match_duration")
    private Integer averageMatchDuration;

    @Column(name = "rating")
    private Integer rating;
}

