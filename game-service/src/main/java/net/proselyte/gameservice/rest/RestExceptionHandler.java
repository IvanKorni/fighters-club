package net.proselyte.gameservice.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import net.proselyte.gameservice.exception.InvalidMoveTargetException;
import net.proselyte.gameservice.exception.InvalidTurnNumberException;
import net.proselyte.gameservice.exception.MatchFinishedException;
import net.proselyte.gameservice.exception.MatchNotFoundException;
import net.proselyte.gameservice.exception.MoveAlreadyExistsException;
import net.proselyte.gameservice.exception.PlayerNotParticipantException;
import net.proselyte.gameservice.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMatchNotFound(MatchNotFoundException ex, HttpServletRequest request) {
        return toResponse(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(PlayerNotParticipantException.class)
    public ResponseEntity<ErrorResponse> handleNotParticipant(PlayerNotParticipantException ex, HttpServletRequest request) {
        return toResponse(HttpStatus.FORBIDDEN, ex, request);
    }

    @ExceptionHandler({
            MatchFinishedException.class,
            InvalidTurnNumberException.class,
            MoveAlreadyExistsException.class,
            InvalidMoveTargetException.class,
            ValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return toResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private static ResponseEntity<ErrorResponse> toResponse(HttpStatus status, RuntimeException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}


