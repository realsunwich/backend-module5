package com.example.demo.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class LineBotService {

    // 1. ใส่ Token ยาวๆ ของคุณที่นี่
    private final String CHANNEL_ACCESS_TOKEN = "AF+7IpMElwszbU+ClDU1WI4AOu5TSaiwB4hbp7sCdWSUJRgR9wvyXKdML69EEHknDi2ssGBq5yoeU2omxK33cLTMVzSAiRCcfJ/4infsaNH6CgnYBK6HByLyPpyAVBAbVYpbxlZCYnkta2SNWWNDdgdB04t89/1O/w1cDnyilFU=";

    private final String TARGET_ID = "U73bd48ef10bf997b6b0c54aec2037f49";

    public void sendPushMessage(String messageText) {
        String url = "https://api.line.me/v2/bot/message/push";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + CHANNEL_ACCESS_TOKEN);

        // สร้าง Body JSON
        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("type", "text");
        textMessage.put("text", messageText);

        Map<String, Object> body = new HashMap<>();
        body.put("to", TARGET_ID);
        body.put("messages", Collections.singletonList(textMessage));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Line notification sent successfully.");
        } catch (Exception e) {
            System.err.println("Error sending Line notification: " + e.getMessage());
        }
    }
}