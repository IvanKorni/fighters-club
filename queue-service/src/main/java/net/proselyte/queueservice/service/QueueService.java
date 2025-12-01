package net.proselyte.queueservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    public void joinQueue(UUID userId) {
        log.info("User {} joined the queue", userId);
        // TODO: Implement queue join logic
    }

    public void leaveQueue(UUID userId) {
        log.info("User {} left the queue", userId);
        // TODO: Implement queue leave logic
    }
}
