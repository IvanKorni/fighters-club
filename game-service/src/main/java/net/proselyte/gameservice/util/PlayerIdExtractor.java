package net.proselyte.gameservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
public class PlayerIdExtractor {
    
    /**
     * Извлекает player ID из JWT токена.
     * Player ID может быть в subject (sub) или в кастомном claim "playerId".
     * Если security не настроен, выбрасывает исключение.
     */
    public static UUID getCurrentPlayerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }
        
        // Пытаемся получить playerId из кастомного claim
        String playerIdStr = jwt.getClaimAsString("playerId");
        if (playerIdStr != null && !playerIdStr.isBlank()) {
            try {
                return UUID.fromString(playerIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid playerId format in JWT token: {}", playerIdStr);
            }
        }
        
        // Если playerId нет, используем subject как fallback
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            try {
                return UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid subject format in JWT token: {}", subject);
            }
        }
        
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player ID not found in JWT token");
    }
}

