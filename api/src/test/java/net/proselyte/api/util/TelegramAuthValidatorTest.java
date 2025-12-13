package net.proselyte.api.util;

import net.proselyte.individual.dto.TelegramAuthRequest;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

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
        Long authDate = System.currentTimeMillis() / 1000;
        
        String initData = buildInitData(userId, firstName, lastName, username, photoUrl, authDate, null, null, null, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNotNull(result, "Valid Telegram auth should pass validation");
        assertEquals(userId, result.getId());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(username, result.getUsername());
        assertEquals(photoUrl, result.getPhotoUrl());
        assertEquals(authDate, result.getAuthDate());
    }

    /**
     * ВАЖНЫЙ ТЕСТ: Проверяет что signature ВКЛЮЧАЕТСЯ в data_check_string
     * Это соответствует официальной библиотеке aiogram и документации Telegram
     */
    @Test
    void testValidTelegramAuthWithSignature() throws Exception {
        // Arrange: данные с signature (как в реальном Telegram Mini App)
        Long userId = 564568468L;
        String firstName = "Ivan";
        String lastName = "K";
        String username = "ivkopich";
        Long authDate = System.currentTimeMillis() / 1000;
        String queryId = "AAGUoaYhAAAAAJShpiFcmzGg";
        String signature = "ZUtslzNDdOS_KXALnJyEHdnzyH4F3_Bl0s9B6XR77hoUMDYgpVz9hlgXSSaErtg_TMOjr1zw0Czii1xQjK98BQ";
        
        String initData = buildInitData(userId, firstName, lastName, username, null, authDate, queryId, signature, null, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNotNull(result, "Telegram auth with signature should pass validation (signature must be included in hash calculation)");
        assertEquals(userId, result.getId());
        assertEquals(firstName, result.getFirstName());
        assertEquals(username, result.getUsername());
    }

    /**
     * Тест с дополнительными полями: query_id, chat_instance, chat_type
     */
    @Test
    void testValidTelegramAuthWithExtraFields() throws Exception {
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = System.currentTimeMillis() / 1000;
        String queryId = "AAGUoaYhAAAAAJShpiECLdLG";
        String chatInstance = "-6996832993589169568";
        String chatType = "private";
        
        String initData = buildInitDataWithAllFields(userId, firstName, null, null, authDate, queryId, null, chatInstance, chatType, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNotNull(result, "Telegram auth with extra fields should pass validation");
        assertEquals(userId, result.getId());
    }

    @Test
    void testInvalidHash() throws Exception {
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = System.currentTimeMillis() / 1000;

        String userJson = String.format("{\"id\":%d,\"first_name\":\"%s\"}", userId, firstName);
        String userEncoded = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String initData = String.format("auth_date=%d&user=%s&hash=invalid_hash_value", authDate, userEncoded);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNull(result, "Invalid hash should fail validation");
    }

    @Test
    void testWithOptionalFieldsOnly() throws Exception {
        Long userId = 987654321L;
        String firstName = "Jane";
        Long authDate = System.currentTimeMillis() / 1000;

        String initData = buildInitData(userId, firstName, null, null, null, authDate, null, null, null, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNotNull(result, "Auth with only required fields should pass validation");
        assertEquals(userId, result.getId());
        assertEquals(firstName, result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getUsername());
        assertNull(result.getPhotoUrl());
    }

    @Test
    void testOldAuthDate() throws Exception {
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = (System.currentTimeMillis() / 1000) - 86401; // 24 часа + 1 секунда назад

        String initData = buildInitData(userId, firstName, null, null, null, authDate, null, null, null, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNull(result, "Auth data older than 24 hours should fail validation");
    }

    @Test
    void testMissingHash() throws Exception {
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = System.currentTimeMillis() / 1000;

        String userJson = String.format("{\"id\":%d,\"first_name\":\"%s\"}", userId, firstName);
        String userEncoded = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String initData = String.format("auth_date=%d&user=%s", authDate, userEncoded);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNull(result, "Missing hash should fail validation");
    }

    @Test
    void testMissingUser() throws Exception {
        Long authDate = System.currentTimeMillis() / 1000;
        String hash = "some_hash_value";
        String initData = String.format("auth_date=%d&hash=%s", authDate, hash);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // Act
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);

        // Assert
        assertNull(result, "Missing user should fail validation");
    }

    @Test
    void testNullRequest() {
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(null, TEST_BOT_TOKEN);
        assertNull(result, "Null request should fail validation");
    }

    @Test
    void testEmptyInitData() {
        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData("");

        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, TEST_BOT_TOKEN);
        assertNull(result, "Empty initData should fail validation");
    }

    @Test
    void testNullBotToken() throws Exception {
        Long userId = 123456789L;
        String firstName = "John";
        Long authDate = System.currentTimeMillis() / 1000;

        String initData = buildInitData(userId, firstName, null, null, null, authDate, null, null, null, TEST_BOT_TOKEN);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, null);
        assertNull(result, "Null bot token should fail validation");
    }

    /**
     * Строит initData в формате Telegram WebApp
     * 
     * ВАЖНО: согласно документации и библиотеке aiogram, 
     * в data_check_string включаются ВСЕ поля кроме hash
     * (signature ВКЛЮЧАЕТСЯ!)
     */
    private String buildInitData(
        Long id, String firstName, String lastName, 
        String username, String photoUrl, Long authDate,
        String queryId, String signature, String chatInstance,
        String botToken
    ) throws Exception {
        // Строим JSON для user
        StringBuilder userJson = new StringBuilder();
        userJson.append("{");
        userJson.append("\"id\":").append(id);
        userJson.append(",\"first_name\":\"").append(firstName).append("\"");
        if (lastName != null && !lastName.isEmpty()) {
            userJson.append(",\"last_name\":\"").append(lastName).append("\"");
        }
        if (username != null && !username.isEmpty()) {
            userJson.append(",\"username\":\"").append(username).append("\"");
        }
        if (photoUrl != null && !photoUrl.isEmpty()) {
            userJson.append(",\"photo_url\":\"").append(photoUrl).append("\"");
        }
        userJson.append("}");
        
        // Собираем все параметры для data_check_string (сортируем по алфавиту)
        TreeMap<String, String> params = new TreeMap<>();
        params.put("auth_date", String.valueOf(authDate));
        params.put("user", userJson.toString());
        
        if (queryId != null) {
            params.put("query_id", queryId);
        }
        if (signature != null) {
            params.put("signature", signature);  // signature ВКЛЮЧАЕТСЯ в hash!
        }
        if (chatInstance != null) {
            params.put("chat_instance", chatInstance);
        }
        
        // Строим data check string
        StringBuilder dataCheckString = new StringBuilder();
        boolean first = true;
        for (var entry : params.entrySet()) {
            if (!first) {
                dataCheckString.append("\n");
            }
            dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        // Вычисляем hash
        String hash = calculateHash(dataCheckString.toString(), botToken);
        
        // Строим initData
        String userEncoded = URLEncoder.encode(userJson.toString(), StandardCharsets.UTF_8);
        StringBuilder initData = new StringBuilder();
        initData.append("auth_date=").append(authDate);
        if (queryId != null) {
            initData.append("&query_id=").append(queryId);
        }
        if (signature != null) {
            initData.append("&signature=").append(signature);
        }
        if (chatInstance != null) {
            initData.append("&chat_instance=").append(chatInstance);
        }
        initData.append("&user=").append(userEncoded);
        initData.append("&hash=").append(hash);
        
        return initData.toString();
    }

    /**
     * Строит initData со всеми возможными полями
     */
    private String buildInitDataWithAllFields(
        Long id, String firstName, String lastName, String username,
        Long authDate, String queryId, String signature,
        String chatInstance, String chatType,
        String botToken
    ) throws Exception {
        StringBuilder userJson = new StringBuilder();
        userJson.append("{");
        userJson.append("\"id\":").append(id);
        userJson.append(",\"first_name\":\"").append(firstName).append("\"");
        if (lastName != null) {
            userJson.append(",\"last_name\":\"").append(lastName).append("\"");
        }
        if (username != null) {
            userJson.append(",\"username\":\"").append(username).append("\"");
        }
        userJson.append("}");
        
        TreeMap<String, String> params = new TreeMap<>();
        params.put("auth_date", String.valueOf(authDate));
        params.put("user", userJson.toString());
        
        if (queryId != null) params.put("query_id", queryId);
        if (signature != null) params.put("signature", signature);
        if (chatInstance != null) params.put("chat_instance", chatInstance);
        if (chatType != null) params.put("chat_type", chatType);
        
        StringBuilder dataCheckString = new StringBuilder();
        boolean first = true;
        for (var entry : params.entrySet()) {
            if (!first) dataCheckString.append("\n");
            dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        String hash = calculateHash(dataCheckString.toString(), botToken);
        
        String userEncoded = URLEncoder.encode(userJson.toString(), StandardCharsets.UTF_8);
        StringBuilder initData = new StringBuilder();
        initData.append("auth_date=").append(authDate);
        if (chatInstance != null) initData.append("&chat_instance=").append(chatInstance);
        if (chatType != null) initData.append("&chat_type=").append(chatType);
        if (queryId != null) initData.append("&query_id=").append(queryId);
        if (signature != null) initData.append("&signature=").append(signature);
        initData.append("&user=").append(userEncoded);
        initData.append("&hash=").append(hash);
        
        return initData.toString();
    }

    /**
     * Вычисляет hash для data check string согласно алгоритму Telegram WebApp
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private String calculateHash(String dataCheckString, String botToken) throws Exception {
        javax.crypto.Mac hmac1 = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec webAppDataKeySpec = new javax.crypto.spec.SecretKeySpec(
            "WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac1.init(webAppDataKeySpec);
        byte[] secretKey = hmac1.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
        
        javax.crypto.Mac hmac2 = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secretKey, "HmacSHA256");
        hmac2.init(secretKeySpec);
        byte[] hash = hmac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
