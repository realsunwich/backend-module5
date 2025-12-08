package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
public class MeetingRequest {
    private String meetingTitle;
    private String meetingNo;
    private String meetingTypeCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime meetingTime;

    private String location;
    private String description;
    private String status;
    private List<Long> memberIds;

    private Integer currentStep;

    @JsonProperty("agenda_1_data")
    @JsonAlias("agendaOneData")
    private String agendaOneData;

    @JsonProperty("agenda_2_data")
    @JsonAlias("agendaTwoData")
    private String agendaTwoData;

    @JsonProperty("agenda_3_data")
    @JsonAlias("agendaThreeData")
    private String agendaThreeData;

    @JsonProperty("agenda_4_data")
    @JsonAlias("agendaFourData")
    private String agendaFourData;

    @JsonProperty("agenda_5_data")
    @JsonAlias("agendaFiveData")
    private String agendaFiveData;

    private String resolutionDetail;
    private String resolutionFourData;
    private String resolutionFiveData;

    private List<Map<String, Object>> attendees;

}