package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service สำหรับวิเคราะห์ Intent จากคำถามของผู้ใช้
 * เพื่อให้ระบบ Query เฉพาะข้อมูลที่จำเป็น
 */
@Service
public class IntentDetectionService {

    /**
     * Intent Types ที่รองรับ
     */
    public enum IntentType {
        GREETING,           // คำทักทาย
        DAILY_BRIEF,        // ถามสรุปวันนี้แบบสั้น (วันนี้มีอะไร, วันนี้มีอะไรบ้าง)
        MEETING_QUERY,      // ถามเกี่ยวกับการประชุม
        MEETING_DETAIL,     // ถามรายละเอียดการประชุมเฉพาะ
        ASSET_QUERY,        // ถามเกี่ยวกับทรัพย์สิน
        ASSET_COUNT,        // ถามจำนวนทรัพย์สิน
        COMMITTEE_QUERY,    // ถามเกี่ยวกับคณะกรรมการ
        COUNT_QUERY,        // ถามจำนวนแบบทั่วไป
        SUMMARY_QUERY,      // ถามสรุปข้อมูลทั่วไป
        GENERAL             // คำถามทั่วไป
    }

    /**
     * Keyword Patterns สำหรับแต่ละ Intent
     */
    private static final Map<IntentType, List<String>> INTENT_KEYWORDS = new HashMap<>();

    static {
        // คำทักทาย
        INTENT_KEYWORDS.put(IntentType.GREETING, Arrays.asList(
                "สวัสดี", "หวัดดี", "ดีครับ", "ดีค่ะ", "hello", "hi", "hey"
        ));

        // สรุปวันนี้แบบสั้น (ต้องอยู่ก่อน MEETING_QUERY)
        INTENT_KEYWORDS.put(IntentType.DAILY_BRIEF, Arrays.asList(
                "วันนี้มีอะไร", "วันนี้มีอะไรบ้าง", "วันนี้มีงานอะไร", "มีอะไรบ้าง"
        ));

        // การประชุม
        INTENT_KEYWORDS.put(IntentType.MEETING_QUERY, Arrays.asList(
                "การประชุม", "ประชุม", "meeting", "วันนี้มีประชุม", "ประชุมวันนี้",
                "ประชุมเมื่อไหร่", "ประชุมกี่โมง", "ประชุมที่ไหน"
        ));

        // รายละเอียดการประชุมเฉพาะ (ต้องมีรหัสการประชุมเท่านั้น)
        INTENT_KEYWORDS.put(IntentType.MEETING_DETAIL, Arrays.asList(
                "รายละเอียดการประชุม", "สรุปการประชุม",
                "รายงานการประชุม", "001/", "002/", "003/"
        ));

        // ทรัพย์สิน
        INTENT_KEYWORDS.put(IntentType.ASSET_QUERY, Arrays.asList(
                "ทรัพย์สิน", "asset", "รอตรวจสอบ", "รอการตรวจสอบ",
                "pending", "อาคาร", "ที่ดิน", "ยานพาหนะ"
        ));

        // จำนวนทรัพย์สิน
        INTENT_KEYWORDS.put(IntentType.ASSET_COUNT, Arrays.asList(
                "ทรัพย์สินกี่", "จำนวนทรัพย์สิน", "นับทรัพย์สิน"
        ));

        // คณะกรรมการ
        INTENT_KEYWORDS.put(IntentType.COMMITTEE_QUERY, Arrays.asList(
                "คณะกรรมการ", "กรรมการ", "committee", "สมาชิก",
                "ผู้เข้าร่วม", "อนุกรรมการ"
        ));

        // จำนวน/นับ
        INTENT_KEYWORDS.put(IntentType.COUNT_QUERY, Arrays.asList(
                "กี่", "จำนวน", "นับ", "count", "ทั้งหมด", "ทั้งสิ้น",
                "มีกี่", "มีจำนวน"
        ));

        // สรุป
        INTENT_KEYWORDS.put(IntentType.SUMMARY_QUERY, Arrays.asList(
                "สรุป", "summary", "ภาพรวม", "overview", "ทั้งหมด",
                "สรุปข้อมูล", "ข้อมูลทั้งหมด"
        ));
    }

    /**
     * ตรวจจับ Intent จากข้อความ
     */
    public IntentType detectIntent(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return IntentType.GENERAL;
        }

        String normalizedMessage = userMessage.toLowerCase().trim();

        // เช็คตามลำดับความสำคัญ (เฉพาะเจาะจงไปทั่วไป)

        // 1. คำทักทายเดี่ยวๆ (ไม่มีคำอื่นต่อท้าย)
        if (isGreetingOnly(normalizedMessage)) {
            return IntentType.GREETING;
        }

