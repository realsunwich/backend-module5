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

    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody MeetingRequest request) {
        try {
            Meeting newMeeting = meetingService.createMeeting(request);

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

    @PutMapping("/meetings/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long id, @RequestBody MeetingRequest request) {
        Meeting updatedMeeting = meetingService.updateMeeting(id, request);

        // 🔥 แก้ไข: เช็ค null ก่อนเปรียบเทียบ (ป้องกัน NullPointerException)
        // จากเดิม: if (request.getCurrentStep() == 5 ...
        if (request.getCurrentStep() != null && request.getCurrentStep() == 5
                && "ACTIVE".equalsIgnoreCase(updatedMeeting.getStatus())) {

            String title = "การบันทึกข้อมูลวาระเสร็จสิ้น";
            String message = String.format("การประชุมรหัส %s บันทึกวาระที่ 5 ครบถ้วนแล้ว พร้อมสำหรับการตรวจสอบ",
                    updatedMeeting.getMeetingNo());

            createNotification("STATUS_CHANGE", title, message, updatedMeeting);
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

    @PutMapping("/meetings/{id}/resolutions")
    public ResponseEntity<?> updateMeetingResolutions(
            @PathVariable Long id,
            @RequestBody MeetingRequest request) {
        try {
            Meeting updated = meetingService.updateMeetingResolutions(id, request);

            if ("PUBLISH".equalsIgnoreCase(updated.getStatus())) {
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