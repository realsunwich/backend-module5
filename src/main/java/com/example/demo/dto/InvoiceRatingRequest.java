package com.example.demo.dto;

public class InvoiceRatingRequest {

    private String invoiceNumber; // เลขที่ใบกำกับภาษี
    private String invoiceData; // ข้อมูล invoice ที่ผ่านการแก้ไขแล้ว (JSON format)
    private Double originalConfidence; // ความมั่นใจของ AI ตอนแรก
    private Integer userRating; // คะแนนที่ user ให้ (1-5 ดาว)
    private String userFeedback; // ความคิดเห็นเพิ่มเติมจาก user
    private String userName; // ชื่อผู้ใช้ที่ให้คะแนน (optional)

    // Constructors
    public InvoiceRatingRequest() {
    }

    public InvoiceRatingRequest(String invoiceNumber, String invoiceData, Double originalConfidence,
                               Integer userRating, String userFeedback, String userName) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceData = invoiceData;
        this.originalConfidence = originalConfidence;
        this.userRating = userRating;
        this.userFeedback = userFeedback;
        this.userName = userName;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "InvoiceRatingRequest{" +
                "invoiceNumber='" + invoiceNumber + '\'' +
                ", originalConfidence=" + originalConfidence +
                ", userRating=" + userRating +
                ", userName='" + userName + '\'' +
                '}';
    }
}
