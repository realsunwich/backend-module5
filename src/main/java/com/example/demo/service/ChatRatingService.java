package com.example.demo.service;

import com.example.demo.dto.ChatFeedbackRequest;
import com.example.demo.dto.ChatFeedbackResponse;
import com.example.demo.entity.ChatRating;
import com.example.demo.repository.ChatRatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatRatingService {

    @Autowired
    private ChatRatingRepository chatRatingRepository;

    /**
     * บันทึกหรืออัพเดทการให้คะแนน
     */
    @Transactional
    public ChatFeedbackResponse saveFeedback(ChatFeedbackRequest request) {
        try {
            // ตรวจสอบว่า logId ถูกส่งมาหรือไม่
            if (request.getLogId() == null || request.getLogId().trim().isEmpty()) {
                return ChatFeedbackResponse.error("Log ID is required");
            }

            // ตรวจสอบว่า rating ถูกส่งมาหรือไม่
            if (request.getRating() == null || request.getRating().trim().isEmpty()) {
                return ChatFeedbackResponse.error("Rating is required");
            }

            // แปลง String เป็น Enum
            ChatRating.RatingType ratingType;
            try {
                ratingType = ChatRating.RatingType.valueOf(request.getRating().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ChatFeedbackResponse.error("Invalid rating type. Must be LIKE or DISLIKE");
            }

            // ตรวจสอบว่ามีการให้คะแนนสำหรับ logId นี้แล้วหรือไม่
            Optional<ChatRating> existingRating = chatRatingRepository.findByLogId(request.getLogId());

            ChatRating chatRating;
            if (existingRating.isPresent()) {
                // อัพเดทการให้คะแนนที่มีอยู่
                chatRating = existingRating.get();
                chatRating.setRating(ratingType);
                chatRating.setComment(request.getComment());

                // อัพเดท context ถ้ามีส่งมา
                if (request.getUserMessage() != null) {
                    chatRating.setUserMessage(request.getUserMessage());
                }
                if (request.getBotResponse() != null) {
                    chatRating.setBotResponse(request.getBotResponse());
                }
                if (request.getUserName() != null) {
                    chatRating.setUserName(request.getUserName());
                }
            } else {
                // สร้างการให้คะแนนใหม่
                chatRating = new ChatRating(
                        request.getLogId(),
                        request.getUserMessage(),
                        request.getBotResponse(),
                        ratingType,
                        request.getComment(),
                        request.getUserName()
                );
            }

            // บันทึกลงฐานข้อมูล
            ChatRating savedRating = chatRatingRepository.save(chatRating);

            return ChatFeedbackResponse.success(
                    "Feedback saved successfully",
                    savedRating.getId()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ChatFeedbackResponse.error("Error saving feedback: " + e.getMessage());
        }
    }

    /**
     * ดึงการให้คะแนนทั้งหมด
     */
    public List<ChatRating> getAllRatings() {
        return chatRatingRepository.findAll();
    }

    /**
     * ดึงการให้คะแนนตาม logId
     */
    public Optional<ChatRating> getRatingByLogId(String logId) {
        return chatRatingRepository.findByLogId(logId);
    }

    /**
     * ดึงการให้คะแนนตามประเภท
     */
    public List<ChatRating> getRatingsByType(ChatRating.RatingType ratingType) {
        return chatRatingRepository.findByRating(ratingType);
    }

    /**
     * ดึงการให้คะแนนตามผู้ใช้
     */
    public List<ChatRating> getRatingsByUserName(String userName) {
        return chatRatingRepository.findByUserName(userName);
    }

    /**
     * นับจำนวน LIKE
     */
    public long countLikes() {
        return chatRatingRepository.countByRating(ChatRating.RatingType.LIKE);
    }

    /**
     * นับจำนวน DISLIKE
     */
    public long countDislikes() {
        return chatRatingRepository.countByRating(ChatRating.RatingType.DISLIKE);
    }

    /**
     * ลบการให้คะแนนตาม logId
     */
    @Transactional
    public boolean deleteRatingByLogId(String logId) {
        Optional<ChatRating> rating = chatRatingRepository.findByLogId(logId);
        if (rating.isPresent()) {
            chatRatingRepository.delete(rating.get());
            return true;
        }
        return false;
    }

    /**
     * ดึงสถิติการให้คะแนน
     */
    public String getRatingStatistics() {
        long likes = countLikes();
        long dislikes = countDislikes();
        long total = likes + dislikes;

        if (total == 0) {
            return "ยังไม่มีการให้คะแนน";
        }

        double likePercentage = (likes * 100.0) / total;
        double dislikePercentage = (dislikes * 100.0) / total;

        return String.format(
                "สถิติการให้คะแนน:\n👍 LIKE: %d (%.1f%%)\n👎 DISLIKE: %d (%.1f%%)\nรวม: %d",
                likes, likePercentage, dislikes, dislikePercentage, total
        );
    }
}
