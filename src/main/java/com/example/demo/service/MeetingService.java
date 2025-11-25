package com.example.demo.service;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.CommitteeMember;
import com.example.demo.entity.Meeting;
import com.example.demo.repository.CommitteeMemberRepository;
import com.example.demo.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private CommitteeMemberRepository memberRepository;

    @Transactional
    public Meeting createMeeting(MeetingRequest request) {
        Meeting meeting = new Meeting();

        meeting.setMeetingTypeCode(request.getMeetingTypeCode());

        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setMeetingTime(request.getMeetingTime());
        meeting.setLocation(request.getLocation());
        meeting.setDescription(request.getDescription());
        meeting.setCurrentStep(request.getCurrentStep());

        meeting.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");

        meeting.setMeetingNo(generateMeetingNo());

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<CommitteeMember> attendees = memberRepository.findAllById(request.getMemberIds());
            meeting.setAttendees(attendees);
        }

        return meetingRepository.save(meeting);
    }

    private String generateMeetingNo() {
        return "001/68" + (System.currentTimeMillis() % 1000);
    }
}