package net.proselyte.queueservice.rest;

import lombok.RequiredArgsConstructor;
import net.proselyte.queue.api.QueueApi;
import net.proselyte.queue.dto.QueueJoinResponseDto;
import net.proselyte.queue.dto.QueueLeaveResponseDto;
import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.service.QueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QueueRestControllerV1 implements QueueApi {

    private final QueueService queueService;

    @Override
    public ResponseEntity<QueueJoinResponseDto> joinQueue() {
        UUID userId = getCurrentUserId();
        queueService.joinQueue(userId);
        
        QueueJoinResponseDto response = new QueueJoinResponseDto();
        response.setMessage("waiting");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<QueueLeaveResponseDto> leaveQueue() {
        UUID userId = getCurrentUserId();
        queueService.leaveQueue(userId);
        
        QueueLeaveResponseDto response = new QueueLeaveResponseDto();
        response.setMessage("left");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<QueueStatusResponseDto> getQueueStatus() {
        UUID userId = getCurrentUserId();
        QueueStatusResponseDto response = queueService.getQueueStatus(userId);
        return ResponseEntity.ok(response);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        // TODO: Extract userId from JWT token
        String principal = authentication.getName();
        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(principal.getBytes());
        }
    }
}
