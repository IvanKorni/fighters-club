package net.proselyte.gameservice.repository;

import net.proselyte.gameservice.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MoveRepository extends JpaRepository<Move, UUID> {
    
    @Query("SELECT m FROM Move m WHERE m.matchId = :matchId ORDER BY m.turnNumber ASC, m.created ASC")
    List<Move> findAllByMatchId(@Param("matchId") UUID matchId);
    
    @Query("SELECT m FROM Move m WHERE m.matchId = :matchId AND m.turnNumber = :turnNumber")
    List<Move> findAllByMatchIdAndTurnNumber(@Param("matchId") UUID matchId, @Param("turnNumber") Integer turnNumber);
    
    @Query("SELECT m FROM Move m WHERE m.matchId = :matchId AND m.playerId = :playerId AND m.turnNumber = :turnNumber")
    Optional<Move> findByMatchIdAndPlayerIdAndTurnNumber(
        @Param("matchId") UUID matchId,
        @Param("playerId") UUID playerId,
        @Param("turnNumber") Integer turnNumber
    );
}

