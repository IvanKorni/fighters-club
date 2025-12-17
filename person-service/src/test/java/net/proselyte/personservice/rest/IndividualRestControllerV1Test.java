package net.proselyte.personservice.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualPageDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.person.dto.IndividualWriteResponseDto;
import net.proselyte.personservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndividualRestControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
class IndividualRestControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        IndividualWriteDto writeDto = new IndividualWriteDto();
        writeDto.setEmail("test@example.com");
        writeDto.setNickname("testuser");

        UUID userId = UUID.randomUUID();
        IndividualWriteResponseDto responseDto = new IndividualWriteResponseDto();
        responseDto.setId(userId.toString());

        when(userService.register(any(IndividualWriteDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(userService).register(any(IndividualWriteDto.class));
    }

    @Test
    void shouldFindByIdReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
        IndividualDto dto = new IndividualDto();
        dto.setEmail("test@example.com");
        dto.setNickname("testuser");

        when(userService.findById(userId)).thenReturn(dto);

        mockMvc.perform(get("/v1/persons/{id}", userId))
                .andExpect(status().isCreated()) // OpenAPI spec says 201
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("testuser"));

        verify(userService).findById(userId);
    }

    @Test
    void shouldFindAllByEmailReturnUsers() throws Exception {
        List<String> emails = List.of("test@example.com");
        IndividualDto dto = new IndividualDto();
        dto.setEmail("test@example.com");
        dto.setNickname("testuser");

        IndividualPageDto pageDto = new IndividualPageDto();
        pageDto.setItems(List.of(dto));

        when(userService.findByEmails(emails)).thenReturn(pageDto);

        mockMvc.perform(get("/v1/persons")
                        .param("email", "test@example.com"))
                .andExpect(status().isCreated()) // OpenAPI spec says 201
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.items[0].nickname").value("testuser"));

        verify(userService).findByEmails(emails);
    }

    @Test
    void shouldUpdateUserSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        IndividualWriteDto writeDto = new IndividualWriteDto();
        writeDto.setEmail("updated@example.com");
        writeDto.setNickname("updateduser");

        IndividualWriteResponseDto responseDto = new IndividualWriteResponseDto();
        responseDto.setId(userId.toString());

        when(userService.update(eq(userId), any(IndividualWriteDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/v1/persons/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeDto)))
                .andExpect(status().isCreated()) // OpenAPI spec says 201
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(userService).update(eq(userId), any(IndividualWriteDto.class));
    }

    @Test
    void shouldDeleteUserSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).delete(userId);

        mockMvc.perform(delete("/v1/persons/{id}", userId))
                .andExpect(status().isOk());

        verify(userService).delete(userId);
    }
}

