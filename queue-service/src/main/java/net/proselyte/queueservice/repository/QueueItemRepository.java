package net.proselyte.queueservice.repository;

import net.proselyte.queueservice.entity.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QueueItemRepository extends JpaRepository<QueueItem, UUID> {
    
    Optional<QueueItem> findByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
}


