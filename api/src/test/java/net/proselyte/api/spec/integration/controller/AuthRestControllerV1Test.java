package net.proselyte.api.spec.integration.controller;

import net.proselyte.api.spec.integration.LifecycleSpecification;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.*;

class AuthRestControllerV1Test extends LifecycleSpecification {

    @Value("${application.telegram.botToken}")
    private String telegramBotToken;

    @Test
    void shouldCreateNewUserAndReturnAccessToken() {
        // when
        var request = dtoCreator.buildIndividualWriteDto();
        var response = individualControllerService.register(request);
        var meResponse = individualControllerService.getMe(response.getAccessToken());

        var personId = keycloakApiTestService
                .getUserRepresentation(request.getEmail())
                .getId();

        // then
        assertTrue(StringUtils.isNoneBlank(personId));
        assertNotNull(response, "Response must not be null");
        assertNotNull(response.getAccessToken(), "Access token must not be null");
        assertEquals("Bearer", response.getTokenType(), "Token type must be Bearer");
        assertEquals(request.getEmail(), meResponse.getEmail());
    }

    @Test
    void shouldLoginAndReturnAccessToken() {
        // given: регистрируем пользователя
        var registerRequest = dtoCreator.buildIndividualWriteDto();
        individualControllerService.register(registerRequest);

        // when: логинимся тем же email/password
        var loginRequest = dtoCreator.buildUserLoginRequest();
        var response = individualControllerService.login(loginRequest);
        var meResponse = individualControllerService.getMe(response.getAccessToken());

        // then
        assertNotNull(response, "Response must not be null");
        assertNotNull(response.getAccessToken(), "Access token must not be null");
        assertEquals("Bearer", response.getTokenType(), "Token type must be Bearer");
        assertEquals(registerRequest.getEmail(), meResponse.getEmail());
    }

    @Test
    void shouldReturnUserInfo() {
        // given
        var individualWriteDto = dtoCreator.buildIndividualWriteDto();
        var registrationResponse = individualControllerService.register(individualWriteDto);

        // when
        var meResponse = individualControllerService.getMe(registrationResponse.getAccessToken());

        // then
        assertNotNull(meResponse.getEmail(), "email in /me must be present");
        assertEquals(individualWriteDto.getEmail(), meResponse.getEmail(), "emails must match");
    }

    @Test
    void shouldAuthenticateViaTelegramAndReturnAccessToken() {
        // given: создаем запрос с корректными данными от Telegram
        var request = dtoCreator.buildTelegramAuthRequest(telegramBotToken);

        // when: аутентифицируемся через Telegram
        var response = individualControllerService.telegramAuth(request);
        var meResponse = individualControllerService.getMe(response.getAccessToken());

        // then: проверяем что токен получен и пользователь создан
        assertNotNull(response, "Response must not be null");
        assertNotNull(response.getAccessToken(), "Access token must not be null");
        assertEquals("Bearer", response.getTokenType(), "Token type must be Bearer");
        
        // Проверяем что email соответствует формату tg_{id}@telegram.user
        // Используем фиксированный ID из DtoCreator (123456789L)
        String expectedEmail = "tg_123456789@telegram.user";
        assertEquals(expectedEmail, meResponse.getEmail(), "Email must match Telegram format");
        
        // Проверяем что nickname установлен
        assertNotNull(meResponse.getNickname(), "Nickname must be set");
        // Nickname должен содержать "John" (firstName) или "johndoe" (username) из DtoCreator
        assertTrue(meResponse.getNickname().contains("John") 
                || meResponse.getNickname().equals("johndoe"),
                "Nickname should contain first name or username");

        // Проверяем что пользователь создан в Keycloak
        var keycloakUser = keycloakApiTestService.getUserRepresentation(expectedEmail);
        assertNotNull(keycloakUser, "User must be created in Keycloak");
        assertTrue(StringUtils.isNoneBlank(keycloakUser.getId()));
    }

    @Test
    void shouldLoginExistingTelegramUser() {
        // given: регистрируем пользователя через Telegram
        var request = dtoCreator.buildTelegramAuthRequest(telegramBotToken);
        var firstResponse = individualControllerService.telegramAuth(request);

        // when: логинимся повторно с теми же данными
        var secondResponse = individualControllerService.telegramAuth(request);
        var meResponse = individualControllerService.getMe(secondResponse.getAccessToken());

        // then: проверяем что получили токен для существующего пользователя
        assertNotNull(secondResponse, "Response must not be null");
        assertNotNull(secondResponse.getAccessToken(), "Access token must not be null");
        
        // Используем фиксированный ID из DtoCreator (123456789L)
        String expectedEmail = "tg_123456789@telegram.user";
        assertEquals(expectedEmail, meResponse.getEmail(), "Should login same user");
        
        // Проверяем что токены разные (новая сессия)
        assertNotEquals(firstResponse.getAccessToken(), secondResponse.getAccessToken(),
                "Should generate new access token");
    }

    @Test
    void shouldRejectInvalidTelegramHash() {
        // given: создаем запрос с неправильным hash в initData
        var request = dtoCreator.buildTelegramAuthRequest(telegramBotToken);
        // Заменяем hash в initData на неверный
        String invalidInitData = request.getInitData().replaceAll("hash=[^&]+", "hash=invalid_hash_12345");
        request.setInitData(invalidInitData);

        // when & then: проверяем что аутентификация отклоняется
        assertThrows(Exception.class, () -> {
            individualControllerService.telegramAuth(request);
        }, "Should reject invalid Telegram hash");
    }
}
