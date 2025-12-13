package net.proselyte.gameservice.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
public class CreateMatchRequest {
    @NotNull(message = "player1Id is required")
    private UUID player1Id;
    
    @NotNull(message = "player2Id is required")
    private UUID player2Id;
}

