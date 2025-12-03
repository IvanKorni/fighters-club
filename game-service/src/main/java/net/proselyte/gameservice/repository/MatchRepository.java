package net.proselyte.gameservice.repository;

import net.proselyte.gameservice.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    
    Optional<Match> findById(UUID id);
    
    @Query("SELECT m FROM Match m WHERE m.player1Id = :playerId OR m.player2Id = :playerId ORDER BY m.created DESC")
    List<Match> findAllByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT m FROM Match m WHERE (m.player1Id = :playerId OR m.player2Id = :playerId) AND m.status = :status")
    List<Match> findAllByPlayerIdAndStatus(@Param("playerId") UUID playerId, @Param("status") Match.MatchStatus status);
}


