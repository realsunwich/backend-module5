package com.example.demo.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "meeting_attendees")
public class MeetingAttendee {

    @EmbeddedId
    private MeetingAttendeeId id;

    @ManyToOne
    @MapsId("meetingId")
    @JoinColumn(name = "meeting_id")
    @JsonIgnoreProperties({"meetingAttendees", "attendees"})
    private Meeting meeting;

    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    @JsonIgnoreProperties({"meetingAttendees"})
    private CommitteeMember member;

    @Column(name = "snacks_accepted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean snacksAccepted = false;

    @Column(name = "snacks_response_time")
    private LocalDateTime snacksResponseTime;

    public MeetingAttendee() {
    }

    public MeetingAttendee(Meeting meeting, CommitteeMember member) {
        this.meeting = meeting;
        this.member = member;
        this.id = new MeetingAttendeeId(meeting.getId(), member.getId());
        this.snacksAccepted = false;
    }

    public MeetingAttendeeId getId() {
        return id;
    }

    public void setId(MeetingAttendeeId id) {
        this.id = id;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public CommitteeMember getMember() {
        return member;
    }

    public void setMember(CommitteeMember member) {
        this.member = member;
    }

    public Boolean getSnacksAccepted() {
        return snacksAccepted;
    }

    public void setSnacksAccepted(Boolean snacksAccepted) {
        this.snacksAccepted = snacksAccepted;
    }

    public LocalDateTime getSnacksResponseTime() {
        return snacksResponseTime;
    }

    public void setSnacksResponseTime(LocalDateTime snacksResponseTime) {
        this.snacksResponseTime = snacksResponseTime;
    }
}
