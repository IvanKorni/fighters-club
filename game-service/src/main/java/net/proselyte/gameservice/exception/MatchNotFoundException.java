package net.proselyte.gameservice.exception;

public class MatchNotFoundException extends RuntimeException {
    
    public MatchNotFoundException(String message) {
        super(message);
    }
}

