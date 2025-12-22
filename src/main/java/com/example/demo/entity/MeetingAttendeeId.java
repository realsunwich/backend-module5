package com.example.demo.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MeetingAttendeeId implements Serializable {

    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "member_id")
    private Long memberId;

    public MeetingAttendeeId() {
    }

    public MeetingAttendeeId(Long meetingId, Long memberId) {
        this.meetingId = meetingId;
        this.memberId = memberId;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MeetingAttendeeId that = (MeetingAttendeeId) o;
        return Objects.equals(meetingId, that.meetingId) &&
                Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meetingId, memberId);
    }
}
