package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class MeetingRequest {
    private String meetingTypeCode;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime meetingTime;

    private String location;
    private String description;
    private Integer currentStep;
    private String status;
    private List<Long> memberIds;
}