package com.example.demo.entity; // ปรับ package ตามโปรเจคคุณ

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // "NEW_MEETING" หรือ "STATUS_CHANGE"
    private String title;
    private String message;

    @Column(name = "is_read")
    private boolean isRead = false;

    private LocalDateTime timestamp;

    // ข้อมูลสำหรับ Link ไปหน้างาน
    private Long meetingId;
    private String meetingTypeCode;
}