        // 2. สรุปวันนี้แบบสั้น (ต้องเช็คก่อน MEETING_QUERY)
        if (containsKeywords(normalizedMessage, IntentType.DAILY_BRIEF)) {
            return IntentType.DAILY_BRIEF;
        }

        // 3. รายละเอียดการประชุมเฉพาะ (มีรหัสการประชุม)
        if (containsKeywords(normalizedMessage, IntentType.MEETING_DETAIL)) {
            return IntentType.MEETING_DETAIL;
        }

        // 3. จำนวนทรัพย์สิน (เฉพาะกว่า ASSET_QUERY)
        if (containsKeywords(normalizedMessage, IntentType.ASSET_COUNT)) {
            return IntentType.ASSET_COUNT;
        }

        // 4. การประชุม
        if (containsKeywords(normalizedMessage, IntentType.MEETING_QUERY)) {
            return IntentType.MEETING_QUERY;
        }

        // 5. ทรัพย์สิน
        if (containsKeywords(normalizedMessage, IntentType.ASSET_QUERY)) {
            return IntentType.ASSET_QUERY;
        }

        // 6. คณะกรรมการ
        if (containsKeywords(normalizedMessage, IntentType.COMMITTEE_QUERY)) {
            return IntentType.COMMITTEE_QUERY;
        }

        // 8. จำนวน/นับ
        if (containsKeywords(normalizedMessage, IntentType.COUNT_QUERY)) {
            return IntentType.COUNT_QUERY;
        }

        // 9. สรุป
        if (containsKeywords(normalizedMessage, IntentType.SUMMARY_QUERY)) {
            return IntentType.SUMMARY_QUERY;
        }

