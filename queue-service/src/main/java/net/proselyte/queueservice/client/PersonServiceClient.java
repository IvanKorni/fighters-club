package net.proselyte.queueservice.client;

import net.proselyte.person.dto.IndividualDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name = "person-service",
    url = "${person.service.url:http://localhost:8092}",
    path = "/v1/persons"
)
public interface PersonServiceClient {
    
    @GetMapping("/{id}")
    IndividualDto getPersonById(@PathVariable("id") UUID id);
}

