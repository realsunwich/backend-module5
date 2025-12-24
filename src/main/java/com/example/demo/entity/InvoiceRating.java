package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_ratings")
public class InvoiceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number")
    private String invoiceNumber; // เลขที่ใบกำกับภาษี

    @Column(name = "invoice_data", columnDefinition = "TEXT")
    private String invoiceData; // ข้อมูล invoice ที่ผ่านการแก้ไขแล้ว (JSON format)

    @Column(name = "original_confidence")
    private Double originalConfidence; // ความมั่นใจของ AI ตอนแรก (0.0 - 1.0)

    @Column(name = "user_rating")
    private Integer userRating; // คะแนนที่ user ให้ (1-5 ดาว)

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback; // ความคิดเห็นเพิ่มเติมจาก user

    @Column(name = "user_name")
    private String userName; // ชื่อผู้ใช้ที่ให้คะแนน (optional)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public InvoiceRating() {
    }

    public InvoiceRating(String invoiceNumber, String invoiceData, Double originalConfidence,
                        Integer userRating, String userFeedback, String userName) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceData = invoiceData;
        this.originalConfidence = originalConfidence;
        this.userRating = userRating;
        this.userFeedback = userFeedback;
        this.userName = userName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceData() {
        return invoiceData;
    }

    public void setInvoiceData(String invoiceData) {
        this.invoiceData = invoiceData;
    }

    public Double getOriginalConfidence() {
        return originalConfidence;
    }

    public void setOriginalConfidence(Double originalConfidence) {
        this.originalConfidence = originalConfidence;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "InvoiceRating{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", originalConfidence=" + originalConfidence +
                ", userRating=" + userRating +
                ", userName='" + userName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
