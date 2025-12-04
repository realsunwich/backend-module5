package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data; // ใช้ Lombok ลด code getter/setter
import java.time.LocalDateTime;

@Entity
@Table(name = "committee_members")
@Data
public class CommitteeMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Column(name = "prename")
    private String prename;

    @Column(name = "firstname", nullable = false)
    private String firstname;

    @Column(name = "middlename")
    private String middlename;

    @Column(name = "lastname", nullable = false)
    private String lastname;

    @Column(name = "affiliation")
    private String affiliation;

    @Column(name = "department")
    private String department;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}