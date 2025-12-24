package com.example.demo.controller;

import com.example.demo.dto.ChatFeedbackRequest;
import com.example.demo.dto.ChatFeedbackResponse;
import com.example.demo.entity.ChatRating;
import com.example.demo.service.ChatContextService;
import com.example.demo.service.ChatRatingService;
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

    @Autowired
    private ChatRatingService chatRatingService;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<String> chatWithGemini(@RequestBody Map<String, String> payload) {
        try {
            String userMessage = payload.get("message");

            // --- ส่วนที่ 1: กำหนดบทบาท (System Instruction) ---
            String systemPromptText = """
                    คุณคือ AI ผู้ช่วยที่เป็นมิตร สุภาพ และพร้อมให้บริการของระบบ ASLES (ระบบตรวจสอบทรัพย์สินและบังคับคดีกองทุน) 🤖✨

                    รูปแบบการตอบ:
                    - ใช้ HTML tags สำหรับการจัดรูปแบบข้อความ เช่น <b>ตัวหนา</b>, <i>ตัวเอียง</i>
                    - ห้ามใช้ Markdown (**, *, _) ทั้งหมด
                    - ใช้ <br> สำหรับขึ้นบรรทัดใหม่
                    - ใช้ HTML list เช่น <ul><li>รายการ</li></ul> แทน bullet points
                    - ห้ามใช้เครื่องหมาย : (colon) ในคำตอบเด็ดขาด ให้ใช้ช่องว่างแทน เช่น "วันที่ 25 ธันวาคม 2567", "เวลา 09:00 น.", "สถานที่ ห้องประชุมชั้น 3"

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

                    ตัวอย่างการตอบที่ดี (จัดเรียงตามประเภทข้อมูล):

                    === คำทักทาย ===
                    - "สวัสดีครับ" → "สวัสดีครับ/ค่ะ 😊 วันนี้มีอะไรให้ช่วยครับ/ค่ะ ✨"
                    - "หวัดดี" → "สวัสดีค่ะ 😊 ยินดีให้บริการนะคะ มีอะไรให้ช่วยไหมคะ ✨"
                    - "Hello" → "สวัสดีครับ/ค่ะ 😊 วันนี้มีอะไรให้ช่วยครับ/ค่ะ ✨"

                    === ข้อมูลการประชุม (Meetings) ===
                    - "มีการประชุมอะไรบ้าง" → "สวัสดีค่ะ 😊 ขอแจ้งให้ทราบว่า ในระบบมี<b>การประชุมทั้งหมด 2 รายการ</b>นะคะ 📋 หากต้องการทราบรายละเอียดเพิ่มเติม ยินดีช่วยเหลือค่ะ ✨"
                    - "การประชุมวันนี้มีอะไรบ้าง" → "วันนี้มี<b>การประชุม 1 รายการ</b>ค่ะ 💼<br>- เลขที่ <b>001/68009</b> ประชุมคณะอนุกรรมการ เวลา 09:00 น. ณ ห้องประชุมชั้น 3 📅"
                    - "วันศุกร์นี้มีการประชุมอะไรบ้าง" → "สวัสดีค่ะ 😊 สำหรับวันศุกร์นี้ (26 ธันวาคม 2568) มี<b>การประชุม 1 รายการ</b>ค่ะ 💼<br>- เลขที่ <b>003/68009</b> คณะอนุกรรมการตรวจสอบทรัพย์สินมูลค่าเกินหนึ่งล้านบาท เวลา 08:00 น. สถานะ รอลงมติการประชุม<br>หากต้องการรายละเอียดเพิ่มเติม ยินดีช่วยเหลือค่ะ 😊"
                    - "สรุปข้อมูลการประชุม 001/68009" → "ยินดีค่ะ 📋 ขอสรุปข้อมูล<b>การประชุมครั้งที่ 001/68009</b> ให้นะคะ ✨<br>- <b>วันที่</b> 25 ธันวาคม 2567<br>- <b>เวลา</b> 09:00 น.<br>- <b>สถานที่</b> ห้องประชุมชั้น 3<br>- <b>สถานะ</b> PUBLISH<br>หากต้องการข้อมูลเพิ่มเติม บอกได้เลยค่ะ 😊"
                    - "การประชุมในปี 2568" → "ค่ะ 📊 ในปี <b>2568</b> มีการประชุมทั้งหมด <b>15 รายการ</b>นะคะ หากต้องการดูรายละเอียดแต่ละครั้ง ยินดีช่วยค่ะ 💼"
                    - "ใครบ้างที่ตอบรับของว่างในการประชุม 001/68009" → "ค่ะ 🍪 สำหรับการประชุม <b>001/68009</b> มีผู้ตอบรับของว่าง <b>3 ท่าน</b> ได้แก่<br><ul><li>คุณสมชาย ใจดี</li><li>คุณสมหญิง รักงาน</li><li>คุณประยุทธ์ ขยัน</li></ul>ค่ะ 😊"

                    === ข้อมูลทรัพย์สิน (Assets) ===
                    - "มีทรัพย์สินรอตรวจสอบไหม" → "ค่ะ 🔍 มี<b>ทรัพย์สินที่รอการตรวจสอบอยู่ 3 รายการ</b>ค่ะ หากท่านต้องการข้อมูลเพิ่มเติม ยินดีให้บริการนะคะ 😊"
                    - "ทรัพย์สินทั้งหมดมีกี่รายการ" → "ขณะนี้มี<b>ทรัพย์สินทั้งหมด 25 รายการ</b>ในระบบค่ะ 🏢 แบ่งเป็น<br>- รอตรวจสอบ (PENDING) 3 รายการ<br>- ยืนยันแล้ว (CONFIRMED) 15 รายการ<br>- เช็คอินแล้ว (CHECKED_IN) 7 รายการ 📋"
                    - "มีทรัพย์สินประเภทอะไรบ้าง" → "ทรัพย์สินในระบบมี <b>6 ประเภท</b>ค่ะ ได้แก่ 🏦<br><ul><li>เอกสาร 📄</li><li>อุปกรณ์ 💻</li><li>อาคาร 🏢</li><li>ยานพาหนะ 🚗</li><li>ที่ดิน 🌳</li><li>อื่นๆ ✨</li></ul>"
                    - "ทรัพย์สินประเภทที่ดินมีกี่รายการ" → "ทรัพย์สิน<b>ประเภทที่ดินมี 8 รายการ</b>ค่ะ 🌳 หากต้องการทราบรายละเอียดเพิ่มเติม ยินดีช่วยเหลือนะคะ 😊"

                    === ข้อมูลคณะกรรมการ (Committee Members) ===
                    - "คณะกรรมการมีกี่คน" → "คณะกรรมการมี<b>ทั้งหมด 12 ท่าน</b>ค่ะ 👥 หากต้องการทราบรายชื่อ ยินดีช่วยเหลือค่ะ 😊"
                    - "แสดงรายชื่อคณะกรรมการ" → "ยินดีค่ะ 📋 รายชื่อคณะกรรมการ<br><ul><li>นายสมชาย ใจดี - กองทุน A</li><li>นางสาวสมหญิง รักงาน - กองทุน B</li><li>นายประยุทธ์ ขยัน - กองทุน C</li></ul>[และอีก 9 ท่าน] ค่ะ ✨"
                    - "ติดต่อคณะกรรมการได้อย่างไร" → "ค่ะ 📞 สามารถติดต่อคณะกรรมการได้ทาง<br>- <b>โทรศัพท์</b> กรุณาระบุชื่อสมาชิกที่ต้องการติดต่อ จะให้เบอร์ให้นะคะ<br>- <b>อีเมล</b> สามารถส่งอีเมลได้ตามข้อมูลของแต่ละท่าน 😊"

                    === สรุปข้อมูลและสถิติ (Summary & Statistics) ===
                    - "วันนี้มีอะไรบ้าง" → "สวัสดีค่ะ 😊 ขอสรุปข้อมูลวันนี้ให้นะคะ ✨<br>📅 <b>การประชุม</b> 1 รายการ<br>🏢 <b>ทรัพย์สินรอตรวจสอบ</b> 3 รายการ<br>📋 <b>การแจ้งเตือนที่ยังไม่อ่าน</b> 2 รายการ<br>หากต้องการข้อมูลเพิ่มเติม บอกได้เลยค่ะ 💙"
                    - "สรุปข้อมูลทั้งหมดให้หน่อย" → "ยินดีค่ะ 📊 ขอสรุปข้อมูลในระบบให้นะคะ<br>💼 <b>การประชุม</b> 25 รายการ<br>🏢 <b>ทรัพย์สิน</b> 25 รายการ (PENDING 3, CONFIRMED 15, CHECKED_IN 7)<br>👥 <b>คณะกรรมการ</b> 12 ท่าน<br>🔔 <b>การแจ้งเตือน</b> 5 รายการ (ยังไม่อ่าน 2)<br>หากต้องการข้อมูลเพิ่มเติม ยินดีช่วยเหลือนะคะ ✨"

                    จำไว้เสมอ: แสดงความเป็นมิตร อบอุ่น พร้อมช่วยเหลือ และใช้ emoji อย่างเหมาะสมเพื่อให้บรรยากาศการสนทนาเป็นกันเอง 💙
                    """;

            // --- ส่วนที่ 2: ดึงข้อมูลจริงจาก Database แบบ Intent-Based (ใหม่!) ---
            String userName = payload.getOrDefault("userName", "ผู้ใช้งาน");

            // ใช้ระบบ Intent-Based Query ใหม่ (ประหยัดและเร็วกว่า)
            String databaseContext = chatContextService.buildDatabaseContextByIntent(userMessage, userName);

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

    /**
     * Endpoint สำหรับรับ Feedback/Rating จากผู้ใช้
     * POST /api/chat/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<ChatFeedbackResponse> submitFeedback(@RequestBody ChatFeedbackRequest request) {
        try {
            ChatFeedbackResponse response = chatRatingService.saveFeedback(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChatFeedbackResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint สำหรับดึงการให้คะแนนทั้งหมด
     * GET /api/chat/ratings
     */
    @GetMapping("/ratings")
    public ResponseEntity<List<ChatRating>> getAllRatings() {
        try {
            List<ChatRating> ratings = chatRatingService.getAllRatings();
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint สำหรับดึงสถิติการให้คะแนน
     * GET /api/chat/ratings/statistics
     */
    @GetMapping("/ratings/statistics")
    public ResponseEntity<Map<String, Object>> getRatingStatistics() {
        try {
            long likes = chatRatingService.countLikes();
            long dislikes = chatRatingService.countDislikes();
            long total = likes + dislikes;

            Map<String, Object> stats = new HashMap<>();
            stats.put("likes", likes);
            stats.put("dislikes", dislikes);
            stats.put("total", total);

            if (total > 0) {
                stats.put("likePercentage", (likes * 100.0) / total);
                stats.put("dislikePercentage", (dislikes * 100.0) / total);
            } else {
                stats.put("likePercentage", 0.0);
                stats.put("dislikePercentage", 0.0);
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint สำหรับดึงการให้คะแนนตาม logId
     * GET /api/chat/ratings/{logId}
     */
    @GetMapping("/ratings/{logId}")
    public ResponseEntity<ChatRating> getRatingByLogId(@PathVariable String logId) {
        try {
            Optional<ChatRating> rating = chatRatingService.getRatingByLogId(logId);
            return rating.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}