package com.example.demo.service;

import com.example.demo.dto.ChatContextDTO;
import com.example.demo.entity.Asset;
import com.example.demo.entity.Meeting;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MeetingRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatContextService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.example.demo.repository.CommitteeMemberRepository committeeMemberRepository;

    private static final Map<String, String> MEETING_TYPE_MAP = new HashMap<>();

    static {
        MEETING_TYPE_MAP.put("001", "การประชุมคณะอนุกรรมการ");
        MEETING_TYPE_MAP.put("002", "การประชุมคณะอนุกรรมการตรวจสอบทรัพย์สิน");
        MEETING_TYPE_MAP.put("003", "การประชุมคณะอนุกรรมการตรวจสอบทรัพย์สินมูลค่าสูง");
    }

    private static final Map<String, String> MEETING_STATUS_MAP = new HashMap<>();

    static {
        MEETING_STATUS_MAP.put("DRAFT", "ร่าง");
        MEETING_STATUS_MAP.put("ACTIVE", "รอเริ่มการประชุม");
        MEETING_STATUS_MAP.put("PUBLISH", "เผยแพร่แล้ว");
    }

    public ChatContextDTO getCurrentContext() {
        return getCurrentContext(null);
    }

    public ChatContextDTO getCurrentContext(String userName) {
        ChatContextDTO context = new ChatContextDTO();

        try {
            // ตั้งค่าชื่อผู้ใช้งาน (ในอนาคตอาจดึงจาก Authentication หรือ Session)
            if (userName != null && !userName.isEmpty()) {
                context.setCurrentUserName(userName);
            } else {
                context.setCurrentUserName("ผู้ใช้งาน");
            }

            // ดึงข้อมูลการประชุมทั้งหมด (ไม่กรองสถานะ และไม่จำกัดจำนวน เพื่อให้ AI เห็นข้อมูลครบถ้วน)
            List<Meeting> allMeetings = meetingRepository.findAll();
            List<Meeting> todayMeetings = null;

            if (allMeetings != null && !allMeetings.isEmpty()) {
                // เรียงลำดับตามวันที่และเวลาล่าสุด (ไม่จำกัดจำนวน)
                todayMeetings = allMeetings.stream()
                        .sorted((m1, m2) -> {
                            int dateCompare = m2.getMeetingDate().compareTo(m1.getMeetingDate());
                            if (dateCompare != 0) return dateCompare;
                            if (m1.getMeetingTime() == null) return 1;
                            if (m2.getMeetingTime() == null) return -1;
                            return m2.getMeetingTime().compareTo(m1.getMeetingTime());
                        })
                        .collect(Collectors.toList());
            }

            if (todayMeetings != null && !todayMeetings.isEmpty()) {
                List<ChatContextDTO.MeetingInfo> meetingInfos = todayMeetings.stream()
                        .map(meeting -> {
                            String meetingType = MEETING_TYPE_MAP.getOrDefault(
                                    meeting.getMeetingTypeCode(),
                                    "การประชุม"
                            );
                            String meetingTime = meeting.getMeetingTime() != null
                                    ? meeting.getMeetingTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " น."
                                    : "ไม่ระบุเวลา";
                            String meetingDate = meeting.getMeetingDate() != null
                                    ? meeting.getMeetingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    : "ไม่ระบุวันที่";
                            String status = MEETING_STATUS_MAP.getOrDefault(
                                    meeting.getStatus(),
                                    meeting.getStatus()
                            );

                            return new ChatContextDTO.MeetingInfo(
                                    meeting.getId(),
                                    meeting.getMeetingNo(),
                                    meetingType,
                                    meetingTime,
                                    meetingDate,
                                    status,
                                    meeting.getDescription()
                            );
                        })
                        .collect(Collectors.toList());

                context.setTodayMeetings(meetingInfos);
            }

            // นับจำนวนเอกสารรออนุมัติ (ในที่นี้ใช้ Meeting ที่เป็น DRAFT)
            long pendingDocuments = meetingRepository.countByStatus("DRAFT");
            context.setPendingDocumentsCount(pendingDocuments);

            // ดึงข้อมูลทรัพย์สินทั้งหมด
            List<Asset> allAssets = assetRepository.findAll();
            if (allAssets != null && !allAssets.isEmpty()) {
                List<ChatContextDTO.AssetInfo> assetInfos = allAssets.stream()
                        .map(asset -> {
                            String statusText = asset.getStatus() != null ? asset.getStatus().name() : "UNKNOWN";
                            return new ChatContextDTO.AssetInfo(
                                    asset.getId(),
                                    asset.getName(),
                                    asset.getAssetType(),
                                    asset.getQuantity(),
                                    statusText
                            );
                        })
                        .collect(Collectors.toList());
                context.setAssetsList(assetInfos);
            }

            // นับจำนวนทรัพย์สินรอตรวจสอบ
            long pendingAssets = assetRepository.countByStatus(Asset.AssetStatus.PENDING);
            context.setPendingAssetsCount(pendingAssets);

            // นับจำนวนการแจ้งเตือนที่ยังไม่อ่าน
            List<com.example.demo.entity.Notification> unreadNotifications =
                notificationRepository.findByIsReadFalseOrderByTimestampDesc();
            context.setUnreadNotificationsCount(unreadNotifications != null ? unreadNotifications.size() : 0);

            // ดึงข้อมูลคณะกรรมการทั้งหมด
            List<com.example.demo.entity.CommitteeMember> allCommitteeMembers = committeeMemberRepository.findAll();
            if (allCommitteeMembers != null && !allCommitteeMembers.isEmpty()) {
                List<ChatContextDTO.CommitteeMemberInfo> memberInfos = allCommitteeMembers.stream()
                        .map(member -> {
                            String fullName = (member.getPrename() != null ? member.getPrename() + " " : "") +
                                    member.getFirstname() + " " + member.getLastname();
                            return new ChatContextDTO.CommitteeMemberInfo(
                                    member.getId(),
                                    member.getCitizenId(),
                                    fullName,
                                    member.getAffiliation(),
                                    member.getDepartment(),
                                    member.getPhone(),
                                    member.getEmail()
                            );
                        })
                        .collect(Collectors.toList());
                context.setCommitteeMembers(memberInfos);
            }

        } catch (Exception e) {
            // Log error and return empty context
            System.err.println("Error getting chat context: " + e.getMessage());
            e.printStackTrace();
        }

        return context;
    }

    public String buildDatabaseContext() {
        return buildDatabaseContext(null);
    }

    public String buildDatabaseContext(String userName) {
        ChatContextDTO context = getCurrentContext(userName);
        String contextString = context.buildContextString();

        // Log เพื่อ debug
        System.out.println("=== DATABASE CONTEXT ===");
        System.out.println(contextString);
        System.out.println("========================");

        return contextString;
    }
}
