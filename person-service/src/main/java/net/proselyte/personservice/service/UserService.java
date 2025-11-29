package net.proselyte.personservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.person.dto.IndividualWriteResponseDto;
import net.proselyte.personservice.exception.PersonException;
import net.proselyte.personservice.mapper.UserMapper;
import net.proselyte.personservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Transactional
    public IndividualWriteResponseDto register(IndividualWriteDto writeDto) {
        var user = userMapper.to(writeDto);
        userRepository.save(user);
        log.info("IN - register: user: [{}] successfully registered", user.getEmail());
        return new IndividualWriteResponseDto(user.getId().toString());
    }

    public IndividualPageDto findByEmails(List<String> emails) {
        var users = userRepository.findAllByEmails(emails);
        var dtos = isEmpty(users)
                ? Collections.<IndividualDto>emptyList()
                : users.stream()
                .map(userMapper::from)
                .collect(Collectors.toList());
        
        var individualPageDto = new IndividualPageDto();
        individualPageDto.setItems(dtos);
        return individualPageDto;
    }

    public IndividualDto findById(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new PersonException("User not found by id=[%s]", id));
        log.info("IN - findById: user with id = [{}] successfully found", id);
        return userMapper.from(user);
    }

    @Transactional
    public void delete(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new PersonException("User not found by id=[%s]", id));
        log.info("IN - delete: user with id = [{}] successfully deleted", id);
        userRepository.delete(user);
    }

    @Transactional
    public IndividualWriteResponseDto update(UUID id, IndividualWriteDto writeDto) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new PersonException("User not found by id=[%s]", id));
        userMapper.update(user, writeDto);
        userRepository.save(user);
        return new IndividualWriteResponseDto(user.getId().toString());
    }
}