        // 10. ถ้าไม่ตรงอะไรเลย
        return IntentType.GENERAL;
    }

    /**
     * เช็คว่าเป็นคำทักทายอย่างเดียวไหม (ไม่มีคำอื่นต่อท้าย)
     */
    private boolean isGreetingOnly(String message) {
        List<String> greetings = INTENT_KEYWORDS.get(IntentType.GREETING);
        for (String greeting : greetings) {
            // เช็คว่าเป็นคำทักทายเดี่ยวๆ หรือมีแค่ ครับ/ค่ะ ต่อท้าย
            if (message.equals(greeting) ||
                    message.equals(greeting + "ครับ") ||
                    message.equals(greeting + "ค่ะ") ||
                    message.matches("^" + Pattern.quote(greeting) + "[\\s]*[ครับค่ะ]*$")) {
                return true;
            }
        }
        return false;
    }

    /**
     * เช็คว่าข้อความมี Keyword ของ Intent นั้นไหม
     */
    private boolean containsKeywords(String message, IntentType intentType) {
        List<String> keywords = INTENT_KEYWORDS.get(intentType);
        if (keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * ดึงข้อมูลเพิ่มเติมจากคำถาม (เช่น รหัสการประชุม, วันที่)
     */
    public Map<String, String> extractParameters(String userMessage) {
        Map<String, String> params = new HashMap<>();

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return params;
        }

        String normalizedMessage = userMessage.toLowerCase().trim();

        // ดึงรหัสการประชุม (เช่น 001/68009)
        Pattern meetingNoPattern = Pattern.compile("(\\d{3}/\\d+)");
        java.util.regex.Matcher matcher = meetingNoPattern.matcher(userMessage);
        if (matcher.find()) {
            params.put("meetingNo", matcher.group(1));
        }

        // เช็คว่าถาม "ทั้งหมด" ไหม
        if (normalizedMessage.contains("ทั้งหมด") || normalizedMessage.contains("ทุก")) {
            params.put("showAll", "true");
        }

        // ดึงปี - รองรับทั้ง พ.ศ. (2568) และ ค.ศ. (2025)
        Pattern buddhistYearPattern = Pattern.compile("(25\\d{2})"); // พ.ศ. 2500-2599
        Pattern christianYearPattern = Pattern.compile("\\b(20\\d{2})\\b"); // ค.ศ. 2000-2099

        java.util.regex.Matcher buddhistMatcher = buddhistYearPattern.matcher(userMessage);
        java.util.regex.Matcher christianMatcher = christianYearPattern.matcher(userMessage);

        if (buddhistMatcher.find()) {
            // ถ้าเจอ พ.ศ. (2568)
            params.put("year", buddhistMatcher.group(1));
            params.put("yearType", "buddhist");
        } else if (christianMatcher.find()) {
            // ถ้าเจอ ค.ศ. (2025) - แปลงเป็น พ.ศ.
            int christianYear = Integer.parseInt(christianMatcher.group(1));
            int buddhistYear = christianYear + 543;
            params.put("year", String.valueOf(buddhistYear));
            params.put("yearType", "christian");
        } else if (normalizedMessage.contains("ปีนี้")) {
            params.put("year", "thisYear");
        }

        // ดึงวันที่ (เช่น วันนี้, เมื่อวาน, พรุ่งนี้)
        if (normalizedMessage.contains("วันนี้")) {
            params.put("dateFilter", "today");
        } else if (normalizedMessage.contains("เมื่อวาน")) {
            params.put("dateFilter", "yesterday");
        } else if (normalizedMessage.contains("พรุ่งนี้")) {
            params.put("dateFilter", "tomorrow");
        } else if (normalizedMessage.contains("สัปดาห์นี้")) {
            params.put("dateFilter", "thisWeek");
        } else if (normalizedMessage.contains("อาทิตย์นี้")) {
            params.put("dateFilter", "thisWeek");
        } else if (normalizedMessage.contains("เดือนนี้")) {
            params.put("dateFilter", "thisMonth");
        }

        // ดึงวันเฉพาะเจาะจงในสัปดาห์ (เช่น วันจันทร์นี้, วันอังคารหน้า)
        String dayOfWeek = extractDayOfWeek(normalizedMessage);
        if (dayOfWeek != null) {
            params.put("dayOfWeek", dayOfWeek);
            // เช็คว่าเป็น "นี้" หรือ "หน้า"
            if (normalizedMessage.contains("หน้า") || normalizedMessage.contains("ถัดไป")) {
                params.put("weekOffset", "next");
            } else {
                params.put("weekOffset", "this");
            }
        }

        // ดึงประเภททรัพย์สิน
        if (normalizedMessage.contains("อาคาร")) {
            params.put("assetType", "อาคาร");
        } else if (normalizedMessage.contains("ที่ดิน")) {
            params.put("assetType", "ที่ดิน");
        } else if (normalizedMessage.contains("ยานพาหนะ")) {
            params.put("assetType", "ยานพาหนะ");
        }

        return params;
    }

    /**
     * ดึงวันในสัปดาห์จากข้อความ (เช่น วันจันทร์, วันอังคาร)
     * @return "MONDAY", "TUESDAY", ... หรือ null ถ้าไม่พบ
     */
    private String extractDayOfWeek(String normalizedMessage) {
        if (normalizedMessage.contains("จันทร์") || normalizedMessage.contains("จันทร")) {
            return "MONDAY";
        } else if (normalizedMessage.contains("อังคาร")) {
            return "TUESDAY";
        } else if (normalizedMessage.contains("พุธ")) {
            return "WEDNESDAY";
        } else if (normalizedMessage.contains("พฤหัสบดี") || normalizedMessage.contains("พฤหัส")) {
            return "THURSDAY";
        } else if (normalizedMessage.contains("ศุกร์")) {
            return "FRIDAY";
        } else if (normalizedMessage.contains("เสาร์")) {
            return "SATURDAY";
        } else if (normalizedMessage.contains("อาทิตย์")) {
            return "SUNDAY";
        }
        return null;
    }

    /**
     * ดึงข้อมูลเพิ่มเติมสำหรับ Debug
     */
    public String getIntentDescription(IntentType intent) {
        return switch (intent) {
            case GREETING -> "คำทักทาย - ไม่ต้อง Query ข้อมูล";
            case DAILY_BRIEF -> "สรุปวันนี้แบบสั้น - Query Count อย่างเดียว (ประหยัด Token สูงสุด)";
            case MEETING_QUERY -> "ถามเกี่ยวกับการประชุม - Query เฉพาะ Meeting";
            case MEETING_DETAIL -> "ถามรายละเอียดการประชุมเฉพาะ - Query Meeting ตาม ID/No";
            case ASSET_QUERY -> "ถามเกี่ยวกับทรัพย์สิน - Query เฉพาะ Asset";
            case ASSET_COUNT -> "ถามจำนวนทรัพย์สิน - Query Count อย่างเดียว";
            case COMMITTEE_QUERY -> "ถามเกี่ยวกับคณะกรรมการ - Query เฉพาะ Committee";
            case COUNT_QUERY -> "ถามจำนวนทั่วไป - Query Count ทั้งหมด";
            case SUMMARY_QUERY -> "ถามสรุปข้อมูล - Query Summary (Count + ข้อมูลสำคัญ)";
            case GENERAL -> "คำถามทั่วไป - Query ข้อมูลพื้นฐาน";
        };
    }
}
