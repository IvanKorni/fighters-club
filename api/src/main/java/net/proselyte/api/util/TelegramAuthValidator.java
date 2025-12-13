package net.proselyte.api.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.proselyte.individual.dto.TelegramAuthRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Утилита для валидации данных аутентификации Telegram WebApp
 * Согласно документации: https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
 */
@Slf4j
public class TelegramAuthValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEBAPP_DATA_CONSTANT = "WebAppData";
    
    /**
     * Флаг для отключения проверки хеша (только для отладки!)
     * Устанавливается через переменную окружения TELEGRAM_SKIP_HASH_VALIDATION=true
     */
    private static final boolean SKIP_HASH_VALIDATION = 
        "true".equalsIgnoreCase(System.getenv("TELEGRAM_SKIP_HASH_VALIDATION"));

    /**
     * Результат валидации и парсинга Telegram auth данных
     */
    @Data
    @AllArgsConstructor
    public static class TelegramUser {
        private Long id;
        private String firstName;
        private String lastName;
        private String username;
        private String photoUrl;
        private Long authDate;
    }

    /**
     * Проверяет подлинность данных из Telegram WebApp и парсит их
     * 
     * Алгоритм валидации для Telegram WebApp:
     * 1. Парсим initData как URL-encoded строку
     * 2. Извлекаем все пары key=value, кроме hash (signature ВКЛЮЧАЕТСЯ!)
     * 3. Сортируем все пары по ключу в алфавитном порядке
     * 4. URL-decode все значения
     * 5. Строим data-check-string: объединяем через \n в формате key=value
     * 6. Вычисляем secret_key = HMAC-SHA256(bot_token, "WebAppData") где "WebAppData" - ключ
     * 7. Вычисляем hash = HMAC-SHA256(data-check-string, secret_key)
     * 8. Сравниваем с переданным hash
     * 
     * ВАЖНО: Согласно официальной библиотеке aiogram, поле signature НЕ исключается
     * из data-check-string. Исключается ТОЛЬКО hash.
     *
     * @param request данные от Telegram WebApp (содержит initData)
     * @param botToken токен бота
     * @return TelegramUser если данные валидны, null если невалидны
     */
    public static TelegramUser validateAndParse(TelegramAuthRequest request, String botToken) {
        try {
            // Проверяем обязательные поля на null
            if (request == null) {
                log.error("Telegram auth request is null");
                return null;
            }
            
            if (request.getInitData() == null || request.getInitData().isEmpty()) {
                log.error("Telegram auth request missing required field: initData");
                return null;
            }

            if (botToken == null || botToken.isBlank()) {
                log.error("Bot token is missing or empty");
                return null;
            }
            
            String initData = request.getInitData();
            
            // Парсим initData как URL-encoded строку
            Map<String, String> params = parseInitData(initData);
            
            String hash = params.get("hash");
            if (hash == null || hash.isEmpty()) {
                log.error("Telegram auth request missing required field: hash in initData");
                return null;
            }
            
            // Строим data check string из всех полей кроме hash
            // Согласно документации Telegram и официальной библиотеке aiogram:
            // ВАЖНО: signature НЕ исключается из расчета хеша!
            // Сортируем по ключу в алфавитном порядке
            TreeMap<String, String> sortedParams = new TreeMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                // Исключаем ТОЛЬКО hash из расчета
                if (!"hash".equals(key)) {
                    sortedParams.put(key, entry.getValue());
                }
            }
            
            // Строим data check string
            // Все значения должны быть URL-decoded
            StringBuilder dataCheckString = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (!first) {
                    dataCheckString.append("\n");
                }
                String key = entry.getKey();
                // URL-decode все значения (как делает Python's parse_qs)
                String value = URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8);
                
                dataCheckString.append(key).append("=").append(value);
                first = false;
            }
            
            // Вычисляем hash
            // Важно: botToken не должен содержать пробельных символов (например, \n в конце)
            String calculatedHash = calculateHash(dataCheckString.toString(), botToken.trim());
            
            // Логируем информацию о валидации
            String tokenId = botToken != null && botToken.contains(":") 
                ? botToken.split(":")[0] 
                : "unknown";
            boolean hashMatches = calculatedHash.equalsIgnoreCase(hash);
            
            log.info("Telegram hash validation: tokenId={}, hashMatches={}, skipValidation={}", 
                tokenId, hashMatches, SKIP_HASH_VALIDATION);
            log.debug("Hash details - received: {}, calculated: {}", hash, calculatedHash);
            log.debug("Data check string fields: {}", sortedParams.keySet());
            
            // Сравниваем с переданным hash
            if (!hashMatches) {
                log.warn("Telegram auth hash mismatch. Token ID: {} (len={}). Expected: {}, got: {}. Fields: {}", 
                    tokenId, botToken != null ? botToken.length() : 0, calculatedHash, hash, sortedParams.keySet());
                
                // Если включен режим отладки - пропускаем проверку хеша
                if (SKIP_HASH_VALIDATION) {
                    log.warn("⚠️ SKIP_HASH_VALIDATION is enabled! Bypassing hash check for debugging. DO NOT USE IN PRODUCTION!");
                } else {
                    return null;
                }
            }
            
            // Извлекаем auth_date для проверки времени
            String authDateStr = params.get("auth_date");
            if (authDateStr == null || authDateStr.isEmpty()) {
                log.error("Telegram auth request missing required field: auth_date in initData");
                return null;
            }
            
            long authDate;
            try {
                authDate = Long.parseLong(authDateStr);
            } catch (NumberFormatException e) {
                log.error("Invalid auth_date format: {}", authDateStr);
                return null;
            }
            
            // Проверяем что данные не старше 1 дня (опционально, для безопасности)
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = currentTime - authDate;
            
            if (timeDiff > 86400) { // 24 часа
                log.warn("Telegram auth data is too old. Auth date: {}, current: {}, diff: {} seconds", 
                    authDate, currentTime, timeDiff);
                return null;
            }
            
            // Парсим user JSON
            String userEncoded = params.get("user");
            if (userEncoded == null || userEncoded.isEmpty()) {
                log.error("Telegram auth request missing required field: user in initData");
                return null;
            }
            
            String userJson = URLDecoder.decode(userEncoded, StandardCharsets.UTF_8);
            JsonObject user = JsonParser.parseString(userJson).getAsJsonObject();
            
            if (!user.has("id") || !user.has("first_name")) {
                log.error("Telegram user JSON missing required fields: id or first_name");
                return null;
            }
            
            Long id = user.get("id").getAsLong();
            String firstName = user.get("first_name").getAsString();
            String lastName = user.has("last_name") ? user.get("last_name").getAsString() : null;
            String username = user.has("username") ? user.get("username").getAsString() : null;
            String photoUrl = user.has("photo_url") ? user.get("photo_url").getAsString() : null;
            
            log.info("Telegram auth validation successful for user ID: {}", id);
            
            return new TelegramUser(id, firstName, lastName, username, photoUrl, authDate);
            
        } catch (Exception e) {
            log.error("Error validating Telegram auth data", e);
            return null;
        }
    }

    /**
     * Парсит initData как URL-encoded строку
     * @param initData URL-encoded строка вида "key1=value1&key2=value2&..."
     * @return Map с парами ключ-значение
     */
    private static Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new TreeMap<>();
        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Вычисляет hash для data check string согласно алгоритму Telegram WebApp
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private static String calculateHash(String dataCheckString, String botToken) {
        try {
            // 1. Вычисляем secret_key = HMAC-SHA256(bot_token, "WebAppData")
            // Ключ: "WebAppData", Данные: bot_token
            Mac hmac1 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec webAppDataKeySpec = new SecretKeySpec(WEBAPP_DATA_CONSTANT.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmac1.init(webAppDataKeySpec);
            byte[] secretKey = hmac1.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            
            // 2. Вычисляем hash = HMAC-SHA256(data-check-string, secret_key)
            Mac hmac2 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            hmac2.init(secretKeySpec);
            byte[] hash = hmac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            // Преобразуем в hex строку
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
