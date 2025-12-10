package net.proselyte.statsservice.repository;

import net.proselyte.statsservice.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    
    @Query("SELECT m FROM Match m WHERE m.player1Id = :playerId OR m.player2Id = :playerId " +
           "AND (:from IS NULL OR m.created >= :from) " +
           "AND (:to IS NULL OR m.created <= :to) " +
           "ORDER BY m.created DESC")
    List<Match> findAllByPlayerId(
        @Param("playerId") UUID playerId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
