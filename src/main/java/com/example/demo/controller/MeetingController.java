package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.CommitteeMember;
import com.example.demo.entity.Meeting;
import com.example.demo.entity.Notification;
import com.example.demo.service.EmailService;
import com.example.demo.service.MeetingService;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.service.LineBotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LineBotService lineBotService;

    // --- Helper Function: สร้าง URL ตามประเภทการประชุม ---
    private String getMeetingUrl(String typeCode, Long id) {
        String path = "subCommittee";
        if ("003".equals(typeCode)) {
            path = "MillionAssets";
        } else if ("002".equals(typeCode)) {
            path = "AssetsCheck";
        }
        return String.format("http://localhost:3000/Meetings/%s/%d", path, id);
    }

    @GetMapping("/meetings")
    public ResponseEntity<?> getAllMeetings() {
        try {
            return ResponseEntity.ok(meetingService.getAllMeetings());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody MeetingRequest request) {
        try {
            Meeting newMeeting = meetingService.createMeeting(request);
            return ResponseEntity.ok(newMeeting);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/meetings/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long id, @RequestBody MeetingRequest request) {
        Meeting updatedMeeting = meetingService.updateMeeting(id, request);

        // Debug log
        System.out.println("=== UPDATE MEETING DEBUG ===");
        System.out.println("Status: " + updatedMeeting.getStatus());
        System.out.println("Attendees: "
                + (updatedMeeting.getAttendees() != null ? updatedMeeting.getAttendees().size() : "null"));
        System.out.println("CurrentStep: " + request.getCurrentStep());
        System.out.println("===========================");

        if ("ACTIVE".equalsIgnoreCase(updatedMeeting.getStatus())
                && updatedMeeting.getAttendees() != null && !updatedMeeting.getAttendees().isEmpty()
                && request.getCurrentStep() != null && request.getCurrentStep() == 5) {

            System.out.println("✅ Condition matched! Sending meeting invitations...");

            String meetingUrl = getMeetingUrl(updatedMeeting.getMeetingTypeCode(), updatedMeeting.getId());
            String emailTitle = "แจ้งนัดหมายการประชุม";

            // ดึงไฟล์แนบทั้งหมดจากวาระการประชุม
            java.util.List<java.util.Map<String, String>> attachedFiles = extractAllAttachedFiles(updatedMeeting);
            String attachedFilesHtml = generateAttachedFilesHtml(attachedFiles);

            // ========== 1. ส่งอีเมลแจ้งเตือนให้ผู้ดูแลระบบ (แบบ Notification ธรรมดา) ==========

            String adminEmailBody = String.format(
                    "<html>" +
                            "<body style=\"font-family: 'Sarabun', Arial, sans-serif; line-height: 1.6; color: #333;\">"
                            +
                            "<div style=\"max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;\">"
                            +
                            "<div style=\"background-color: #141371; padding: 20px; text-align: center;\">" +
                            "<h2 style=\"color: #ffffff; margin: 0;\">แจ้งนัดหมายการประชุม</h2>" +
                            "</div>" +
                            "<div style=\"padding: 30px;\">" +
                            "<h3 style=\"color: #141371; margin-top: 0;\">เรียน ผู้ดูแลระบบ</h3>" +
                            "<p>ขอเรียนเชิญท่านเข้าร่วมการประชุม โดยมีรายละเอียดดังนี้</p>" +

                            "<div style=\"background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #141371;\">"
                            +
                            "<p style=\"margin: 5px 0;\"><b>เลขคำสั่งตรวจสอบ:</b> %s</p>" +
                            "<div style=\"margin: 5px 0;\"><b>เรื่อง:</b> %s</div>" +
                            "<p style=\"margin: 5px 0;\"><b>วันที่:</b> %s</p>" +
                            "<p style=\"margin: 5px 0;\"><b>เวลา:</b> %s</p>" +
                            "<p style=\"margin: 5px 0;\"><b>สถานที่:</b> %s</p>" +
                            "%s" + // เพิ่มส่วนแสดงไฟล์แนบ
                            "</div>" +

                            "<p>กรุณาเข้าร่วมการประชุมตามวัน เวลา และสถานที่ดังกล่าว</p>" +
                            "<div style=\"text-align: center; margin: 30px 0;\">" +
                            "<a href=\"%s\" style=\"background-color: #141371; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">ดูรายละเอียดเพิ่มเติม</a>"
                            +
                            "</div>" +

                            "<hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\" />" +
                            "<p style=\"font-size: 0.9em; color: #666;\">ขอแสดงความนับถือ,<br>ทีมงาน ASLES Support</p>"
                            +
                            "</div>" +
                            "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 0.8em; color: #888;\">"
                            +
                            "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    updatedMeeting.getMeetingNo(),
                    updatedMeeting.getDescription() != null ? updatedMeeting.getDescription() : "-",
                    updatedMeeting.getMeetingDate(),
                    updatedMeeting.getMeetingTime(),
                    updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "-",
                    attachedFilesHtml, // เพิ่มพารามิเตอร์ไฟล์แนบ
                    meetingUrl);

            // ส่งอีเมล notification ธรรมดาให้ผู้ดูแลระบบ (ไม่ใช่ Calendar Invite)
            emailService.sendMeetingNotification("nuntiya.suw@ilustro.co", emailTitle, adminEmailBody);

            String lineMsg = "📅 แจ้งนัดหมายการประชุม\n" +
                    "เรื่อง: " + stripHtml(updatedMeeting.getDescription()) + "\n" +
                    "วันที่: " + updatedMeeting.getMeetingDate() + "\n" +
                    "เวลา: " + updatedMeeting.getMeetingTime() + "\n" +
                    "สถานที่: " + (updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "-") +
                    "ดูรายละเอียด: " + meetingUrl;

            lineBotService.sendPushMessage(lineMsg);

            // ========== 2. ส่ง Calendar Invite ให้ผู้เข้าร่วมประชุม (มีปุ่ม Accept/Decline) ==========

            // สร้างไฟล์ .ics สำหรับ Calendar Invite
            String icsContent = generateIcsContent(updatedMeeting);

            for (CommitteeMember attendee : updatedMeeting.getAttendees()) {
                if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
                    String attendeeName = attendee.getPrename() != null
                            ? attendee.getPrename() + attendee.getFirstname() + " " + attendee.getLastname()
                            : attendee.getFirstname() + " " + attendee.getLastname();

                    // สร้าง URL สำหรับปุ่มยอมรับ/ปฏิเสธของว่าง
                    String acceptUrl = String.format(
                            "http://localhost:8080/api/meetings/%d/snacks-response?memberId=%d&accept=true",
                            updatedMeeting.getId(),
                            attendee.getId());

                    String declineUrl = String.format(
                            "http://localhost:8080/api/meetings/%d/snacks-response?memberId=%d&accept=false",
                            updatedMeeting.getId(),
                            attendee.getId());

                    String snacksSection = String.format(
                            "<div style=\"text-align: center; margin: 20px 0; padding: 20px; background-color: #fff7ed; border-radius: 6px; border: 1px solid #fed7aa;\">"
                                    +
                                    "<p style=\"margin: 0 0 10px 0; font-weight: 600; color: #ea580c; font-size: 15px;\">🍪 ของว่างระหว่างประชุม</p>"
                                    +
                                    "<p style=\"margin: 0 0 15px 0; font-size: 14px; color: #78716c;\">กรุณาตอบรับหรือปฏิเสธของว่าง</p>"
                                    +
                                    "<a href=\"%s\" style=\"background-color: #059669; color: #ffffff; padding: 10px 24px; text-decoration: none; border-radius: 5px; margin-right: 10px; display: inline-block; font-weight: 600;\">✅ ยอมรับ</a>"
                                    +
                                    "<a href=\"%s\" style=\"background-color: #dc2626; color: #ffffff; padding: 10px 24px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: 600;\">❌ ปฏิเสธ</a>"
                                    +
                                    "</div>",
                            acceptUrl,
                            declineUrl);

                    String attendeeEmailBody = String.format(
                            "<html>" +
                                    "<body style=\"font-family: 'Sarabun', Arial, sans-serif; line-height: 1.6; color: #333;\">"
                                    +
                                    "<div style=\"max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;\">"
                                    +
                                    "<div style=\"background-color: #141371; padding: 20px; text-align: center;\">" +
                                    "<h2 style=\"color: #ffffff; margin: 0;\">แจ้งนัดหมายการประชุม</h2>" +
                                    "</div>" +
                                    "<div style=\"padding: 30px;\">" +
                                    "<h3 style=\"color: #141371; margin-top: 0;\">เรียน %s</h3>" +
                                    "<p>ขอเรียนเชิญท่านเข้าร่วมการประชุม โดยมีรายละเอียดดังนี้</p>" +

                                    "<div style=\"background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #141371;\">"
                                    +
                                    "<p style=\"margin: 5px 0;\"><b>เลขคำสั่งตรวจสอบ:</b> %s</p>" +
                                    "<div style=\"margin: 5px 0;\"><b>เรื่อง:</b> %s</div>" +
                                    "<p style=\"margin: 5px 0;\"><b>วันที่:</b> %s</p>" +
                                    "<p style=\"margin: 5px 0;\"><b>เวลา:</b> %s</p>" +
                                    "<p style=\"margin: 5px 0;\"><b>สถานที่:</b> %s</p>" +
                                    "%s" + // เพิ่มส่วนแสดงไฟล์แนบ
                                    "</div>" +

                                    "<p>กรุณาเข้าร่วมการประชุมตามวัน เวลา และสถานที่ดังกล่าว</p>" +

                                    "<div style=\"background-color: #eff6ff; padding: 15px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #3b82f6;\">"
                                    +
                                    "<p style=\"margin: 0; font-size: 14px; color: #1e40af;\">💡 <b>หมายเหตุ:</b> หลังจากกดปุ่ม <b>Accept Meeting</b> ในอีเมลนี้แล้ว ท่านสามารถตรวจสอบรายละเอียดการประชุม วันที่ เวลา และสถานที่ ได้จาก <b>Outlook Calendar</b> ของท่านได้ทันที</p>"
                                    +
                                    "</div>" +

                                    "%s" + // เพิ่มส่วนของว่าง (snacksSection)

                                    "<div style=\"text-align: center; margin: 30px 0;\">" +
                                    "<a href=\"%s\" style=\"background-color: #141371; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">ดูรายละเอียดเพิ่มเติม</a>"
                                    +
                                    "</div>" +

                                    "<hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\" />" +
                                    "<p style=\"font-size: 0.9em; color: #666;\">ขอแสดงความนับถือ,<br>ทีมงาน ASLES Support</p>"
                                    +
                                    "</div>" +
                                    "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 0.8em; color: #888;\">"
                                    +
                                    "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>"
                                    +
                                    "</div>" +
                                    "</div>" +
                                    "</body>" +
                                    "</html>",
                            attendeeName,
                            updatedMeeting.getMeetingNo(),
                            updatedMeeting.getDescription() != null ? updatedMeeting.getDescription() : "-",
                            updatedMeeting.getMeetingDate(),
                            updatedMeeting.getMeetingTime(),
                            updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "-",
                            attachedFilesHtml, // เพิ่มพารามิเตอร์ไฟล์แนบ
                            snacksSection, // เพิ่มส่วนของว่าง
                            meetingUrl);

                    // ส่ง Calendar Invite แทน sendMeetingNotification
                    emailService.sendCalendarInvite(attendee.getEmail(), emailTitle, attendeeEmailBody, icsContent);
                }
            }
        }

        if (request.getCurrentStep() != null && request.getCurrentStep() == 5
                && "ACTIVE".equalsIgnoreCase(updatedMeeting.getStatus())) {

            String title = "บันทึกวาระการประชุมครบ 5 วาระแล้ว";
            String notifMessage = String.format("การประชุม %s บันทึกวาระครบ 5 วาระแล้ว กดเพื่อตรวจสอบรายละเอียด",
                    updatedMeeting.getMeetingNo());

            createNotification("NEW_MEETING", title, notifMessage, updatedMeeting);

            String meetingUrl = getMeetingUrl(updatedMeeting.getMeetingTypeCode(), updatedMeeting.getId());
            String adminEmail = "ictbookingroom@outlook.com";

            // ดึงไฟล์แนบสำหรับอีเมลนี้ด้วย
            java.util.List<java.util.Map<String, String>> attachedFilesForAdmin = extractAllAttachedFiles(
                    updatedMeeting);
            String attachedFilesHtmlForAdmin = generateAttachedFilesHtml(attachedFilesForAdmin);

            String emailBody = String.format(
                    "<html>" +
                            "<body style=\"font-family: 'Sarabun', Arial, sans-serif; line-height: 1.6; color: #333;\">"
                            +
                            "<div style=\"max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;\">"
                            +
                            "<div style=\"background-color: #141371; padding: 20px; text-align: center;\">" +
                            "<h2 style=\"color: #ffffff; margin: 0;\">แจ้งนัดหมายการประชุม</h2>" +
                            "</div>" +
                            "<div style=\"padding: 30px;\">" +
                            "<h3 style=\"color: #141371; margin-top: 0;\">เรียน ผู้ดูแลระบบ</h3>" +
                            "<p>ขอเรียนเชิญท่านเข้าร่วมการประชุม โดยมีรายละเอียดดังนี้</p>" +

                            "<div style=\"background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #141371;\">"
                            +
                            "<p style=\"margin: 5px 0;\"><b>เลขคำสั่งตรวจสอบ:</b> %s</p>" +
                            "<div style=\"margin: 5px 0;\"><b>เรื่อง:</b> %s</div>" +
                            "<p style=\"margin: 5px 0;\"><b>วันที่:</b> %s</p>" +
                            "<p style=\"margin: 5px 0;\"><b>เวลา:</b> %s</p>" +
                            "<p style=\"margin: 5px 0;\"><b>สถานที่:</b> %s</p>" +
                            "%s" + // เพิ่มส่วนแสดงไฟล์แนบ
                            "</div>" +

                            "<p>กรุณาเข้าร่วมการประชุมตามวัน เวลา และสถานที่ดังกล่าว</p>" +
                            "<div style=\"text-align: center; margin: 30px 0;\">" +
                            "<a href=\"%s\" style=\"background-color: #141371; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">ดูรายละเอียดเพิ่มเติม</a>"
                            +
                            "</div>" +

                            "<hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\" />" +
                            "<p style=\"font-size: 0.9em; color: #666;\">ขอแสดงความนับถือ,<br>ทีมงาน ASLES Support</p>"
                            +
                            "</div>" +
                            "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 0.8em; color: #888;\">"
                            +
                            "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    updatedMeeting.getMeetingNo(),
                    updatedMeeting.getDescription() != null ? updatedMeeting.getDescription() : "-",
                    updatedMeeting.getMeetingDate(),
                    updatedMeeting.getMeetingTime(),
                    updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "-",
                    attachedFilesHtmlForAdmin, // เพิ่มพารามิเตอร์ไฟล์แนบ
                    meetingUrl);
            emailService.sendMeetingNotification(adminEmail, title, emailBody);
        }
        return ResponseEntity.ok(updatedMeeting);
    }

    @GetMapping("/meetings/{id}")
    public ResponseEntity<?> getMeetingById(@PathVariable Long id) {
        try {
            Meeting meeting = meetingService.getMeetingById(id);
            return ResponseEntity.ok(meeting);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("ไม่พบข้อมูลการประชุม: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // --- 2. สรุปผลการประชุม (HTML Email) ---
    @PutMapping("/meetings/{id}/resolutions")
    public ResponseEntity<?> updateMeetingResolutions(
            @PathVariable Long id,
            @RequestBody MeetingRequest request) {
        try {
            Meeting updated = meetingService.updateMeetingResolutions(id, request);

            // ส่งอีเมลและแจ้งเตือนเฉพาะเมื่ออยู่ step 2 และสถานะเป็น PUBLISH
            if ("PUBLISH".equalsIgnoreCase(updated.getStatus())
                    && request.getCurrentStep() != null && request.getCurrentStep() == 2) {

                String title = "สรุปผลการประชุมเรียบร้อยแล้ว";
                String message = String.format("เรื่อง %s (รหัส %s) ได้รับการลงมติและสรุปผลแล้ว",
                        updated.getDescription() != null ? updated.getDescription() : "ไม่ระบุ",
                        updated.getMeetingNo());

                // 1. Notification
                createNotification("STATUS_CHANGE", title, message, updated);

                // 2. ส่งอีเมลให้ผู้เกี่ยวข้องและผู้ดูแลระบบ
                String meetingUrl = getMeetingUrl(updated.getMeetingTypeCode(), updated.getId());

                // ดึงไฟล์แนบจาก resolutionDetail สำหรับอีเมลสรุปผล
                java.util.List<java.util.Map<String, String>> attachedFilesResolution = extractResolutionAttachedFiles(
                        updated);
                String attachedFilesHtmlResolution = generateResolutionFilesHtml(attachedFilesResolution);

                // อีเมลสำหรับผู้เกี่ยวข้อง
                String emailBodyForStaff = String.format(
                        "<html>" +
                                "<body style=\"font-family: 'Sarabun', Arial, sans-serif; line-height: 1.6; color: #333;\">"
                                +
                                "<div style=\"max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;\">"
                                +
                                "<div style=\"background-color: #059669; padding: 20px; text-align: center;\">" +
                                "<h2 style=\"color: #ffffff; margin: 0;\">สรุปผลการประชุมเรียบร้อย</h2>" +
                                "</div>" +
                                "<div style=\"padding: 30px;\">" +
                                "<h3 style=\"color: #059669; margin-top: 0;\">เรียน ผู้เกี่ยวข้อง</h3>" +
                                "<p>การประชุมดังต่อไปนี้ ได้รับการลงมติและสรุปผลการประชุมเป็นที่เรียบร้อยแล้ว</p>" +

                                "<div style=\"background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #059669;\">"
                                +
                                "<p style=\"margin: 5px 0;\"><b>เลขคำสั่งตรวจสอบ:</b> %s</p>" +
                                "<div style=\"margin: 5px 0;\"><b>หัวข้อเรื่อง:</b> %s</div>" +
                                "<p style=\"margin: 5px 0;\"><b>วันที่ประชุม:</b> %s</p>" +
                                "<p style=\"margin: 5px 0;\"><b>เวลา:</b> %s</p>" +
                                "<p style=\"margin: 5px 0;\"><b>สถานที่:</b> %s</p>" +
                                "%s" + // เพิ่มส่วนแสดงไฟล์แนบ
                                "<p style=\"margin: 5px 0;\"><b>สถานะ:</b> <span style=\"color: #059669; font-weight: bold;\">สรุปผลการประชุมและลงมติเรียบร้อยแล้ว</span></p>"
                                +
                                "</div>" +

                                "<p>ท่านสามารถดูรายละเอียดผลการประชุมได้ที่</p>" +
                                "<div style=\"text-align: center; margin: 30px 0;\">" +
                                "<a href=\"%s\" style=\"background-color: #059669; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">ดูรายละเอียด</a>"
                                +
                                "</div>" +

                                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\" />" +
                                "<p style=\"font-size: 0.9em; color: #666;\">ขอแสดงความนับถือ,<br>ทีมงาน ASLES Support</p>"
                                +
                                "</div>" +
                                "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 0.8em; color: #888;\">"
                                +
                                "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>" +
                                "</div>" +
                                "</div>" +
                                "</body>" +
                                "</html>",
                        updated.getMeetingNo(),
                        updated.getDescription() != null ? updated.getDescription() : "-",
                        updated.getMeetingDate() != null ? updated.getMeetingDate().toString() : "-",
                        updated.getMeetingTime() != null ? updated.getMeetingTime() : "-",
                        updated.getLocation() != null ? updated.getLocation() : "-",
                        attachedFilesHtmlResolution, // เพิ่มพารามิเตอร์ไฟล์แนบ
                        meetingUrl);

                // อีเมลสำหรับผู้ดูแลระบบ
                String emailBodyForAdmin = String.format(
                        "<html>" +
                                "<body style=\"font-family: 'Sarabun', Arial, sans-serif; line-height: 1.6; color: #333;\">"
                                +
                                "<div style=\"max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;\">"
                                +
                                "<div style=\"background-color: #059669; padding: 20px; text-align: center;\">" +
                                "<h2 style=\"color: #ffffff; margin: 0;\">สรุปผลการประชุมเรียบร้อย</h2>" +
                                "</div>" +
                                "<div style=\"padding: 30px;\">" +
                                "<h3 style=\"color: #059669; margin-top: 0;\">เรียน ผู้ดูแลระบบ</h3>" +
                                "<p>การประชุมดังต่อไปนี้ ได้รับการลงมติและสรุปผลการประชุมเป็นที่เรียบร้อยแล้ว</p>" +

                                "<div style=\"background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #059669;\">"
                                +
                                "<p style=\"margin: 5px 0;\"><b>เลขคำสั่งตรวจสอบ:</b> %s</p>" +
                                "<div style=\"margin: 5px 0;\"><b>หัวข้อเรื่อง:</b> %s</div>" +
                                "<p style=\"margin: 5px 0;\"><b>วันที่ประชุม:</b> %s</p>" +
                                "<p style=\"margin: 5px 0;\"><b>เวลา:</b> %s</p>" +
                                "<p style=\"margin: 5px 0;\"><b>สถานที่:</b> %s</p>" +
                                "%s" + // เพิ่มส่วนแสดงไฟล์แนบ
                                "<p style=\"margin: 5px 0;\"><b>สถานะ:</b> <span style=\"color: #059669; font-weight: bold;\">สรุปผลการประชุมและลงมติเรียบร้อยแล้ว</span></p>"
                                +
                                "</div>" +

                                "<p>ท่านสามารถดูรายละเอียดผลการประชุมได้ที่</p>" +
                                "<div style=\"text-align: center; margin: 30px 0;\">" +
                                "<a href=\"%s\" style=\"background-color: #059669; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;\">ดูรายละเอียด</a>"
                                +
                                "</div>" +

                                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\" />" +
                                "<p style=\"font-size: 0.9em; color: #666;\">ขอแสดงความนับถือ,<br>ทีมงาน ASLES Support</p>"
                                +
                                "</div>" +
                                "<div style=\"background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 0.8em; color: #888;\">"
                                +
                                "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>" +
                                "</div>" +
                                "</div>" +
                                "</body>" +
                                "</html>",
                        updated.getMeetingNo(),
                        updated.getDescription() != null ? updated.getDescription() : "-",
                        updated.getMeetingDate() != null ? updated.getMeetingDate().toString() : "-",
                        updated.getMeetingTime() != null ? updated.getMeetingTime() : "-",
                        updated.getLocation() != null ? updated.getLocation() : "-",
                        attachedFilesHtmlResolution, // เพิ่มพารามิเตอร์ไฟล์แนบ
                        meetingUrl);

                // ส่งอีเมลให้ผู้เกี่ยวข้อง
                emailService.sendMeetingNotification("nuntiya.suw@ilustro.co", title, emailBodyForStaff);

                // ส่งอีเมลให้ผู้ดูแลระบบ
                emailService.sendMeetingNotification("ictbookingroom@outlook.com", title, emailBodyForAdmin);

                // --- ADD LINE NOTIFY: แจ้งเตือนสรุปผล ---
                String lineSummaryMsg = "📢 สรุปผลการประชุมเรียบร้อย\n" +
                        "เรื่อง: " + stripHtml(updated.getDescription()) + "\n" +
                        "สถานะ: ลงมติเรียบร้อย\n" +
                        "ดูรายละเอียด: " + meetingUrl;

                lineBotService.sendPushMessage(lineSummaryMsg);
            }

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating resolutions: " + e.getMessage());
        }
    }

    // ================= HELPER FUNCTIONS =================

    // --- Helper 1: สร้างไฟล์ .ics สำหรับ Calendar Invite ---
    private String generateIcsContent(Meeting meeting) {
        // สร้าง LocalDateTime สำหรับเวลาเริ่มและจบ
        LocalDateTime startDateTime = LocalDateTime.of(meeting.getMeetingDate(), meeting.getMeetingTime());
        LocalDateTime endDateTime = startDateTime.plusHours(2); // +2 ชั่วโมง (รองรับข้ามวัน)

        // Format เป็น iCalendar format (yyyyMMddTHHmmss)
        DateTimeFormatter icsFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String dtStart = startDateTime.format(icsFormatter);
        String dtEnd = endDateTime.format(icsFormatter);
        String dtStamp = LocalDateTime.now().format(icsFormatter);

        // Escape ข้อความตามมาตรฐาน RFC 5545
        String description = escapeIcsText(stripHtml(meeting.getDescription()));
        String location = escapeIcsText(meeting.getLocation() != null ? meeting.getLocation() : "ไม่ระบุ");
        String subject = escapeIcsText(stripHtml(meeting.getDescription()));
        String uid = "MEETING-" + meeting.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        // สร้าง String แบบ iCalendar format
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("VERSION:2.0\n");
        sb.append("PRODID:-//ICT Booking Room//Meeting System//EN\n");
        sb.append("METHOD:REQUEST\n"); // ใช้ REQUEST เพื่อให้มีปุ่ม Accept/Decline
        sb.append("BEGIN:VEVENT\n");
        sb.append("UID:").append(uid).append("\n");
        sb.append("DTSTAMP:").append(dtStamp).append("\n");
        sb.append("DTSTART:").append(dtStart).append("\n");
        sb.append("DTEND:").append(dtEnd).append("\n");
        sb.append("SUMMARY:").append(subject).append("\n");
        sb.append("DESCRIPTION:").append(description).append("\n");
        sb.append("LOCATION:").append(location).append("\n");
        sb.append("ORGANIZER;CN=ICT Booking Admin:mailto:ictbookingroom@gmail.com\n");

        // เพิ่ม ATTENDEE สำหรับผู้เข้าร่วม พร้อม RSVP=TRUE
        if (meeting.getAttendees() != null) {
            for (CommitteeMember attendee : meeting.getAttendees()) {
                if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
                    String attendeeName = (attendee.getPrename() != null ? attendee.getPrename() : "")
                            + attendee.getFirstname() + " " + attendee.getLastname();
                    sb.append("ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=")
                            .append(escapeIcsText(attendeeName))
                            .append(":mailto:").append(attendee.getEmail()).append("\n");
                }
            }
        }

        sb.append("STATUS:CONFIRMED\n");
        sb.append("SEQUENCE:0\n");
        sb.append("CLASS:PUBLIC\n");
        sb.append("END:VEVENT\n");
        sb.append("END:VCALENDAR");

        return sb.toString();
    }

    // --- Helper 2: Escape ข้อความสำหรับ ICS (RFC 5545) ---
    private String escapeIcsText(String text) {
        if (text == null || text.isEmpty())
            return "";
        return text.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    // --- Helper 3: Notification ---
    private void createNotification(String type, String title, String message, Meeting meeting) {
        try {
            Notification n = new Notification();
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setRead(false);
            n.setTimestamp(LocalDateTime.now());
            n.setMeetingId(meeting.getId());
            n.setMeetingTypeCode(meeting.getMeetingTypeCode());

            notificationRepository.save(n);
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    // --- Helper 4: Strip HTML ---
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "-";
        }
        // ใช้ Regex นี้: <[^>]*> แปลว่า "หาเครื่องหมาย < ตามด้วยอะไรก็ได้ที่ไม่ใช่ >
        // แล้วปิดด้วย >"
        return html.replaceAll("<[^>]*>", "").trim();
    }

    // --- Helper 5: ดึงไฟล์แนบทั้งหมดจาก Agenda ---
    private java.util.List<java.util.Map<String, String>> extractAllAttachedFiles(Meeting meeting) {
        java.util.List<java.util.Map<String, String>> allFiles = new java.util.ArrayList<>();

        // ตรวจสอบแต่ละวาระ (1-5)
        String[] agendaFields = {
                meeting.getAgendaOneData(),
                meeting.getAgendaTwoData(),
                meeting.getAgendaThreeData(),
                meeting.getAgendaFourData(),
                meeting.getAgendaFiveData()
        };

        for (int i = 0; i < agendaFields.length; i++) {
            String agendaData = agendaFields[i];
            if (agendaData == null || agendaData.isEmpty())
                continue;

            try {
                // Parse JSON
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(agendaData);

                // ดึง attachedFiles array
                if (root.has("attachedFiles")) {
                    com.fasterxml.jackson.databind.JsonNode filesNode = root.get("attachedFiles");
                    if (filesNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode fileNode : filesNode) {
                            String fileName = fileNode.has("name") ? fileNode.get("name").asText() : "";
                            String fileUrl = fileNode.has("url") ? fileNode.get("url").asText() : "";

                            if (!fileName.isEmpty() && !fileUrl.isEmpty()) {
                                java.util.Map<String, String> fileInfo = new java.util.HashMap<>();
                                fileInfo.put("agendaNo", String.valueOf(i + 1));
                                fileInfo.put("name", fileName);
                                fileInfo.put("url", fileUrl);
                                allFiles.add(fileInfo);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing agenda " + (i + 1) + " data: " + e.getMessage());
            }
        }

        return allFiles;
    }

    // --- Helper 6: สร้าง HTML สำหรับแสดงไฟล์แนบ (Professional + Emoji) ---
    private String generateAttachedFilesHtml(java.util.List<java.util.Map<String, String>> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        // เริ่มต้น div container พร้อมเส้นแบ่ง
        html.append("<div style=\"margin-top: 15px; padding-top: 15px; border-top: 1px solid #e5e7eb;\">");

        // หัวข้อพร้อม emoji และจำนวนไฟล์
        html.append("<p style=\"margin: 5px 0 10px 0; font-weight: 600; color: #1f2937; font-size: 14px;\">");
        html.append("📎 เอกสารแนบ (").append(files.size()).append(" ไฟล์)");
        html.append("</p>");

        // container สำหรับรายการไฟล์
        html.append("<div style=\"margin: 0; padding: 0;\">");

        for (java.util.Map<String, String> file : files) {
            String fileName = file.get("name");
            String fileUrl = file.get("url");
            String agendaNo = file.get("agendaNo");

            // สร้าง full URL
            String fullUrl = "http://localhost:8080" + fileUrl;

            // แต่ละไฟล์
            html.append("<div style=\"margin: 6px 0; padding-left: 8px;\">");
            html.append("<span style=\"font-size: 14px;\">📄</span>");
            html.append("<a href=\"").append(fullUrl).append("\" ");
            html.append("style=\"color: #1f2937; text-decoration: none; font-size: 14px; margin-left: 4px;\">");
            html.append(fileName);
            html.append("</a>");
            html.append("<span style=\"color: #6b7280; font-size: 13px; margin-left: 6px;\">วาระที่ ");
            html.append(agendaNo);
            html.append("</span>");
            html.append("</div>");
        }

        html.append("</div>"); // ปิด files container
        html.append("</div>"); // ปิด main container

        return html.toString();
    }

    // --- Helper 7: ดึงไฟล์แนบจาก resolutionDetail ---
    private java.util.List<java.util.Map<String, String>> extractResolutionAttachedFiles(Meeting meeting) {
        java.util.List<java.util.Map<String, String>> allFiles = new java.util.ArrayList<>();

        String resolutionDetail = meeting.getResolutionDetail();
        if (resolutionDetail == null || resolutionDetail.isEmpty()) {
            return allFiles;
        }

        try {
            // Parse JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(resolutionDetail);

            // กรณีที่ 1: root เป็น array ของ resolutions
            if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode resolutionNode : root) {
                    extractFilesFromNode(resolutionNode, allFiles);
                }
            }
            // กรณีที่ 2: root เป็น object เดียว
            else if (root.isObject()) {
                extractFilesFromNode(root, allFiles);
            }
        } catch (Exception e) {
            System.err.println("Error parsing resolutionDetail data: " + e.getMessage());
            e.printStackTrace();
        }

        return allFiles;
    }

    // Helper method สำหรับดึงไฟล์จาก JsonNode
    private void extractFilesFromNode(com.fasterxml.jackson.databind.JsonNode node,
            java.util.List<java.util.Map<String, String>> allFiles) {
        // ตรวจสอบทั้ง "files" (สำหรับ resolutionDetail) และ "attachedFiles" (สำหรับ
        // agenda)
        String[] possibleFields = { "files", "attachedFiles" };

        for (String fieldName : possibleFields) {
            if (node.has(fieldName)) {
                com.fasterxml.jackson.databind.JsonNode filesNode = node.get(fieldName);
                if (filesNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode fileNode : filesNode) {
                        String fileName = fileNode.has("name") ? fileNode.get("name").asText() : "";
                        String fileUrl = fileNode.has("url") ? fileNode.get("url").asText() : "";

                        if (!fileName.isEmpty() && !fileUrl.isEmpty()) {
                            java.util.Map<String, String> fileInfo = new java.util.HashMap<>();
                            fileInfo.put("name", fileName);
                            fileInfo.put("url", fileUrl);
                            allFiles.add(fileInfo);
                        }
                    }
                }
                break; // พบแล้วไม่ต้องหาต่อ
            }
        }
    }

    // --- Helper 8: สร้าง HTML สำหรับแสดงไฟล์แนบจากรายละเอียดผลการประชุม
    // (ไม่แสดงวาระ) ---
    private String generateResolutionFilesHtml(java.util.List<java.util.Map<String, String>> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        // เริ่มต้น div container พร้อมเส้นแบ่ง
        html.append("<div style=\"margin-top: 15px; padding-top: 15px; border-top: 1px solid #e5e7eb;\">");

        // หัวข้อพร้อม emoji และจำนวนไฟล์
        html.append("<p style=\"margin: 5px 0 10px 0; font-weight: 600; color: #1f2937; font-size: 14px;\">");
        html.append("📎 เอกสารแนบ (").append(files.size()).append(" ไฟล์)");
        html.append("</p>");

        // container สำหรับรายการไฟล์
        html.append("<div style=\"margin: 0; padding: 0;\">");

        for (java.util.Map<String, String> file : files) {
            String fileName = file.get("name");
            String fileUrl = file.get("url");

            // สร้าง full URL
            String fullUrl = "http://localhost:8080" + fileUrl;

            // แต่ละไฟล์ (ไม่แสดงวาระเพราะมาจาก resolutionDetail)
            html.append("<div style=\"margin: 6px 0; padding-left: 8px;\">");
            html.append("<span style=\"font-size: 14px;\">📄</span>");
            html.append("<a href=\"").append(fullUrl).append("\" ");
            html.append("style=\"color: #1f2937; text-decoration: none; font-size: 14px; margin-left: 4px;\">");
            html.append(fileName);
            html.append("</a>");
            html.append("</div>");
        }

        html.append("</div>"); // ปิด files container
        html.append("</div>"); // ปิด main container

        return html.toString();
    }

    // --- API Endpoint: รับการตอบรับ/ปฏิเสธของว่าง ---
    @GetMapping("/meetings/{meetingId}/snacks-response")
    public ResponseEntity<String> handleSnacksResponse(
            @PathVariable Long meetingId,
            @RequestParam Long memberId,
            @RequestParam Boolean accept) {

        try {
            meetingService.updateSnacksResponse(meetingId, memberId, accept);

            String message = accept
                    ? "✅ ยอมรับของว่างเรียบร้อย ขอบคุณครับ/ค่ะ"
                    : "❌ ปฏิเสธของว่างเรียบร้อย ขอบคุณครับ/ค่ะ";

            // String bgColor = accept ? "#d1fae5" : "#fee2e2";
            String textColor = accept ? "#065f46" : "#991b1b";
            String icon = accept ? "✅" : "❌";

            // Return HTML page
            String htmlResponse = String.format(
                    "<html>" +
                            "<head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"font-family: 'Sarabun', Arial, sans-serif; text-align: center; padding: 50px; background-color: #f9fafb;\">"
                            +
                            "<div style=\"max-width: 500px; margin: 0 auto; background-color: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">"
                            +
                            "<div style=\"font-size: 64px; margin-bottom: 20px;\">%s</div>" +
                            "<h2 style=\"color: %s; margin: 20px 0;\">%s</h2>" +
                            "<p style=\"color: #6b7280; font-size: 16px;\">คุณสามารถปิดหน้านี้ได้แล้ว</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    icon,
                    textColor,
                    message);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlResponse);

        } catch (Exception e) {
            String errorHtml = String.format(
                    "<html>" +
                            "<head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"font-family: 'Sarabun', Arial, sans-serif; text-align: center; padding: 50px; background-color: #f9fafb;\">"
                            +
                            "<div style=\"max-width: 500px; margin: 0 auto; background-color: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">"
                            +
                            "<div style=\"font-size: 64px; margin-bottom: 20px;\">⚠️</div>" +
                            "<h2 style=\"color: #dc2626; margin: 20px 0;\">เกิดข้อผิดพลาด</h2>" +
                            "<p style=\"color: #6b7280; font-size: 16px;\">%s</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    e.getMessage());

            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }
    }

    // --- API Endpoint: ดูสรุปผลการตอบรับของว่าง (สำหรับ Admin) ---
    @GetMapping("/meetings/{id}/snacks-summary")
    public ResponseEntity<?> getSnacksSummary(@PathVariable Long id) {
        try {
            Meeting meeting = meetingService.getMeetingById(id);

            java.util.Map<String, Object> summary = new java.util.HashMap<>();

            java.util.List<java.util.Map<String, Object>> accepted = meeting.getMeetingAttendees().stream()
                    .filter(ma -> ma.getSnacksAccepted())
                    .map(ma -> {
                        java.util.Map<String, Object> info = new java.util.HashMap<>();
                        CommitteeMember m = ma.getMember();
                        String fullName = (m.getPrename() != null ? m.getPrename() : "")
                                + m.getFirstname() + " " + m.getLastname();
                        info.put("name", fullName);
                        info.put("email", m.getEmail());
                        info.put("responseTime", ma.getSnacksResponseTime());
                        return info;
                    })
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<java.util.Map<String, Object>> declined = meeting.getMeetingAttendees().stream()
                    .filter(ma -> !ma.getSnacksAccepted())
                    .map(ma -> {
                        java.util.Map<String, Object> info = new java.util.HashMap<>();
                        CommitteeMember m = ma.getMember();
                        String fullName = (m.getPrename() != null ? m.getPrename() : "")
                                + m.getFirstname() + " " + m.getLastname();
                        info.put("name", fullName);
                        info.put("email", m.getEmail());
                        info.put("responseTime", ma.getSnacksResponseTime());
                        return info;
                    })
                    .collect(java.util.stream.Collectors.toList());

            summary.put("meetingNo", meeting.getMeetingNo());
            summary.put("description", meeting.getDescription());
            summary.put("meetingDate", meeting.getMeetingDate());
            summary.put("totalAttendees", meeting.getMeetingAttendees().size());
            summary.put("acceptedCount", accepted.size());
            summary.put("declinedCount", declined.size());
            summary.put("accepted", accepted);
            summary.put("declined", declined);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}