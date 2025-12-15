package net.proselyte.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.api.config.TelegramProperties;
import net.proselyte.api.dto.KeycloakCredentialsRepresentation;
import net.proselyte.api.mapper.KeycloakMapper;
import net.proselyte.api.mapper.TokenResponseMapper;
import net.proselyte.api.util.TelegramAuthValidator;
import net.proselyte.api.util.TelegramAuthValidator.TelegramUser;
import net.proselyte.individual.dto.IndividualWriteDto;
import net.proselyte.individual.dto.TelegramAuthRequest;
import net.proselyte.individual.dto.TokenResponse;
import net.proselyte.individual.dto.UserInfoResponse;
import net.proselyte.individual.dto.UserLoginRequest;
import net.proselyte.api.client.KeycloakClient;
import net.proselyte.api.dto.KeycloakUserRepresentation;
import net.proselyte.api.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final PersonService personService;
    private final KeycloakClient keycloakClient;
    private final TokenResponseMapper tokenResponseMapper;
    private final TelegramProperties telegramProperties;

    @WithSpan(value = "userService.getCurrentUserInfo")
    public Mono<UserInfoResponse> getUserInfo() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(UserService::getUserInfoResponseMono)
                .switchIfEmpty(Mono.error(new ApiException("No authentication present")));
    }

    private static Mono<UserInfoResponse> getUserInfoResponseMono(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            var userInfoResponse = new UserInfoResponse();
            userInfoResponse.setId(jwt.getSubject());
            userInfoResponse.setEmail(jwt.getClaimAsString("email"));
            userInfoResponse.setNickname(jwt.getClaimAsString("preferred_username"));
            userInfoResponse.setRoles(jwt.getClaimAsStringList("roles"));

            if (jwt.getIssuedAt() != null) {
                userInfoResponse.setCreatedAt(jwt.getIssuedAt().atOffset(ZoneOffset.UTC));
            }
            log.info("User[email={}] was successfully get info", jwt.getClaimAsString("email"));

            return Mono.just(userInfoResponse);
        }

        log.error("Can not get current user info: Invalid principal");
        return Mono.error(new ApiException("Can not get current user info: Invalid principal"));
    }

    @WithSpan("userService.register")
    public Mono<TokenResponse> register(IndividualWriteDto request) {
        return personService.register(request) // Mono<UUID> personId
                .flatMap(personId ->
                        keycloakClient.adminLogin()
                                .flatMap(adminToken -> {
                                    var kcUser = new net.proselyte.api.dto.KeycloakUserRepresentation(
                                            null,
                                            request.getNickname() != null ? request.getNickname() : request.getEmail(),
                                            request.getEmail(),
                                            true,
                                            true,
                                            null
                                    );
                                    return keycloakClient.registerUser(adminToken, kcUser)
                                            .flatMap(kcUserId -> {
                                                var cred = new net.proselyte.api.dto.KeycloakCredentialsRepresentation(
                                                        "password",
                                                        request.getPassword(),
                                                        false
                                                );
                                                return keycloakClient
                                                        .resetUserPassword(kcUserId, cred, adminToken.getAccessToken())
                                                        .thenReturn(kcUserId)
                                                        .flatMap(r ->
                                                                keycloakClient.login(
                                                                        new net.proselyte.keycloak.dto.UserLoginRequest(
                                                                                request.getEmail(),
                                                                                request.getPassword()
                                                                        )
                                                                )
                                                        )
                                                        .onErrorResume(ex ->
                                                                keycloakClient.executeOnError(kcUserId, adminToken.getAccessToken(), ex)
                                                                        .then(Mono.error(ex))
                                                        );
                                            })
                                            .onErrorResume(err ->
                                                    personService.delete(personId.getId().toString())
                                                            .then(Mono.error(err))
                                            )
                                            .map(tokenResponseMapper::toTokenResponse);

                                })
                );
    }

    @WithSpan("userService.telegramAuth")
    public Mono<TokenResponse> telegramAuth(TelegramAuthRequest request) {
        log.info("Starting Telegram authentication with initData");

        // Валидируем данные от Telegram и парсим их
        TelegramAuthValidator.TelegramUser telegramUser = TelegramAuthValidator.validateAndParse(
            request, telegramProperties.botToken());
        
        if (telegramUser == null) {
            log.error("Telegram auth validation failed");
            return Mono.error(new ApiException("Invalid Telegram authentication data"));
        }

        log.info("Telegram auth validated successfully for user ID: {}", telegramUser.getId());

        // Создаем email на основе Telegram ID
        String email = "tg_" + telegramUser.getId() + "@telegram.user";
        
        // Создаем nickname из имени и фамилии или username
        String nickname = telegramUser.getUsername();
        if (nickname == null || nickname.isBlank()) {
            nickname = telegramUser.getFirstName();
            if (telegramUser.getLastName() != null && !telegramUser.getLastName().isBlank()) {
                nickname = nickname + " " + telegramUser.getLastName();
            }
        }

        // Генерируем детерминированный пароль для Telegram пользователя
        // Используем Telegram ID и bot token для создания стабильного пароля
        // Безопасность обеспечивается валидацией Telegram hash, пароль - техническое требование Keycloak
        String generatedPassword = generateTelegramPassword(telegramUser.getId(), telegramProperties.botToken());

        log.info("Attempting to login Telegram user with email: {}", email);

        // Пытаемся залогиниться (если пользователь уже существует)
        String finalNickname = nickname;
        return keycloakClient.login(new net.proselyte.keycloak.dto.UserLoginRequest(email, generatedPassword))
                .map(tokenResponseMapper::toTokenResponse)
                .doOnNext(t -> log.info("Telegram user logged in successfully: {}", email))
                .onErrorResume(loginError -> {
                    String errorMessage = loginError.getMessage() != null ? loginError.getMessage().toLowerCase() : "";
                    
                    // Проверяем, является ли ошибка проблемой конфигурации клиента
                    if (errorMessage.contains("invalid_client") || 
                        errorMessage.contains("invalid_client_credentials") ||
                        errorMessage.contains("unauthorized_client")) {
                        log.error("Keycloak client configuration error. Check KEYCLOAK_CLIENT_SECRET. Error: {}", 
                                loginError.getMessage());
                        return Mono.error(new ApiException(
                                "Authentication service configuration error. Please contact support."));
                    }
                    
                    // Если это ошибка неверных учетных данных пользователя - регистрируем нового
                    log.info("Telegram user not found, registering new user: {}", email);
                    
                    IndividualWriteDto individualWriteDto = new IndividualWriteDto();
                    individualWriteDto.setEmail(email);
                    individualWriteDto.setNickname(finalNickname);
                    individualWriteDto.setPassword(generatedPassword);
                    individualWriteDto.setConfirmPassword(generatedPassword);

                    return register(individualWriteDto)
                            .doOnNext(t -> log.info("Telegram user registered successfully: {}", email))
                            .onErrorResume(regError -> {
                                // Если регистрация не удалась из-за дубликата - пользователь уже есть,
                                // но по какой-то причине логин не работает
                                String regErrorMsg = regError.getMessage() != null ? regError.getMessage().toLowerCase() : "";
                                if (regErrorMsg.contains("duplicate") || regErrorMsg.contains("already exists") ||
                                    regErrorMsg.contains("unique constraint")) {
                                    log.warn("User {} already exists in database but login failed. " +
                                            "Possible data inconsistency between Keycloak and Person DB.", email);
                                    return Mono.error(new ApiException(
                                            "User account exists but authentication failed. Please contact support."));
                                }
                                log.error("Failed to register Telegram user: {}", email, regError);
                                return Mono.error(regError);
                            });
                });
    }

    /**
     * Генерирует детерминированный пароль для Telegram пользователя.
     * Пароль основан на Telegram ID и bot token, что обеспечивает стабильность
     * для повторных логинов того же пользователя.
     */
    private String generateTelegramPassword(Long telegramId, String botToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = telegramId + ":" + botToken;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Преобразуем в UUID-подобную строку для совместимости
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            // Форматируем как UUID для читаемости
            String hex = sb.toString();
            return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + 
                   hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20, 32);
        } catch (NoSuchAlgorithmException e) {
            // Fallback к простому детерминированному паролю
            return "tg_" + telegramId + "_" + botToken.substring(0, Math.min(16, botToken.length()));
        }
    }

}
