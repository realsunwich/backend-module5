package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.CommitteeMember;
import com.example.demo.entity.Meeting;
import com.example.demo.entity.Notification;
import com.example.demo.service.EmailService;
import com.example.demo.service.MeetingService;
import com.example.demo.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

        if ("ACTIVE".equalsIgnoreCase(updatedMeeting.getStatus())
                && updatedMeeting.getAttendees() != null && !updatedMeeting.getAttendees().isEmpty()
                && request.getCurrentStep() != null && request.getCurrentStep() == 5) {

            String meetingUrl = getMeetingUrl(updatedMeeting.getMeetingTypeCode(), updatedMeeting.getId());
            String emailTitle = "แจ้งนัดหมายการประชุม";

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
                    meetingUrl);

            emailService.sendMeetingNotification("nuntiya.suw@ilustro.co", emailTitle, adminEmailBody);

            // จากนั้นส่งให้ผู้เข้าร่วมประชุมทุกคน
            for (CommitteeMember attendee : updatedMeeting.getAttendees()) {
                if (attendee.getEmail() != null && !attendee.getEmail().isEmpty()) {
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
                                    "<p style=\"margin: 0;\">อีเมลฉบับนี้เป็นการแจ้งเตือนอัตโนมัติ กรุณาอย่าตอบกลับ</p>"
                                    +
                                    "</div>" +
                                    "</div>" +
                                    "</body>" +
                                    "</html>",
                            attendee.getPrename() != null
                                    ? attendee.getPrename() + attendee.getFirstname() + " " + attendee.getLastname()
                                    : attendee.getFirstname() + " " + attendee.getLastname(),
                            updatedMeeting.getMeetingNo(),
                            updatedMeeting.getDescription() != null ? updatedMeeting.getDescription() : "-",
                            updatedMeeting.getMeetingDate(),
                            updatedMeeting.getMeetingTime(),
                            updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "-",
                            meetingUrl);

                    emailService.sendMeetingNotification(attendee.getEmail(), emailTitle, attendeeEmailBody);
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
                        meetingUrl);

                // ส่งอีเมลให้ผู้เกี่ยวข้อง
                emailService.sendMeetingNotification("nuntiya.suw@ilustro.co", title, emailBodyForStaff);

                // ส่งอีเมลให้ผู้ดูแลระบบ
                emailService.sendMeetingNotification("ictbookingroom@outlook.com", title, emailBodyForAdmin);
            }

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating resolutions: " + e.getMessage());
        }
    }

    // Helper Function
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
}