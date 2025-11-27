package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String status;
    private List<Long> memberIds;

    @JsonProperty("agenda_1_data")
    private String agendaOneData;
    @JsonProperty("agenda_2_data")
    private String agendaTwoData;
    @JsonProperty("agenda_3_data")
    private String agendaThreeData;
    @JsonProperty("agenda_4_data")
    private String agendaFourData;
    @JsonProperty("agenda_5_data")
    private String agendaFiveData;
}
