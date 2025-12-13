package net.proselyte.api.util;

import lombok.extern.slf4j.Slf4j;
import net.proselyte.individual.dto.TelegramAuthRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Утилита для валидации данных аутентификации Telegram
 * Согласно документации: https://core.telegram.org/widgets/login#checking-authorization
 */
@Slf4j
public class TelegramAuthValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Проверяет подлинность данных из Telegram Widget
     *
     * @param request данные от Telegram
     * @param botToken токен бота
     * @return true если данные валидны
     */
    public static boolean validate(TelegramAuthRequest request, String botToken) {
        try {
            // Создаем список пар ключ=значение
            List<String> dataCheckArr = new ArrayList<>();
            
            dataCheckArr.add("auth_date=" + request.getAuthDate());
            dataCheckArr.add("first_name=" + request.getFirstName());
            dataCheckArr.add("id=" + request.getId());
            
            if (request.getLastName() != null) {
                dataCheckArr.add("last_name=" + request.getLastName());
            }
            if (request.getPhotoUrl() != null) {
                dataCheckArr.add("photo_url=" + request.getPhotoUrl());
            }
            if (request.getUsername() != null) {
                dataCheckArr.add("username=" + request.getUsername());
            }

            // Сортируем в алфавитном порядке
            Collections.sort(dataCheckArr);

            // Объединяем в одну строку через \n
            String dataCheckString = String.join("\n", dataCheckArr);

            // Получаем secret_key = SHA256(bot_token)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

            // Вычисляем HMAC-SHA256
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            // Преобразуем в hex строку
            String calculatedHash = bytesToHex(hash);

            // Сравниваем с переданным hash
            boolean isValid = calculatedHash.equalsIgnoreCase(request.getHash());
            
            if (!isValid) {
                log.warn("Telegram auth validation failed. Expected hash: {}, got: {}", 
                    calculatedHash, request.getHash());
            }
            
            // Также проверяем что данные не старше 1 дня (опционально, для безопасности)
            long currentTime = System.currentTimeMillis() / 1000;
            long authTime = request.getAuthDate();
            long timeDiff = currentTime - authTime;
            
            if (timeDiff > 86400) { // 24 часа
                log.warn("Telegram auth data is too old. Auth date: {}, current: {}, diff: {} seconds", 
                    authTime, currentTime, timeDiff);
                return false;
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating Telegram auth data", e);
            return false;
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

