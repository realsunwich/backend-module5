package com.example.demo.controller;

import com.example.demo.dto.MeetingShowDTO;
import com.example.demo.entity.Meeting;
import com.example.demo.repository.ShowMeetingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class ShowMeetingController {

    private final ShowMeetingRepository repository;
    private final ObjectMapper objectMapper;

    public ShowMeetingController(ShowMeetingRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/showmeeting")
    public List<MeetingShowDTO> getAllMeetings() {
        List<Meeting> meetings = repository.findAll();

        return meetings.stream().map(meeting -> {
            MeetingShowDTO dto = new MeetingShowDTO();
            dto.setId(meeting.getId());
            dto.setMeetingNo(meeting.getMeetingNo());
            dto.setMeetingDate(meeting.getMeetingDate());
            dto.setMeetingTime(meeting.getMeetingTime());
            dto.setLocation(meeting.getLocation());
            dto.setDescription(meeting.getDescription());
            dto.setMeetingTypeCode(meeting.getMeetingTypeCode());
            dto.setStatus(meeting.getStatus());
            dto.setCreatedAt(meeting.getCreatedAt());

            try {
                if (meeting.getAgendaOneData() != null && !meeting.getAgendaOneData().isEmpty()) {
                    dto.setAgenda1Data(objectMapper.readValue(meeting.getAgendaOneData(),
                            new TypeReference<Map<String, Object>>() {
                            }));
                }
                if (meeting.getAgendaTwoData() != null && !meeting.getAgendaTwoData().isEmpty()) {
                    dto.setAgenda2Data(objectMapper.readValue(meeting.getAgendaTwoData(),
                            new TypeReference<Map<String, Object>>() {
                            }));
                }
                if (meeting.getAgendaThreeData() != null && !meeting.getAgendaThreeData().isEmpty()) {
                    dto.setAgenda3Data(objectMapper.readValue(meeting.getAgendaThreeData(),
                            new TypeReference<Map<String, Object>>() {
                            }));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return dto;
        }).collect(Collectors.toList());
    }
}
