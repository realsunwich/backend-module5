package com.example.demo.service;

import com.example.demo.dto.InvoiceRatingRequest;
import com.example.demo.dto.InvoiceRatingResponse;
import com.example.demo.entity.InvoiceRating;
import com.example.demo.repository.InvoiceRatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InvoiceRatingService {

    @Autowired
    private InvoiceRatingRepository invoiceRatingRepository;

    /**
     * บันทึกหรืออัพเดทการให้คะแนน
     */
    @Transactional
    public InvoiceRatingResponse saveRating(InvoiceRatingRequest request) {
        try {
            // Validation
            if (request.getInvoiceData() == null || request.getInvoiceData().trim().isEmpty()) {
                return InvoiceRatingResponse.error("Invoice data is required");
            }

            if (request.getUserRating() == null || request.getUserRating() < 1 || request.getUserRating() > 5) {
                return InvoiceRatingResponse.error("User rating must be between 1 and 5");
            }

            // ตรวจสอบว่ามีการให้คะแนนสำหรับ invoice number นี้แล้วหรือไม่ (ถ้ามี invoice number)
            InvoiceRating invoiceRating;

            if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().trim().isEmpty()) {
                Optional<InvoiceRating> existingRating = invoiceRatingRepository.findByInvoiceNumber(request.getInvoiceNumber());

                if (existingRating.isPresent()) {
                    // อัพเดทการให้คะแนนที่มีอยู่
                    invoiceRating = existingRating.get();
                    invoiceRating.setInvoiceData(request.getInvoiceData());
                    invoiceRating.setOriginalConfidence(request.getOriginalConfidence());
                    invoiceRating.setUserRating(request.getUserRating());
                    invoiceRating.setUserFeedback(request.getUserFeedback());

                    if (request.getUserName() != null) {
                        invoiceRating.setUserName(request.getUserName());
                    }
                } else {
                    // สร้างการให้คะแนนใหม่
                    invoiceRating = createNewRating(request);
                }
            } else {
                // ไม่มี invoice number ก็สร้างใหม่เลย
                invoiceRating = createNewRating(request);
            }

            // บันทึกลงฐานข้อมูล
            InvoiceRating savedRating = invoiceRatingRepository.save(invoiceRating);

            return InvoiceRatingResponse.success(
                    "Rating saved successfully",
                    savedRating.getId()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return InvoiceRatingResponse.error("Error saving rating: " + e.getMessage());
        }
    }

    /**
     * สร้าง InvoiceRating object ใหม่จาก request
     */
    private InvoiceRating createNewRating(InvoiceRatingRequest request) {
        return new InvoiceRating(
                request.getInvoiceNumber(),
                request.getInvoiceData(),
                request.getOriginalConfidence(),
                request.getUserRating(),
                request.getUserFeedback(),
                request.getUserName()
        );
    }

    /**
     * ดึงการให้คะแนนทั้งหมด
     */
    public List<InvoiceRating> getAllRatings() {
        return invoiceRatingRepository.findAll();
    }

    /**
     * ดึงการให้คะแนนตาม invoice number
     */
    public Optional<InvoiceRating> getRatingByInvoiceNumber(String invoiceNumber) {
        return invoiceRatingRepository.findByInvoiceNumber(invoiceNumber);
    }

    /**
     * ดึงการให้คะแนนตามคะแนน
     */
    public List<InvoiceRating> getRatingsByScore(Integer rating) {
        return invoiceRatingRepository.findByUserRating(rating);
    }

    /**
     * ดึงการให้คะแนนตามผู้ใช้
     */
    public List<InvoiceRating> getRatingsByUserName(String userName) {
        return invoiceRatingRepository.findByUserName(userName);
    }

    /**
     * ดึงการให้คะแนนล่าสุด 10 รายการ
     */
    public List<InvoiceRating> getRecentRatings() {
        return invoiceRatingRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * ลบการให้คะแนนตาม invoice number
     */
    @Transactional
    public boolean deleteRatingByInvoiceNumber(String invoiceNumber) {
        Optional<InvoiceRating> rating = invoiceRatingRepository.findByInvoiceNumber(invoiceNumber);
        if (rating.isPresent()) {
            invoiceRatingRepository.delete(rating.get());
            return true;
        }
        return false;
    }

    /**
     * ดึงสถิติการให้คะแนน
     */
    public Map<String, Object> getRatingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // นับจำนวนแต่ละดาว
        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            starCounts.put(i, invoiceRatingRepository.countByUserRating(i));
        }
        stats.put("star_distribution", starCounts);

        // คำนวณค่าเฉลี่ย rating
        Double avgRating = invoiceRatingRepository.getAverageRating();
        stats.put("average_rating", avgRating != null ? avgRating : 0.0);

        // คำนวณค่าเฉลี่ย confidence
        Double avgConfidence = invoiceRatingRepository.getAverageConfidence();
        stats.put("average_confidence", avgConfidence != null ? avgConfidence : 0.0);

        // นับจำนวนทั้งหมด
        long totalRatings = invoiceRatingRepository.count();
        stats.put("total_ratings", totalRatings);

        return stats;
    }

    /**
     * ดึงสถิติเป็น String สำหรับแสดงผล
     */
    public String getRatingStatisticsText() {
        Map<String, Object> stats = getRatingStatistics();

        long total = (Long) stats.get("total_ratings");
        if (total == 0) {
            return "ยังไม่มีการให้คะแนน";
        }

        Double avgRating = (Double) stats.get("average_rating");
        Double avgConfidence = (Double) stats.get("average_confidence");

        @SuppressWarnings("unchecked")
        Map<Integer, Long> starDist = (Map<Integer, Long>) stats.get("star_distribution");

        StringBuilder sb = new StringBuilder();
        sb.append("สถิติการให้คะแนน Invoice AI:\n");
        sb.append(String.format("คะแนนเฉลี่ย: %.2f ⭐\n", avgRating));
        sb.append(String.format("Confidence เฉลี่ย: %.1f%%\n", avgConfidence * 100));
        sb.append("\nการกระจายของคะแนน:\n");

        for (int i = 5; i >= 1; i--) {
            long count = starDist.getOrDefault(i, 0L);
            double percentage = (count * 100.0) / total;
            sb.append(String.format("%d ⭐: %d (%.1f%%)\n", i, count, percentage));
        }

        sb.append(String.format("\nรวมทั้งหมด: %d รายการ", total));

        return sb.toString();
    }
}
