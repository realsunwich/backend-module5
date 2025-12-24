package com.example.demo.dto;

public class ChatFeedbackResponse {

    private boolean success;
    private String message;
    private Long ratingId;

    // Constructors
    public ChatFeedbackResponse() {
    }

    public ChatFeedbackResponse(boolean success, String message, Long ratingId) {
        this.success = success;
        this.message = message;
        this.ratingId = ratingId;
    }

    // Static factory methods
    public static ChatFeedbackResponse success(String message, Long ratingId) {
        return new ChatFeedbackResponse(true, message, ratingId);
    }

    public static ChatFeedbackResponse error(String message) {
        return new ChatFeedbackResponse(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }

    @Override
    public String toString() {
        return "ChatFeedbackResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", ratingId=" + ratingId +
                '}';
    }
}
