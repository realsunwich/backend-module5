package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

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

    @ManyToMany
    @JoinTable(name = "meeting_attendees", joinColumns = @JoinColumn(name = "meeting_id"), inverseJoinColumns = @JoinColumn(name = "member_id"))
    private List<CommitteeMember> attendees;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = "DRAFT";
    }
}