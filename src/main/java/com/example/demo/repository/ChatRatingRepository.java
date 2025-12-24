package com.example.demo.repository;

import com.example.demo.entity.ChatRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRatingRepository extends JpaRepository<ChatRating, Long> {

    // ค้นหา rating ด้วย logId
    Optional<ChatRating> findByLogId(String logId);

    // ค้นหา rating ทั้งหมดของผู้ใช้
    List<ChatRating> findByUserName(String userName);

    // ค้นหา rating ตามประเภท (LIKE หรือ DISLIKE)
    List<ChatRating> findByRating(ChatRating.RatingType rating);

    // นับจำนวน rating แต่ละประเภท
    long countByRating(ChatRating.RatingType rating);

    // ตรวจสอบว่ามี rating สำหรับ logId นี้แล้วหรือยัง
    boolean existsByLogId(String logId);
}
