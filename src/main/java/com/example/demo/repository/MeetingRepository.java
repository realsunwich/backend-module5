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
}