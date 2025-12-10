package net.proselyte.personservice.rest;

import lombok.RequiredArgsConstructor;
import net.proselyte.person.api.PersonApi;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.person.dto.IndividualWriteResponseDto;
import net.proselyte.personservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Email;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class IndividualRestControllerV1 implements PersonApi {

    private final UserService userService;

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        userService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<IndividualPageDto> findAllByEmail(List<@Email String> email) {
        var users = userService.findByEmails(email);
        return ResponseEntity.status(HttpStatus.CREATED).body(users);
    }

    @Override
    public ResponseEntity<IndividualDto> findById(UUID id) {
        var user = userService.findById(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> registration(IndividualWriteDto individualWriteDto) {
        var user = userService.register(individualWriteDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> update(UUID id, IndividualWriteDto individualWriteDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.update(id, individualWriteDto));
    }
}