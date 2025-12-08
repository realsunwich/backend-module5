package com.example.demo.controller;

import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:3000") // 🔥 อนุญาตให้ Frontend เรียกใช้
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    // --- 1. GET: ดึงรายการแจ้งเตือนทั้งหมด ---
    // API: http://localhost:8080/api/notifications
    @GetMapping
    public List<Notification> getNotifications() {
        // ดึงข้อมูลทั้งหมด โดยเรียงจากใหม่ไปเก่า (Timestamp Descending)
        return notificationRepository.findAllByOrderByTimestampDesc();
    }

    // --- 2. PUT: อ่านแล้ว 1 รายการ ---
    // API: http://localhost:8080/api/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- 3. PUT: อ่านทั้งหมด ---
    // API: http://localhost:8080/api/notifications/read-all
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        List<Notification> all = notificationRepository.findAll();
        for (Notification n : all) {
            n.setRead(true);
        }
        notificationRepository.saveAll(all);
        return ResponseEntity.ok().build();
    }

    // --- (Optional) API สำหรับเคลียร์การแจ้งเตือนทั้งหมด (ลบ) ---
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAll() {
        notificationRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}