package net.proselyte.gameservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateMatchRequest {
    private UUID player1Id;
    private UUID player2Id;
}

