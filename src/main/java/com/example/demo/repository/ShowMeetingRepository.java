package com.example.demo.repository;

import com.example.demo.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowMeetingRepository extends JpaRepository<Meeting, Long> {
}
