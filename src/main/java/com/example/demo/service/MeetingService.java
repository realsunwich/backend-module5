package com.example.demo.service;

import com.example.demo.dto.MeetingRequest;
import com.example.demo.entity.CommitteeMember;
import com.example.demo.entity.Meeting;
import com.example.demo.entity.MeetingAttendee;
import com.example.demo.repository.CommitteeMemberRepository;
import com.example.demo.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        // 1. กลุ่มตั้งค่า PDF
        if (request.getPdfConfig() != null) {
            meeting.setPdfConfig(request.getPdfConfig());
        }

        // 2. กลุ่มข้อมูลทั่วไป (เช็ค null ทุกตัว เพื่อไม่ให้ของเดิมหาย)
        if (request.getMeetingTypeCode() != null)
            meeting.setMeetingTypeCode(request.getMeetingTypeCode());
        if (request.getMeetingDate() != null)
            meeting.setMeetingDate(request.getMeetingDate());
        if (request.getMeetingTime() != null)
            meeting.setMeetingTime(request.getMeetingTime());
        if (request.getLocation() != null)
            meeting.setLocation(request.getLocation());
        if (request.getDescription() != null)
            meeting.setDescription(request.getDescription());
        if (request.getStatus() != null)
            meeting.setStatus(request.getStatus());

        // 3. กลุ่มวาระการประชุม (Agenda Data)
        if (request.getAgendaOneData() != null)
            meeting.setAgendaOneData(request.getAgendaOneData());
        if (request.getAgendaTwoData() != null)
            meeting.setAgendaTwoData(request.getAgendaTwoData());
        if (request.getAgendaThreeData() != null)
            meeting.setAgendaThreeData(request.getAgendaThreeData());
        if (request.getAgendaFourData() != null)
            meeting.setAgendaFourData(request.getAgendaFourData());
        if (request.getAgendaFiveData() != null)
            meeting.setAgendaFiveData(request.getAgendaFiveData());

        // 4. กลุ่มมติ (Resolution)
        if (request.getResolutionDetail() != null)
            meeting.setResolutionDetail(request.getResolutionDetail());
        if (request.getResolutionFourData() != null)
            meeting.setResolutionFourData(request.getResolutionFourData());
        if (request.getResolutionFiveData() != null)
            meeting.setResolutionFiveData(request.getResolutionFiveData());

        // 5. กลุ่มผู้เข้าร่วม (Attendees)
        // Logic นี้สำคัญ: ถ้าส่ง null มา = ไม่ทำอะไร, ถ้าส่ง [] มา = ลบหมด, ถ้าส่ง ids
        // มา = อัปเดตใหม่
        if (request.getMemberIds() != null) {
            List<CommitteeMember> attendees = memberRepository.findAllById(request.getMemberIds());
            meeting.setAttendees(attendees);
        }

        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting updateMeetingResolutions(Long id, MeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));
        meeting.setResolutionDetail(request.getResolutionDetail());
        meeting.setResolutionFourData(request.getResolutionFourData());
        meeting.setResolutionFiveData(request.getResolutionFiveData());

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            meeting.setStatus(request.getStatus());
        }

        return meetingRepository.save(meeting);
    }

    @Transactional
    public void updateSnacksResponse(Long meetingId, Long memberId, Boolean accept) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        MeetingAttendee attendee = meeting.getMeetingAttendees().stream()
                .filter(ma -> ma.getMember().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Member not found in this meeting"));

        attendee.setSnacksAccepted(accept);
        attendee.setSnacksResponseTime(LocalDateTime.now());

        meetingRepository.save(meeting);
    }
}