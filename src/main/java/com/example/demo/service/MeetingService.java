package com.example.demo.service;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.CommitteeMember;
import com.example.demo.entity.Meeting;
import com.example.demo.repository.CommitteeMemberRepository;
import com.example.demo.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private CommitteeMemberRepository memberRepository;

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    // ใน class MeetingService
    public Meeting getMeetingById(Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));
    }

    @Transactional
    public Meeting createMeeting(MeetingRequest request) {
        Meeting meeting = new Meeting();

        meeting.setMeetingTypeCode(request.getMeetingTypeCode());
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setMeetingTime(request.getMeetingTime());
        meeting.setLocation(request.getLocation());
        meeting.setDescription(request.getDescription());
        meeting.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");

        meeting.setAgendaOneData(request.getAgendaOneData());
        meeting.setAgendaTwoData(request.getAgendaTwoData());
        meeting.setAgendaThreeData(request.getAgendaThreeData());
        meeting.setAgendaFourData(request.getAgendaFourData());
        meeting.setAgendaFiveData(request.getAgendaFiveData());

        meeting.setMeetingNo(generateMeetingNo(request.getMeetingTypeCode()));

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<CommitteeMember> attendees = memberRepository.findAllById(request.getMemberIds());
            meeting.setAttendees(attendees);
        }

        return meetingRepository.save(meeting);
    }

    private String generateMeetingNo(String typeCode) {
        int thaiYear = LocalDate.now().getYear() + 543;
        String yearTwoDigits = String.valueOf(thaiYear).substring(2);

        String code = (typeCode != null && !typeCode.isEmpty()) ? typeCode : "001";
        String prefix = code + "/" + yearTwoDigits;

        Meeting lastMeeting = meetingRepository.findTopByMeetingNoStartingWithOrderByIdDesc(prefix);

        if (lastMeeting == null || lastMeeting.getMeetingNo() == null) {
            return prefix + "001";
        }

        String lastNo = lastMeeting.getMeetingNo();

        try {
            String runningNumberStr = lastNo.substring(prefix.length());
            int nextNumber = Integer.parseInt(runningNumberStr) + 1;

            return prefix + String.format("%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return prefix + "001"; // กันพลาด
        }
    }

    @Transactional
    public Meeting updateMeeting(Long id, MeetingRequest request) {

        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        meeting.setMeetingTypeCode(request.getMeetingTypeCode());
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setMeetingTime(request.getMeetingTime());
        meeting.setLocation(request.getLocation());
        meeting.setDescription(request.getDescription());
        meeting.setStatus(request.getStatus());

        meeting.setAgendaOneData(request.getAgendaOneData());
        meeting.setAgendaTwoData(request.getAgendaTwoData());
        meeting.setAgendaThreeData(request.getAgendaThreeData());
        meeting.setAgendaFourData(request.getAgendaFourData());
        meeting.setAgendaFiveData(request.getAgendaFiveData());

        if (request.getMemberIds() != null) {
            List<CommitteeMember> attendees = memberRepository.findAllById(request.getMemberIds());
            meeting.setAttendees(attendees);
        }

        return meetingRepository.save(meeting);
    }
}