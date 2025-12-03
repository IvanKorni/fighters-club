package net.proselyte.statsservice.repository;

import net.proselyte.statsservice.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatsRepository extends JpaRepository<Stats, UUID> {
    
    Optional<Stats> findByUserId(UUID userId);
}


