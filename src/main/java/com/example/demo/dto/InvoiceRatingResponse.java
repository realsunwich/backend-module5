package com.example.demo.dto;

public class InvoiceRatingResponse {

    private boolean success;
    private String message;
    private Long ratingId;

    // Constructors
    public InvoiceRatingResponse() {
    }

    public InvoiceRatingResponse(boolean success, String message, Long ratingId) {
        this.success = success;
        this.message = message;
        this.ratingId = ratingId;
    }

    // Static factory methods
    public static InvoiceRatingResponse success(String message, Long ratingId) {
        return new InvoiceRatingResponse(true, message, ratingId);
    }

    public static InvoiceRatingResponse error(String message) {
        return new InvoiceRatingResponse(false, message, null);
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
        return "InvoiceRatingResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", ratingId=" + ratingId +
                '}';
    }
}
