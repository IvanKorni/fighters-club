package net.proselyte.api.environment.config.testcontainer.data;

import org.springframework.stereotype.Component;

import net.proselyte.individual.dto.IndividualWriteDto;
import net.proselyte.individual.dto.TelegramAuthRequest;
import net.proselyte.individual.dto.UserLoginRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;


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

    public TelegramAuthRequest buildTelegramAuthRequest(String botToken) {
        // Используем фиксированный Telegram ID для тестов
        Long telegramId = 123456789L;
        String firstName = "John";
        String lastName = "Doe";
        String username = "johndoe";
        Long authDate = System.currentTimeMillis() / 1000;

        // Строим initData в формате Telegram WebApp
        String initData = buildInitData(telegramId, firstName, lastName, username, authDate, null, null, botToken);

        var request = new TelegramAuthRequest();
        request.setInitData(initData);

        return request;
    }

    /**
     * Создает запрос с signature (как в реальном Telegram Mini App)
     */
    public TelegramAuthRequest buildTelegramAuthRequestWithSignature(String botToken) {
        Long telegramId = 123456789L;
        String firstName = "John";
        String lastName = "Doe";
        String username = "johndoe";
        Long authDate = System.currentTimeMillis() / 1000;
        String queryId = "AAGUoaYhAAAAAJShpiECLdLG";
        String signature = "test_signature_value_for_testing";

        String initData = buildInitData(telegramId, firstName, lastName, username, authDate, queryId, signature, botToken);

        var request = new TelegramAuthRequest();
        request.setInitData(initData);

        return request;
    }

    /**
     * Строит initData в формате Telegram WebApp
     * Алгоритм должен соответствовать TelegramAuthValidator.validateAndParse()
     * 
     * ВАЖНО: В data_check_string включаются ВСЕ поля кроме hash!
     * (signature ВКЛЮЧАЕТСЯ в расчет хеша согласно официальной библиотеке aiogram)
     */
    private String buildInitData(Long id, String firstName, String lastName, 
                                 String username, Long authDate, 
                                 String queryId, String signature,
                                 String botToken) {
        try {
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
            userJson.append("}");
            
            // URL-encode user JSON для initData
            String userEncoded = URLEncoder.encode(userJson.toString(), StandardCharsets.UTF_8);
            
            // Строим data check string для вычисления hash
            // ВАЖНО: Ключи должны быть отсортированы в алфавитном порядке
            // ВАЖНО: signature ВКЛЮЧАЕТСЯ в расчет (исключается ТОЛЬКО hash)
            TreeMap<String, String> sortedParams = new TreeMap<>();
            sortedParams.put("auth_date", String.valueOf(authDate));
            sortedParams.put("user", userJson.toString());
            
            if (queryId != null) {
                sortedParams.put("query_id", queryId);
            }
            if (signature != null) {
                sortedParams.put("signature", signature);
            }
            
            // Строим data check string в формате key=value\nkey=value
            StringBuilder dataCheckString = new StringBuilder();
            boolean first = true;
            for (var entry : sortedParams.entrySet()) {
                if (!first) {
                    dataCheckString.append("\n");
                }
                dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            
            // Вычисляем hash
            String hash = calculateHash(dataCheckString.toString(), botToken);
            
            // Строим initData
            StringBuilder initData = new StringBuilder();
            initData.append("auth_date=").append(authDate);
            if (queryId != null) {
                initData.append("&query_id=").append(queryId);
            }
            if (signature != null) {
                initData.append("&signature=").append(signature);
            }
            initData.append("&user=").append(userEncoded);
            initData.append("&hash=").append(hash);
            
            return initData.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build initData", e);
        }
    }

    /**
     * Вычисляет hash для data check string согласно алгоритму Telegram WebApp
     * Должен соответствовать TelegramAuthValidator.calculateHash()
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private String calculateHash(String dataCheckString, String botToken) {
        try {
            // 1. Вычисляем secret_key = HMAC-SHA256("WebAppData", bot_token)
            // Ключ: "WebAppData", Данные: bot_token
            Mac hmac1 = Mac.getInstance("HmacSHA256");
            SecretKeySpec webAppDataKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac1.init(webAppDataKeySpec);
            byte[] secretKey = hmac1.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            
            // 2. Вычисляем hash = HMAC-SHA256(data-check-string, secret_key)
            Mac hmac2 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            hmac2.init(secretKeySpec);
            byte[] hash = hmac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate Telegram hash", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
