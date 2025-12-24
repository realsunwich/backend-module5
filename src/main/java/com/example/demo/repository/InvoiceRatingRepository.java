package com.example.demo.repository;

import com.example.demo.entity.InvoiceRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRatingRepository extends JpaRepository<InvoiceRating, Long> {

    // ค้นหา rating ด้วย invoice number
    Optional<InvoiceRating> findByInvoiceNumber(String invoiceNumber);

    // ค้นหา rating ทั้งหมดของผู้ใช้
    List<InvoiceRating> findByUserName(String userName);

    // ค้นหา rating ที่มีคะแนนตามที่ระบุ
    List<InvoiceRating> findByUserRating(Integer rating);

    // ตรวจสอบว่ามี rating สำหรับ invoice number นี้แล้วหรือยัง
    boolean existsByInvoiceNumber(String invoiceNumber);

    // หาค่าเฉลี่ย rating
    @Query("SELECT AVG(r.userRating) FROM InvoiceRating r")
    Double getAverageRating();

    // หาค่าเฉลี่ย confidence score
    @Query("SELECT AVG(r.originalConfidence) FROM InvoiceRating r")
    Double getAverageConfidence();

    // นับจำนวน rating แต่ละดาว
    long countByUserRating(Integer rating);

    // ดึง rating ล่าสุด
    List<InvoiceRating> findTop10ByOrderByCreatedAtDesc();
}
