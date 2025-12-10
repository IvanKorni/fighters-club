package net.proselyte.api.environment.config.testcontainer.data;

import org.springframework.stereotype.Component;

import net.proselyte.individual.dto.IndividualWriteDto;
import net.proselyte.individual.dto.UserLoginRequest;


@Component
public class DtoCreator {
    public IndividualWriteDto buildIndividualWriteDto() {
        var request = new IndividualWriteDto();
        request.setNickname("john_doe");
        request.setEmail("test@mail.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");

        return request;
    }

    public UserLoginRequest buildUserLoginRequest() {
        var request = new UserLoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("secret123");
        return request;
    }


}
