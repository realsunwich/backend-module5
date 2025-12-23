package com.example.demo.service;

import com.example.demo.dto.ChatContextDTO;
import com.example.demo.entity.Asset;
import com.example.demo.entity.Meeting;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MeetingRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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

    @Autowired
    private IntentDetectionService intentDetectionService;

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

    /**
     * ฟังก์ชันหลักสำหรับสร้าง Context แบบ Intent-Based (ใหม่)
     */
    public String buildDatabaseContextByIntent(String userMessage, String userName) {
        // 1. ตรวจจับ Intent
        IntentDetectionService.IntentType intent = intentDetectionService.detectIntent(userMessage);

        // 2. ดึง Parameters เพิ่มเติม (เช่น รหัสการประชุม, วันที่)
        Map<String, String> params = intentDetectionService.extractParameters(userMessage);

        // 3. Log สำหรับ Debug
        System.out.println("=== INTENT DETECTION ===");
        System.out.println("User Message: " + userMessage);
        System.out.println("Detected Intent: " + intent);
        System.out.println("Description: " + intentDetectionService.getIntentDescription(intent));
        System.out.println("Parameters: " + params);
        System.out.println("========================");

        // 4. Query ข้อมูลตาม Intent
        String context = buildContextByIntent(intent, params, userName);

        // 5. Log ผลลัพธ์
        System.out.println("=== CONTEXT RESULT ===");
        System.out.println(context);
        System.out.println("======================");

        return context;
    }

    /**
     * สร้าง Context ตาม Intent ที่ตรวจจับได้
     */
    private String buildContextByIntent(
            IntentDetectionService.IntentType intent,
            Map<String, String> params,
            String userName
    ) {
        return switch (intent) {
            case GREETING -> buildGreetingContext(userName);
            case DAILY_BRIEF -> buildDailyBriefContext();
            case MEETING_QUERY -> buildMeetingQueryContext(params);
            case MEETING_DETAIL -> buildMeetingDetailContext(params);
            case ASSET_QUERY -> buildAssetQueryContext(params);
            case ASSET_COUNT -> buildAssetCountContext(params);
            case COMMITTEE_QUERY -> buildCommitteeQueryContext();
            case DOCUMENT_QUERY -> buildDocumentQueryContext();
            case COUNT_QUERY -> buildCountQueryContext();
            case SUMMARY_QUERY -> buildSummaryQueryContext();
            case GENERAL -> buildMinimalContext();
        };
    }

    // === Context Builders สำหรับแต่ละ Intent ===

    /**
     * GREETING - ไม่ต้อง Query อะไรเลย
     */
    private String buildGreetingContext(String userName) {
        return String.format("ผู้ใช้งาน: %s\nไม่มีข้อมูลเพิ่มเติม (คำทักทาย)", userName != null ? userName : "ผู้ใช้งาน");
    }

    /**
     * DAILY_BRIEF - สรุปวันนี้แบบสั้น (Query Count อย่างเดียว - ประหยัด Token สูงสุด)
     */
    private String buildDailyBriefContext() {
        StringBuilder sb = new StringBuilder();

        // Query แค่จำนวน (COUNT) - ไม่ดึงรายละเอียด
        long todayMeetingCount = meetingRepository.findByMeetingDate(LocalDate.now()).size();
        long pendingAssetCount = assetRepository.countByStatus(Asset.AssetStatus.PENDING);
        long pendingDocCount = meetingRepository.countByStatus("DRAFT");

        sb.append("สรุปวันนี้:\n");
        sb.append(String.format("- การประชุมวันนี้: %d รายการ\n", todayMeetingCount));
        sb.append(String.format("- ทรัพย์สินรอตรวจสอบ: %d รายการ\n", pendingAssetCount));
        sb.append(String.format("- เอกสารรออนุมัติ: %d รายการ\n", pendingDocCount));

        return sb.toString();
    }

    /**
     * MEETING_QUERY - Query เฉพาะการประชุม
     */
    private String buildMeetingQueryContext(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        // ดึงข้อมูลการประชุมตาม DateFilter
        List<Meeting> meetings;
        String dateFilter = params.getOrDefault("dateFilter", "recent");
        String year = params.get("year");

        // ถ้ามีการระบุปี ให้กรองตามปี
        if (year != null) {
            LocalDate startDate, endDate;

            if ("thisYear".equals(year)) {
                // ปีนี้
                startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
                endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfYear());
                sb.append("การประชุมปีนี้:\n");
            } else {
                // ปีที่ระบุ (เช่น 2568)
                int buddhistYear = Integer.parseInt(year);
                int gregorianYear = buddhistYear - 543; // แปลง พ.ศ. เป็น ค.ศ.
                startDate = LocalDate.of(gregorianYear, 1, 1);
                endDate = LocalDate.of(gregorianYear, 12, 31);
                sb.append(String.format("การประชุมภายในปี %s:\n", year));
            }

            meetings = meetingRepository.findMeetingsBetweenDates(startDate, endDate);
        } else if ("today".equals(dateFilter)) {
            meetings = meetingRepository.findByMeetingDate(LocalDate.now());
            sb.append("การประชุมวันนี้:\n");
        } else if ("thisWeek".equals(dateFilter)) {
            LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            meetings = meetingRepository.findMeetingsBetweenDates(startOfWeek, endOfWeek);
            sb.append("การประชุมสัปดาห์นี้:\n");
        } else if ("thisMonth".equals(dateFilter)) {
            LocalDate startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
            meetings = meetingRepository.findMeetingsBetweenDates(startOfMonth, endOfMonth);
            sb.append("การประชุมเดือนนี้:\n");
        } else {
            // ดึง 10 รายการล่าสุด
            meetings = meetingRepository.findTop10RecentMeetings(PageRequest.of(0, 10));
            sb.append("การประชุมล่าสุด (10 รายการ):\n");
        }

        if (meetings.isEmpty()) {
            sb.append("- ไม่มีการประชุม\n");
        } else {
            sb.append(String.format("- มีทั้งหมด %d รายการ\n", meetings.size()));

            // ถ้าถามแบบระบุปี หรือ "ทั้งหมด" ให้แสดงทั้งหมด (จำกัดไว้ที่ 50 เพื่อป้องกัน Token overflow)
            boolean showAll = (year != null || params.containsKey("showAll"));
            int limit = showAll ? Math.min(50, meetings.size()) : Math.min(3, meetings.size());

            for (int i = 0; i < limit; i++) {
                sb.append(formatMeetingInfo(meetings.get(i))).append("\n");
            }

            // ถ้ายังมีเหลือ บอกว่ามีอีก
            if (meetings.size() > limit) {
                sb.append(String.format("... และอีก %d รายการ (ถ้าต้องการรายละเอียด กรุณาถามเพิ่มเติม)\n",
                        meetings.size() - limit));
            }
        }

        return sb.toString();
    }

    /**
     * MEETING_DETAIL - Query การประชุมเฉพาะตามเลขที่
     */
    private String buildMeetingDetailContext(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        String meetingNo = params.get("meetingNo");

        if (meetingNo != null) {
            Meeting meeting = meetingRepository.findByMeetingNo(meetingNo);
            if (meeting != null) {
                sb.append("รายละเอียดการประชุม:\n");
                sb.append(formatMeetingDetailInfo(meeting));
            } else {
                sb.append(String.format("ไม่พบการประชุมเลขที่ %s\n", meetingNo));
            }
        } else {
            // ถ้าไม่มีเลขที่ ให้แสดงการประชุมล่าสุด
            List<Meeting> meetings = meetingRepository.findTop10RecentMeetings(PageRequest.of(0, 5));
            sb.append("การประชุมล่าสุด (5 รายการ):\n");
            if (meetings.isEmpty()) {
                sb.append("- ไม่มีการประชุม\n");
            } else {
                for (Meeting m : meetings) {
                    sb.append(formatMeetingInfo(m)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * ASSET_QUERY - Query เฉพาะทรัพย์สิน
     */
    private String buildAssetQueryContext(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        String assetType = params.get("assetType");

        List<Asset> assets;
        if (assetType != null) {
            assets = assetRepository.findByAssetType(assetType);
            sb.append(String.format("ทรัพย์สินประเภท '%s':\n", assetType));
        } else {
            // ดึงทรัพย์สินที่รอตรวจสอบ
            assets = assetRepository.findByStatus(Asset.AssetStatus.PENDING);
            sb.append("ทรัพย์สินรอตรวจสอบ:\n");
        }

        if (assets.isEmpty()) {
            sb.append("- ไม่มีทรัพย์สิน\n");
        } else {
            sb.append(String.format("- มีทั้งหมด %d รายการ\n", assets.size()));
            // แสดงแค่ 20 รายการแรก
            assets.stream().limit(20).forEach(asset -> {
                sb.append(formatAssetInfo(asset)).append("\n");
            });
            if (assets.size() > 20) {
                sb.append(String.format("... และอีก %d รายการ\n", assets.size() - 20));
            }
        }

        return sb.toString();
    }

    /**
     * ASSET_COUNT - Query แค่จำนวนทรัพย์สิน
     */
    private String buildAssetCountContext(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        String assetType = params.get("assetType");

        if (assetType != null) {
            long count = assetRepository.countByAssetType(assetType);
            sb.append(String.format("จำนวนทรัพย์สินประเภท '%s': %d รายการ\n", assetType, count));
        } else {
            long pendingCount = assetRepository.countByStatus(Asset.AssetStatus.PENDING);
            long confirmedCount = assetRepository.countByStatus(Asset.AssetStatus.CONFIRMED);
            long checkedInCount = assetRepository.countByStatus(Asset.AssetStatus.CHECKED_IN);
            long totalCount = assetRepository.count();
            sb.append("สรุปจำนวนทรัพย์สิน:\n");
            sb.append(String.format("- รอตรวจสอบ: %d รายการ\n", pendingCount));
            sb.append(String.format("- พบแล้ว: %d รายการ\n", confirmedCount));
            sb.append(String.format("- เช็คอินแล้ว: %d รายการ\n", checkedInCount));
            sb.append(String.format("- ทั้งหมด: %d รายการ\n", totalCount));
        }

        return sb.toString();
    }

    /**
     * COMMITTEE_QUERY - Query เฉพาะคณะกรรมการ
     */
    private String buildCommitteeQueryContext() {
        StringBuilder sb = new StringBuilder();
        List<com.example.demo.entity.CommitteeMember> members = committeeMemberRepository.findAll();

        sb.append("คณะกรรมการ:\n");
        if (members.isEmpty()) {
            sb.append("- ไม่มีข้อมูล\n");
        } else {
            sb.append(String.format("- มีทั้งหมด %d คน\n", members.size()));
            members.forEach(member -> {
                String fullName = (member.getPrename() != null ? member.getPrename() + " " : "") +
                        member.getFirstname() + " " + member.getLastname();
                sb.append(String.format("  - %s (%s)\n", fullName, member.getAffiliation()));
            });
        }

        return sb.toString();
    }

    /**
     * DOCUMENT_QUERY - Query เฉพาะเอกสาร
     */
    private String buildDocumentQueryContext() {
        StringBuilder sb = new StringBuilder();
        long draftCount = meetingRepository.countByStatus("DRAFT");

        sb.append("สรุปเอกสาร:\n");
        sb.append(String.format("- เอกสารรออนุมัติ (ร่าง): %d รายการ\n", draftCount));

        return sb.toString();
    }

    /**
     * COUNT_QUERY - Query แบบนับทั้งหมด
     */
    private String buildCountQueryContext() {
        StringBuilder sb = new StringBuilder();

        long meetingCount = meetingRepository.countActiveMeetings();
        long assetPendingCount = assetRepository.countByStatus(Asset.AssetStatus.PENDING);
        long documentDraftCount = meetingRepository.countByStatus("DRAFT");
        long committeeCount = committeeMemberRepository.count();

        sb.append("สรุปจำนวนข้อมูลทั้งหมด:\n");
        sb.append(String.format("- การประชุม: %d รายการ\n", meetingCount));
        sb.append(String.format("- ทรัพย์สินรอตรวจสอบ: %d รายการ\n", assetPendingCount));
        sb.append(String.format("- เอกสารรออนุมัติ: %d รายการ\n", documentDraftCount));
        sb.append(String.format("- คณะกรรมการ: %d คน\n", committeeCount));

        return sb.toString();
    }

    /**
     * SUMMARY_QUERY - Query แบบสรุปทั้งหมด
     */
    private String buildSummaryQueryContext() {
        StringBuilder sb = new StringBuilder();

        // Count Summary
        sb.append(buildCountQueryContext());

        // เพิ่มข้อมูลสำคัญบางส่วน
        sb.append("\nการประชุมวันนี้:\n");
        List<Meeting> todayMeetings = meetingRepository.findByMeetingDate(LocalDate.now());
        if (todayMeetings.isEmpty()) {
            sb.append("- ไม่มีการประชุม\n");
        } else {
            todayMeetings.forEach(m -> sb.append(formatMeetingInfo(m)).append("\n"));
        }

        return sb.toString();
    }

    /**
     * GENERAL - Query ข้อมูลพื้นฐาน (ลดลงจากเดิม)
     */
    private String buildMinimalContext() {
        return buildCountQueryContext();
    }

    // === Helper Methods สำหรับ Format ข้อมูล ===

    private String formatMeetingInfo(Meeting meeting) {
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

        return String.format("  - %s เลขที่ %s วันที่ %s เวลา %s (สถานะ: %s)",
                meetingType, meeting.getMeetingNo(), meetingDate, meetingTime, status);
    }

    private String formatMeetingDetailInfo(Meeting meeting) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatMeetingInfo(meeting)).append("\n");
        if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
            sb.append(String.format("  รายละเอียด: %s\n", meeting.getDescription()));
        }
        return sb.toString();
    }

    private String formatAssetInfo(Asset asset) {
        return String.format("  - %s (ประเภท: %s, จำนวน: %d, สถานะ: %s)",
                asset.getName(),
                asset.getAssetType(),
                asset.getQuantity(),
                asset.getStatus().name());
    }

    // === ฟังก์ชันเก่า (เก็บไว้เผื่อใช้งาน Legacy) ===

    public ChatContextDTO getCurrentContext() {
        return getCurrentContext(null);
    }

    public ChatContextDTO getCurrentContext(String userName) {
        ChatContextDTO context = new ChatContextDTO();

        try {
            if (userName != null && !userName.isEmpty()) {
                context.setCurrentUserName(userName);
            } else {
                context.setCurrentUserName("ผู้ใช้งาน");
            }

            List<Meeting> allMeetings = meetingRepository.findAll();
            List<Meeting> todayMeetings = null;

            if (allMeetings != null && !allMeetings.isEmpty()) {
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

            long pendingDocuments = meetingRepository.countByStatus("DRAFT");
            context.setPendingDocumentsCount(pendingDocuments);

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

            long pendingAssets = assetRepository.countByStatus(Asset.AssetStatus.PENDING);
            context.setPendingAssetsCount(pendingAssets);

            List<com.example.demo.entity.Notification> unreadNotifications =
                notificationRepository.findByIsReadFalseOrderByTimestampDesc();
            context.setUnreadNotificationsCount(unreadNotifications != null ? unreadNotifications.size() : 0);

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
            System.err.println("Error getting chat context: " + e.getMessage());
            e.printStackTrace();
        }

        return context;
    }

    @Deprecated
    public String buildDatabaseContext() {
        return buildDatabaseContext(null);
    }

    @Deprecated
    public String buildDatabaseContext(String userName) {
        ChatContextDTO context = getCurrentContext(userName);
        String contextString = context.buildContextString();

        System.out.println("=== DATABASE CONTEXT (OLD METHOD) ===");
        System.out.println(contextString);
        System.out.println("=====================================");

        return contextString;
    }
}
