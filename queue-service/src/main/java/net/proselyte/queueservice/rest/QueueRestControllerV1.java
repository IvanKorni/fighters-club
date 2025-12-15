package net.proselyte.queueservice.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.queue.api.QueueApi;
import net.proselyte.queue.dto.QueueJoinResponseDto;
import net.proselyte.queue.dto.QueueLeaveResponseDto;
import net.proselyte.queue.dto.QueueStatusResponseDto;
import net.proselyte.queueservice.client.PersonServiceClient;
import net.proselyte.queueservice.service.QueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QueueRestControllerV1 implements QueueApi {

    private final QueueService queueService;
    private final PersonServiceClient personServiceClient;

    @Override
    public ResponseEntity<QueueJoinResponseDto> joinQueue() {
        UUID personId = getCurrentPersonId();
        queueService.joinQueue(personId);
        
        QueueJoinResponseDto response = new QueueJoinResponseDto();
        response.setMessage("waiting");
        response.setPersonId(personId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<QueueLeaveResponseDto> leaveQueue() {
        UUID personId = getCurrentPersonId();
        queueService.leaveQueue(personId);
        
        QueueLeaveResponseDto response = new QueueLeaveResponseDto();
        response.setMessage("left");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<QueueStatusResponseDto> getQueueStatus() {
        UUID personId = getCurrentPersonId();
        QueueStatusResponseDto response = queueService.getQueueStatus(personId);
        return ResponseEntity.ok(response);
    }

    /**
     * Извлекает person ID из JWT токена.
     * Сначала получает email из JWT, затем находит person по email в persons-api.
     * Это необходимо, потому что в очереди должен храниться person ID, а не Keycloak user ID.
     */
    private UUID getCurrentPersonId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }
        
        // Извлекаем email из JWT токена
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email not found in JWT token");
        }
        
        log.debug("Extracting person ID for email: {}", email);
        
        try {
            // Находим person по email в persons-api
            IndividualPageDto page = personServiceClient.findByEmail(Collections.singletonList(email));
            
            if (page == null || page.getItems() == null || page.getItems().isEmpty()) {
                log.error("Person not found for email: {}", email);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found for email: " + email);
            }
            
            IndividualDto person = page.getItems().get(0);
            if (person.getId() == null) {
                log.error("Person ID is null for email: {}", email);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Person ID is null");
            }
            
            UUID personId = person.getId();
            log.debug("Found person ID: {} for email: {}", personId, email);
            return personId;
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting person ID for email: {}", email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to get person ID: " + e.getMessage());
        }
    }
}
