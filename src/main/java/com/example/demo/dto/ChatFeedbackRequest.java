package com.example.demo.dto;

public class ChatFeedbackRequest {

    private String logId;           // ID ของข้อความที่ให้คะแนน
    private String rating;          // "LIKE" หรือ "DISLIKE"
    private String comment;         // ความคิดเห็นเพิ่มเติม (optional)
    private String userName;        // ชื่อผู้ใช้ (optional)
    private String userMessage;     // คำถามของผู้ใช้ (optional, สำหรับบันทึก context)
    private String botResponse;     // คำตอบของ AI (optional, สำหรับบันทึก context)

    // Constructors
    public ChatFeedbackRequest() {
    }

    public ChatFeedbackRequest(String logId, String rating, String comment, String userName, String userMessage, String botResponse) {
        this.logId = logId;
        this.rating = rating;
        this.comment = comment;
        this.userName = userName;
        this.userMessage = userMessage;
        this.botResponse = botResponse;
    }

    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
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

    @Override
    public String toString() {
        return "ChatFeedbackRequest{" +
                "logId='" + logId + '\'' +
                ", rating='" + rating + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
