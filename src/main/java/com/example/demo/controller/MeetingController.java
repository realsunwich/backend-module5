package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.Meeting;
import com.example.demo.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

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
    public ResponseEntity<?> updateMeeting(
            @PathVariable Long id,
            @RequestBody MeetingRequest request) {
        try {
            Meeting updated = meetingService.updateMeeting(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}