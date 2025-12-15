package net.proselyte.queueservice.client;

import net.proselyte.game.dto.CreateMatchRequest;
import net.proselyte.game.dto.MatchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "game-service",
    url = "${game.service.url:http://localhost:8094}",
    path = "/v1/game"
)
public interface GameServiceClient {
    
    @PostMapping("/match")
    MatchResponse createMatch(@RequestBody CreateMatchRequest request);
}



