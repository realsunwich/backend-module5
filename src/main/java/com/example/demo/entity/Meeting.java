package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "meetings")
@Data
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_no")
    private String meetingNo;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(name = "meeting_time")
    private LocalTime meetingTime;

    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "meeting_type_code")
    private String meetingTypeCode;

    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "agenda_1_data", columnDefinition = "LONGTEXT")
    private String agendaOneData;

    @Column(name = "agenda_2_data", columnDefinition = "LONGTEXT")
    private String agendaTwoData;

    @Column(name = "agenda_3_data", columnDefinition = "LONGTEXT")
    private String agendaThreeData;

    @Column(name = "agenda_4_data", columnDefinition = "LONGTEXT")
    private String agendaFourData;

    @Column(name = "agenda_5_data", columnDefinition = "LONGTEXT")
    private String agendaFiveData;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"meeting"})
    private List<MeetingAttendee> meetingAttendees = new ArrayList<>();

    @Transient
    public List<CommitteeMember> getAttendees() {
        return meetingAttendees.stream()
                .map(MeetingAttendee::getMember)
                .collect(Collectors.toList());
    }

    public void setAttendees(List<CommitteeMember> members) {
        this.meetingAttendees.clear();
        if (members != null) {
            for (CommitteeMember member : members) {
                MeetingAttendee ma = new MeetingAttendee();
                ma.setMeeting(this);
                ma.setMember(member);
                ma.setId(new MeetingAttendeeId(this.id, member.getId()));
                ma.setSnacksAccepted(false);
                this.meetingAttendees.add(ma);
            }
        }
    }

    @Column(name = "resolution_detail", columnDefinition = "LONGTEXT")
    private String resolutionDetail;

    @Column(name = "resolution_4_data", columnDefinition = "LONGTEXT")
    private String resolutionFourData;

    @Column(name = "resolution_5_data", columnDefinition = "LONGTEXT")
    private String resolutionFiveData;

    @Column(columnDefinition = "TEXT")
    private String pdfConfig;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = "DRAFT";
    }
}