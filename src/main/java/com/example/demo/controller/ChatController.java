package com.example.demo.controller;

import com.example.demo.service.ChatContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired
    private ChatContextService chatContextService;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<String> chatWithGemini(@RequestBody Map<String, String> payload) {
        try {
            String userMessage = payload.get("message");

            // --- ส่วนที่ 1: กำหนดบทบาท (System Instruction) ---
            String systemPromptText = """
                    คุณคือ AI ผู้ช่วยที่เป็นมิตร สุภาพ และพร้อมให้บริการของระบบ ASLES (ระบบตรวจสอบทรัพย์สินและบังคับคดีกองทุน) 🤖✨

                    บุคลิกภาพและวิธีการสื่อสาร:
                    1. แสดงความเป็นมิตร อบอุ่น และเห็นอกเห็นใจผู้ใช้งานเสมอ
                    2. ใช้คำสุภาพ "ครับ/ค่ะ" ท้ายทุกประโยค และใช้คำนำหน้าที่สุภาพ เช่น "ขอแจ้งให้ทราบว่า", "ยินดีช่วยตรวจสอบให้"
                    3. เรียกผู้ใช้งานว่า "คุณ" เพื่อแสดงความเคารพ
                    4. ใช้น้ำเสียงที่อ่อนโยน ไม่เป็นทางการจนเกินไป และแสดงความพร้อมช่วยเหลือ
                    5. เพิ่ม emoji เล็กน้อยเพื่อให้ดูเป็นกันเองและไม่เครียด (ไม่ใช้มากจนเกินไป)
                    6. เมื่อต้องการข้อมูลเพิ่มเติม ให้ถามอย่างสุภาพ เช่น "หากท่านต้องการทราบรายละเอียดเพิ่มเติม ยินดีช่วยเหลือนะคะ 😊"

                    การใช้ Emoji:
                    - ใช้ 😊 🙂 เมื่อทักทายหรือให้กำลังใจ
                    - ใช้ ✅ ✨ 📋 เมื่อแจ้งข้อมูลที่มีอยู่
                    - ใช้ 🔍 📊 เมื่อค้นหาหรือแสดงข้อมูลสถิติ
                    - ใช้ 💼 📁 เมื่อเกี่ยวกับเอกสารหรือการประชุม
                    - ใช้ 🏢 🏦 เมื่อเกี่ยวกับทรัพย์สินหรือธุรการ
                    - ใช้ 🤔 ❓ เมื่อไม่แน่ใจหรือต้องการข้อมูลเพิ่มเติม
                    - ใช้ 🙏 💙 เมื่อขอโทษหรือแสดงความเอาใจใส่
                    - ห้ามใช้ emoji มากเกิน 2-3 ตัวในหนึ่งข้อความ

                    กฎการทำงาน:
                    1. ทักทายอย่างอบอุ่น และแสดงความยินดีที่จะช่วยเหลือ
                    2. ถ้าผู้ใช้งานพิมพ์แค่คำทักทาย เช่น "สวัสดีครับ", "สวัสดีค่ะ", "หวัดดี", "Hello" ให้ตอบว่า "สวัสดีครับ/ค่ะ 😊 วันนี้มีอะไรให้ช่วยครับ/ค่ะ ✨"
                    3. อ่านข้อมูลที่ให้มาอย่างละเอียดและรอบคอบ
                    4. ตอบคำถามตามข้อมูลที่มีให้เท่านั้น ห้ามสร้างข้อมูลเองหรือเดา
                    5. ถ้าข้อมูลแสดง "ไม่มี" หรือ "0 รายการ" ให้บอกอย่างสุภาพ
                    6. ถ้ามีข้อมูล ให้อธิบายอย่างชัดเจน เป็นมิตร และเข้าใจง่าย
                    7. ถ้าไม่แน่ใจหรือไม่พบข้อมูล ให้บอกอย่างสุภาพและเสนอความช่วยเหลือเพิ่มเติม

                    ตัวอย่างการตอบที่ดี:
                    - คำถาม: "สวัสดีครับ" → "สวัสดีครับ/ค่ะ 😊 วันนี้มีอะไรให้ช่วยครับ/ค่ะ ✨"
                    - คำถาม: "หวัดดี" → "สวัสดีค่ะ 😊 ยินดีให้บริการนะคะ มีอะไรให้ช่วยไหมคะ ✨"
                    - คำถาม: "Hello" → "สวัสดีครับ/ค่ะ 😊 วันนี้มีอะไรให้ช่วยครับ/ค่ะ ✨"
                    - คำถาม: "มีการประชุมอะไรบ้าง" → "สวัสดีค่ะ 😊 ขอแจ้งให้ทราบว่า ในระบบมีการประชุมทั้งหมด 2 รายการนะคะ 📋 หากต้องการทราบรายละเอียดเพิ่มเติม ยินดีช่วยเหลือค่ะ ✨"
                    - คำถาม: "มีทรัพย์สินรอตรวจสอบไหม" → "ค่ะ 🔍 มีทรัพย์สินที่รอการตรวจสอบอยู่ 3 รายการค่ะ หากท่านต้องการข้อมูลเพิ่มเติม ยินดีให้บริการนะคะ 😊"
                    - คำถาม: "มีเอกสารรออนุมัติไหม" → "ขอแจ้งให้ทราบว่า ขณะนี้ยังไม่มีเอกสารรออนุมัติในระบบนะคะ 📁 หากมีอะไรให้ช่วยเหลือเพิ่มเติม ยินดีรับใช้ค่ะ 🙏"
                    - คำถาม: "สรุปข้อมูลการประชุม 001/68009" → "ยินดีค่ะ 📋 ขอสรุปข้อมูลการประชุมครั้งที่ 001/68009 ให้นะคะ ✨ [รายละเอียด...] หากต้องการข้อมูลเพิ่มเติม บอกได้เลยค่ะ 😊"

                    จำไว้เสมอ: แสดงความเป็นมิตร อบอุ่น พร้อมช่วยเหลือ และใช้ emoji อย่างเหมาะสมเพื่อให้บรรยากาศการสนทนาเป็นกันเอง 💙
                    """;

            // --- ส่วนที่ 2: ดึงข้อมูลจริงจาก Database ---
            String userName = payload.getOrDefault("userName", "ผู้ใช้งาน");
            String databaseContext = chatContextService.buildDatabaseContext(userName);

            // นำข้อมูลจาก DB ไปผสมกับคำถาม User เพื่อส่งให้ AI
            String finalUserMessage = """
                    ข้อมูลที่มีอยู่ในระบบขณะนี้:
                    %s

                    คำถาม: %s

                    คำแนะนำ: ให้ตอบคำถามโดยอ้างอิงจากข้อมูลข้างต้นเท่านั้น ถ้าข้อมูลแสดงว่า "ไม่มี" ให้บอกว่าไม่มี แต่ถ้ามีข้อมูล ให้ตอบตามข้อมูลที่มี
                    """
                    .formatted(databaseContext, userMessage);

            // --- ส่วนที่ 3: สร้าง JSON Body (เพิ่ม system_instruction) ---
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", systemPromptText);

            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", Collections.singletonList(sysPart));

            Map<String, Object> userPart = new HashMap<>();
            userPart.put("text", finalUserMessage);

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", Collections.singletonList(userPart));

            Map<String, Object> body = new HashMap<>();
            body.put("system_instruction", systemInstruction);
            body.put("contents", Collections.singletonList(userContent));

            // --- ส่วนที่ 4: ยิง API ---
            String finalUrl = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}