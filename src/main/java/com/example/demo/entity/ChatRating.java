package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_ratings")
public class ChatRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id", nullable = false)
    private String logId; // ID ของข้อความที่ต้องการให้คะแนน (ตรงกับ message.id จาก frontend)

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage; // คำถามของผู้ใช้

    @Column(name = "bot_response", columnDefinition = "TEXT")
    private String botResponse; // คำตอบของ AI

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private RatingType rating; // LIKE หรือ DISLIKE

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment; // ความคิดเห็นเพิ่มเติม (optional)

    @Column(name = "user_name")
    private String userName; // ชื่อผู้ใช้ที่ให้คะแนน (optional)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RatingType {
        LIKE,
        DISLIKE
    }

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
    public ChatRating() {
    }

    public ChatRating(String logId, String userMessage, String botResponse, RatingType rating, String comment, String userName) {
        this.logId = logId;
        this.userMessage = userMessage;
        this.botResponse = botResponse;
        this.rating = rating;
        this.comment = comment;
        this.userName = userName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getBotResponse() {
        return botResponse;
    }

    public void setBotResponse(String botResponse) {
        this.botResponse = botResponse;
    }

    public RatingType getRating() {
        return rating;
    }

    public void setRating(RatingType rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
        return "ChatRating{" +
                "id=" + id +
                ", logId='" + logId + '\'' +
                ", rating=" + rating +
                ", userName='" + userName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
