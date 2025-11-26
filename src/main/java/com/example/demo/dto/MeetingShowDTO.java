package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Map;

public class MeetingShowDTO {
    private Long id;
    private String meetingNo;
    private LocalDate meetingDate;
    private LocalTime meetingTime;
    private String location;
    private String description;
    private String meetingTypeCode;
    private String status;
    private LocalDateTime createdAt;

    // agenda data เป็น Map<String, Object> เพื่อเก็บข้อมูล JSON ที่แปลงแล้ว
    private Map<String, Object> agenda1Data;
    private Map<String, Object> agenda2Data;
    private Map<String, Object> agenda3Data;

    // --- getter / setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMeetingNo() {
        return meetingNo;
    }

    public void setMeetingNo(String meetingNo) {
        this.meetingNo = meetingNo;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public LocalTime getMeetingTime() {
        return meetingTime;
    }

    public void setMeetingTime(LocalTime meetingTime) {
        this.meetingTime = meetingTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMeetingTypeCode() {
        return meetingTypeCode;
    }

    public void setMeetingTypeCode(String meetingTypeCode) {
        this.meetingTypeCode = meetingTypeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getAgenda1Data() {
        return agenda1Data;
    }

    public void setAgenda1Data(Map<String, Object> agenda1Data) {
        this.agenda1Data = agenda1Data;
    }

    public Map<String, Object> getAgenda2Data() {
        return agenda2Data;
    }

    public void setAgenda2Data(Map<String, Object> agenda2Data) {
        this.agenda2Data = agenda2Data;
    }

    public Map<String, Object> getAgenda3Data() {
        return agenda3Data;
    }

    public void setAgenda3Data(Map<String, Object> agenda3Data) {
        this.agenda3Data = agenda3Data;
    }
}