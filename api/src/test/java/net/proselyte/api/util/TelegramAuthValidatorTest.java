package net.proselyte.api.util;

import net.proselyte.individual.dto.TelegramAuthRequest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class TelegramAuthValidatorTest {

    private static final String TEST_BOT_TOKEN = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11";

    @Test
    void testValidTelegramAuth() throws Exception {
        // Arrange: создаем валидные тестовые данные
        Long userId = 123456789L;
        String firstName = "John";
        String lastName = "Doe";
        String username = "johndoe";
        String photoUrl = "https://t.me/i/userpic/320/johndoe.jpg";
        Long authDate = 1733857180L; // фиксированная дата для теста
        
        // Генерируем правильный hash согласно алгоритму Telegram
        String correctHash = generateTelegramHash(
            userId, firstName, lastName, username, photoUrl, authDate, TEST_BOT_TOKEN
        );

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setId(userId);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setUsername(username);
        request.setPhotoUrl(photoUrl);
        request.setAuthDate(authDate);
        request.setHash(correctHash);

        // Act
        boolean result = TelegramAuthValidator.validate(request, TEST_BOT_TOKEN);

        // Assert
        assertTrue(result, "Valid Telegram auth should pass validation");
    }

    @Test
    void testInvalidHash() throws Exception {
        // Arrange
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = System.currentTimeMillis() / 1000;

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setId(userId);
        request.setFirstName(firstName);
        request.setAuthDate(authDate);
        request.setHash("invalid_hash_value");

        // Act
        boolean result = TelegramAuthValidator.validate(request, TEST_BOT_TOKEN);

        // Assert
        assertFalse(result, "Invalid hash should fail validation");
    }

    @Test
    void testWithOptionalFieldsOnly() throws Exception {
        // Arrange: тест с минимальным набором полей
        Long userId = 987654321L;
        String firstName = "Jane";
        Long authDate = System.currentTimeMillis() / 1000;

        String correctHash = generateTelegramHash(
            userId, firstName, null, null, null, authDate, TEST_BOT_TOKEN
        );

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setId(userId);
        request.setFirstName(firstName);
        request.setAuthDate(authDate);
        request.setHash(correctHash);

        // Act
        boolean result = TelegramAuthValidator.validate(request, TEST_BOT_TOKEN);

        // Assert
        assertTrue(result, "Auth with only required fields should pass validation");
    }

    @Test
    void testOldAuthDate() throws Exception {
        // Arrange: данные старше 24 часов
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = (System.currentTimeMillis() / 1000) - 86401; // 24 часа + 1 секунда назад

        String correctHash = generateTelegramHash(
            userId, firstName, null, null, null, authDate, TEST_BOT_TOKEN
        );

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setId(userId);
        request.setFirstName(firstName);
        request.setAuthDate(authDate);
        request.setHash(correctHash);

        // Act
        boolean result = TelegramAuthValidator.validate(request, TEST_BOT_TOKEN);

        // Assert
        assertFalse(result, "Auth data older than 24 hours should fail validation");
    }

    @Test
    void testEmptyStringsVsNull() throws Exception {
        // Arrange: тест с пустыми строками вместо null
        // Это частая причина ошибок - фронтенд может отправить "" вместо null
        Long userId = 111222333L;
        String firstName = "Test";
        Long authDate = System.currentTimeMillis() / 1000;

        // Генерируем hash БЕЗ пустых полей
        String correctHash = generateTelegramHash(
            userId, firstName, null, null, null, authDate, TEST_BOT_TOKEN
        );

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setId(userId);
        request.setFirstName(firstName);
        request.setLastName(""); // пустая строка вместо null
        request.setUsername(""); // пустая строка вместо null
        request.setPhotoUrl(""); // пустая строка вместо null
        request.setAuthDate(authDate);
        request.setHash(correctHash);

        // Act
        boolean result = TelegramAuthValidator.validate(request, TEST_BOT_TOKEN);

        // Assert
        assertTrue(result, "Empty strings should be treated as null and excluded from validation");
    }

    /**
     * Вспомогательный метод для генерации правильного Telegram hash
     * согласно алгоритму из документации: https://core.telegram.org/widgets/login#checking-authorization
     */
    private String generateTelegramHash(
        Long id, String firstName, String lastName, 
        String username, String photoUrl, Long authDate, 
        String botToken
    ) throws Exception {
        // Создаем data_check_string
        StringBuilder dataCheckString = new StringBuilder();
        
        dataCheckString.append("auth_date=").append(authDate).append("\n");
        dataCheckString.append("first_name=").append(firstName).append("\n");
        dataCheckString.append("id=").append(id);
        
        if (lastName != null && !lastName.isEmpty()) {
            dataCheckString.append("\n").append("last_name=").append(lastName);
        }
        if (photoUrl != null && !photoUrl.isEmpty()) {
            dataCheckString.append("\n").append("photo_url=").append(photoUrl);
        }
        if (username != null && !username.isEmpty()) {
            dataCheckString.append("\n").append("username=").append(username);
        }

        // Вычисляем secret_key = SHA256(bot_token)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

        // Вычисляем HMAC-SHA256
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
        hmac.init(secretKeySpec);
        byte[] hash = hmac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));

        // Преобразуем в hex
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

