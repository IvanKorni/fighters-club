package net.proselyte.queueservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "queue_items", schema = "queue")
public class QueueItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @NotNull
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    public enum QueueStatus {
        WAITING,
        MATCHED,
        LEFT
    }
}


