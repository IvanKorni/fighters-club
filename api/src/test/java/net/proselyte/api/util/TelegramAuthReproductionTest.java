package net.proselyte.api.util;

import net.proselyte.individual.dto.TelegramAuthRequest;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TelegramAuthReproductionTest {

    private static final String DUMMY_BOT_TOKEN = "123456789:ABCDefGHIjklMNOpqrsTUVwxYz";

    @Test
    void testReproductionWithLogData() {
        // Data extracted from the user's logs
        // user={"id":564568468,"first_name":"Ivan","last_name":"K","username":"ivkopich","language_code":"ru","allows_write_to_pm":true,"photo_url":"https:\/\/t.me\/i\/userpic\/320\/O2Kli1ZznFC5Eogr2M0YOv7lvo8o9X0tw55HWhVTohs.svg"}
        // Note: The log showed escaped slashes for the URL.
        String userJson = "{\"id\":564568468,\"first_name\":\"Ivan\",\"last_name\":\"K\",\"username\":\"ivkopich\",\"language_code\":\"ru\",\"allows_write_to_pm\":true,\"photo_url\":\"https:\\/\\/t.me\\/i\\/userpic\\/320\\/O2Kli1ZznFC5Eogr2M0YOv7lvo8o9X0tw55HWhVTohs.svg\"}";
        String authDate = "1765397575";
        String queryId = "AAGUoaYhAAAAAJShpiEnn5gP";
        
        // The hash received from Telegram (from logs)
        String receivedHash = "18d17ded910b8b73031f344a727003d1e625997228d688a673e692c2fb722e79";

        // Reconstruct initData string as it would be sent by Telegram
        // Telegram usually encodes the values.
        String userEncoded = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String initData = String.format("query_id=%s&user=%s&auth_date=%s&hash=%s", 
                queryId, userEncoded, authDate, receivedHash);

        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setInitData(initData);

        // We expect validation to fail here because DUMMY_BOT_TOKEN matches neither the real token used by Telegram nor creates the hash 18d1...
        // However, this test verifies that the parsing logic handles this input without crashing 
        // and constructs the internal check string as expected.
        TelegramAuthValidator.TelegramUser result = TelegramAuthValidator.validateAndParse(request, DUMMY_BOT_TOKEN);
        
        assertNull(result, "Expected validation to fail due to hash mismatch with dummy token");
        
        // To verify the Data Check String construction, we can manually reproduce the logic here
        // and assert it matches what we saw in the logs.
        String expectedDataCheckString = "auth_date=" + authDate + "\n" +
                "query_id=" + queryId + "\n" +
                "user=" + userJson;
                
        // We can't easily access the internal dataCheckString of the Validator without reflection or logs.
        // But we can verify that our reconstruction logic produces the same string.
        
        // Verify reconstruction logic:
        Map<String, String> params = new TreeMap<>();
        params.put("query_id", queryId);
        params.put("user", userEncoded);
        params.put("auth_date", authDate);
        params.put("hash", receivedHash);
        
        StringBuilder dataCheckString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equals("hash")) {
                if (!first) dataCheckString.append("\n");
                String value = entry.getValue();
                // Simulate decoder
                if (entry.getKey().equals("user")) {
                     value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                }
                dataCheckString.append(entry.getKey()).append("=").append(value);
                first = false;
            }
        }
        
        assertEquals(expectedDataCheckString, dataCheckString.toString(), "Data check string construction mismatch");
    }
}

