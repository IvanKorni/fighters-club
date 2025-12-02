package net.proselyte.queueservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.entity.QueueItem;
import net.proselyte.queueservice.repository.QueueItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueItemRepository queueItemRepository;

    public void joinQueue(UUID userId) {
        log.info("User {} joined the queue", userId);
        // TODO: Implement queue join logic
    }

    public void leaveQueue(UUID userId) {
        log.info("User {} left the queue", userId);
        // TODO: Implement queue leave logic
    }

    public QueueStatusResponseDto getQueueStatus(UUID userId) {
        QueueItem queueItem = queueItemRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in queue"));

        QueueStatusResponseDto response = new QueueStatusResponseDto();
        response.setStatus(QueueStatusResponseDto.StatusEnum.valueOf(queueItem.getStatus().name()));
        response.setJoinedAt(queueItem.getJoinedAt().atOffset(ZoneOffset.UTC));
        
        // Calculate waiting time in seconds
        long waitingTimeSeconds = Duration.between(queueItem.getJoinedAt(), Instant.now()).getSeconds();
        response.setWaitingTime((int) waitingTimeSeconds);
        
        // TODO: Calculate position in queue if needed
        // response.setPosition(position);
        
        return response;
    }
}
