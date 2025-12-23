package com.example.demo.repository;

import com.example.demo.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Meeting findTopByMeetingNoStartingWithOrderByIdDesc(String prefix);

    List<Meeting> findByMeetingDate(LocalDate meetingDate);

    List<Meeting> findByMeetingDateAndStatusNot(LocalDate meetingDate, String status);

    @Query("SELECT m FROM Meeting m WHERE m.meetingDate = :date AND m.status != 'DRAFT' ORDER BY m.meetingTime ASC")
    List<Meeting> findTodayActiveMeetings(@org.springframework.data.repository.query.Param("date") LocalDate date);

    @Query("SELECT m FROM Meeting m WHERE m.status != 'DRAFT' ORDER BY m.meetingDate DESC, m.meetingTime DESC")
    List<Meeting> findRecentActiveMeetings();

    List<Meeting> findByStatus(String status);

    long countByStatus(String status);

    // Query Methods สำหรับ Intent-Based System

    // ดึงการประชุมล่าสุด (จำกัดจำนวน)
    @Query("SELECT m FROM Meeting m WHERE m.status != 'DRAFT' ORDER BY m.meetingDate DESC, m.meetingTime DESC")
    List<Meeting> findTop10RecentMeetings(org.springframework.data.domain.Pageable pageable);

    // ดึงการประชุมตามช่วงวันที่
    @Query("SELECT m FROM Meeting m WHERE m.meetingDate BETWEEN :startDate AND :endDate AND m.status != 'DRAFT' ORDER BY m.meetingDate DESC, m.meetingTime DESC")
    List<Meeting> findMeetingsBetweenDates(
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate
    );

    // ค้นหาการประชุมตามเลขที่
    Meeting findByMeetingNo(String meetingNo);

    // นับการประชุมทั้งหมด (ไม่รวม DRAFT)
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.status != 'DRAFT'")
    long countActiveMeetings();
}