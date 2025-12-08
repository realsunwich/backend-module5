package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.Meeting;
import com.example.demo.entity.Notification;
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

    @GetMapping("/meetings")
    public ResponseEntity<?> getAllMeetings() {
        try {
            return ResponseEntity.ok(meetingService.getAllMeetings());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- 1. สร้างการประชุม (ปรับปรุงข้อความแจ้งเตือน) ---
    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody MeetingRequest request) {
        try {
            Meeting newMeeting = meetingService.createMeeting(request);

            // ✅ ปรับปรุง: ดึง "ชื่อเรื่อง" และ "วันเวลา" มาแสดงให้ชัดเจน
            String title = "มีการนัดหมายการประชุมใหม่";
            String message = String.format("เรื่อง %s (รหัส %s) นัดหมายวันที่ %s เวลา %s",
                    newMeeting.getDescription(),
                    newMeeting.getMeetingNo(),
                    newMeeting.getMeetingDate(),
                    newMeeting.getMeetingTime());

            createNotification("NEW_MEETING", title, message, newMeeting);

            return ResponseEntity.ok(newMeeting);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- 2. อัปเดตสถานะ (ปรับปรุงข้อความแจ้งเตือน) ---
    @PutMapping("/meetings/{id}")
    public ResponseEntity<?> updateMeeting(
            @PathVariable Long id,
            @RequestBody MeetingRequest request) {
        try {
            Meeting updatedMeeting = meetingService.updateMeeting(id, request);

            if (updatedMeeting.getStatus() != null) {
                // ✅ ปรับปรุง: แสดงชื่อเรื่อง และสถานะที่เปลี่ยนไปอย่างชัดเจน
                String title = "สถานะการประชุมอัปเดต";
                String message = String.format("เรื่อง %s (รหัส %s) สถานะ%s",
                        updatedMeeting.getDescription(),
                        updatedMeeting.getMeetingNo(),
                        updatedMeeting.getStatus());

                createNotification("STATUS_CHANGE", title, message, updatedMeeting);
            }

            return ResponseEntity.ok(updatedMeeting);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating meeting: " + e.getMessage());
        }
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

    // --- 3. บันทึกมติ/ปิดการประชุม (ปรับปรุงข้อความแจ้งเตือน) ---
    @PutMapping("/meetings/{id}/resolutions")
    public ResponseEntity<?> updateMeetingResolutions(
            @PathVariable Long id,
            @RequestBody MeetingRequest request) {
        try {
            Meeting updated = meetingService.updateMeetingResolutions(id, request);

            if ("PUBLISH".equalsIgnoreCase(updated.getStatus())) {
                // ✅ ปรับปรุง: แจ้งเตือนเมื่อประชุมเสร็จสิ้น (PUBLISH)
                String title = "สรุปผลการประชุมเรียบร้อย";
                String message = String.format("เรื่อง %s (รหัส %s) ได้รับการลงมติและสรุปผลแล้ว",
                        updated.getDescription(),
                        updated.getMeetingNo());

                createNotification("STATUS_CHANGE", title, message, updated);
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
            n.setMessage(message); // ข้อความที่ปรับปรุงแล้วจะถูกบันทึกที่นี่
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