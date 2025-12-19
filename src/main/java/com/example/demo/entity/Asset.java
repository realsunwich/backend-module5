package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Data // ใช้ Lombok เพื่อลด code getter/setter
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    // เก็บพิกัดเป็น Double
    private Double latitude;
    private Double longitude;

    // ฟีลด์ใหม่
    @Column(name = "asset_type")
    private String assetType; // ประเภทหลักฐาน: เอกสาร, อุปกรณ์, อาคาร, ยานพาหนะ, ที่ดิน, อื่นๆ

    private Integer quantity; // จำนวน

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.PENDING; // สถานะ: PENDING, CONFIRMED, CHECKED_IN

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt; // วันเวลาที่เช็คอิน

    // ฟีลด์สำหรับบันทึกการยืนยันการพบหลักฐาน
    @Column(name = "confirm_notes", length = 2000)
    private String confirmNotes; // บันทึกเพิ่มเติมเมื่อยืนยันการพบหลักฐาน

    @Column(name = "photo_urls", length = 5000)
    private String photoUrls; // URL ของรูปภาพหลักฐาน (เก็บเป็น JSON array string)

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt; // วันเวลาที่ยืนยันการพบหลักฐาน

    // ฟีลด์สำหรับมูลค่าหลักฐาน
    @Column(name = "total_value")
    private Double totalValue; // มูลค่ารวมทั้งหมด

    @Column(name = "individual_values", length = 3000)
    private String individualValues; // มูลค่าแต่ละชิ้น (เก็บเป็น JSON array) เช่น [1000, 2000, 1500]

    @Column(name = "value_unit", length = 50)
    private String valueUnit; // หน่วยเงิน เช่น "บาท", "USD", "ล้านบาท"

    @Column(name = "bank_details", columnDefinition = "TEXT")
    private String bankDetails; // ข้อมูลบัญชีธนาคาร (เก็บเป็น JSON array)

    // เพิ่มฟีลด์ timestamp
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // วันที่สร้าง (auto-generated)

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // วันที่อัพเดทล่าสุด (auto-updated)

    // Enum สำหรับสถานะ
    public enum AssetStatus {
        PENDING, // รอตรวจสอบ
        CONFIRMED, // พบแล้ว
        CHECKED_IN // เช็คอินแล้ว
    }
}