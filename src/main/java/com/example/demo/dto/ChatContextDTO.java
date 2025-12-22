package com.example.demo.dto;

import java.util.List;

public class ChatContextDTO {
    private String currentUserName;
    private List<MeetingInfo> todayMeetings;
    private long pendingDocumentsCount;
    private long pendingAssetsCount;
    private long unreadNotificationsCount;
    private List<AssetInfo> assetsList;
    private List<CommitteeMemberInfo> committeeMembers;

    public static class CommitteeMemberInfo {
        private Long id;
        private String citizenId;
        private String fullName;
        private String affiliation;
        private String department;
        private String phone;
        private String email;

        public CommitteeMemberInfo(Long id, String citizenId, String fullName, String affiliation, String department, String phone, String email) {
            this.id = id;
            this.citizenId = citizenId;
            this.fullName = fullName;
            this.affiliation = affiliation;
            this.department = department;
            this.phone = phone;
            this.email = email;
        }

        public Long getId() { return id; }
        public String getCitizenId() { return citizenId; }
        public String getFullName() { return fullName; }
        public String getAffiliation() { return affiliation; }
        public String getDepartment() { return department; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
    }

    public static class AssetInfo {
        private Long id;
        private String name;
        private String assetType;
        private Integer quantity;
        private String status;

        public AssetInfo(Long id, String name, String assetType, Integer quantity, String status) {
            this.id = id;
            this.name = name;
            this.assetType = assetType;
            this.quantity = quantity;
            this.status = status;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getAssetType() { return assetType; }
        public Integer getQuantity() { return quantity; }
        public String getStatus() { return status; }
    }

    public static class MeetingInfo {
        private Long id;
        private String meetingNo;
        private String meetingType;
        private String meetingTime;
        private String meetingDate;
        private String status;
        private String description;

        public MeetingInfo(Long id, String meetingNo, String meetingType, String meetingTime, String meetingDate, String status, String description) {
            this.id = id;
            this.meetingNo = meetingNo;
            this.meetingType = meetingType;
            this.meetingTime = meetingTime;
            this.meetingDate = meetingDate;
            this.status = status;
            this.description = description;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMeetingNo() {
            return meetingNo;
        }

        public void setMeetingNo(String meetingNo) {
            this.meetingNo = meetingNo;
        }

        public String getMeetingType() {
            return meetingType;
        }

        public void setMeetingType(String meetingType) {
            this.meetingType = meetingType;
        }

        public String getMeetingTime() {
            return meetingTime;
        }

        public void setMeetingTime(String meetingTime) {
            this.meetingTime = meetingTime;
        }

        public String getMeetingDate() {
            return meetingDate;
        }

        public void setMeetingDate(String meetingDate) {
            this.meetingDate = meetingDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public List<MeetingInfo> getTodayMeetings() {
        return todayMeetings;
    }

    public void setTodayMeetings(List<MeetingInfo> todayMeetings) {
        this.todayMeetings = todayMeetings;
    }

    public long getPendingDocumentsCount() {
        return pendingDocumentsCount;
    }

    public void setPendingDocumentsCount(long pendingDocumentsCount) {
        this.pendingDocumentsCount = pendingDocumentsCount;
    }

    public long getPendingAssetsCount() {
        return pendingAssetsCount;
    }

    public void setPendingAssetsCount(long pendingAssetsCount) {
        this.pendingAssetsCount = pendingAssetsCount;
    }

    public long getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(long unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public List<AssetInfo> getAssetsList() {
        return assetsList;
    }

    public void setAssetsList(List<AssetInfo> assetsList) {
        this.assetsList = assetsList;
    }

    public List<CommitteeMemberInfo> getCommitteeMembers() {
        return committeeMembers;
    }

    public void setCommitteeMembers(List<CommitteeMemberInfo> committeeMembers) {
        this.committeeMembers = committeeMembers;
    }

    public String buildContextString() {
        StringBuilder context = new StringBuilder();
        context.append("[ข้อมูลล่าสุดในระบบ]:\n");

        if (currentUserName != null && !currentUserName.isEmpty()) {
            context.append("- ผู้ใช้งานปัจจุบัน: ").append(currentUserName).append("\n");
        }

        if (todayMeetings != null && !todayMeetings.isEmpty()) {
            context.append("- การประชุมที่มีในระบบ (").append(todayMeetings.size()).append(" รายการ):\n");
            for (MeetingInfo meeting : todayMeetings) {
                context.append("  * เลขที่: ").append(meeting.getMeetingNo())
                       .append(", ประเภท: ").append(meeting.getMeetingType())
                       .append(", วันที่: ").append(meeting.getMeetingDate())
                       .append(", เวลา: ").append(meeting.getMeetingTime())
                       .append(", สถานะ: ").append(meeting.getStatus());
                if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
                    // ลบ HTML tags
                    String cleanDesc = meeting.getDescription()
                            .replaceAll("<[^>]*>", "")
                            .replaceAll("&nbsp;", " ")
                            .trim();
                    if (!cleanDesc.isEmpty()) {
                        context.append(", เรื่อง: ").append(cleanDesc);
                    }
                }
                context.append("\n");
            }
        } else {
            context.append("- การประชุม: ไม่มีในระบบ\n");
        }

        if (pendingDocumentsCount > 0) {
            context.append("- เอกสารรออนุมัติ: ").append(pendingDocumentsCount).append(" รายการ\n");
        } else {
            context.append("- เอกสารรออนุมัติ: ไม่มี\n");
        }

        if (assetsList != null && !assetsList.isEmpty()) {
            context.append("- ทรัพย์สินทั้งหมดในระบบ (").append(assetsList.size()).append(" รายการ):\n");
            for (AssetInfo asset : assetsList) {
                context.append("  * ID: ").append(asset.getId())
                       .append(", ชื่อ: ").append(asset.getName())
                       .append(", ประเภท: ").append(asset.getAssetType())
                       .append(", จำนวน: ").append(asset.getQuantity())
                       .append(", สถานะ: ").append(asset.getStatus())
                       .append("\n");
            }
        } else {
            context.append("- ทรัพย์สิน: ไม่มีในระบบ\n");
        }

        if (pendingAssetsCount > 0) {
            context.append("- ทรัพย์สินรอตรวจสอบ (สถานะ PENDING): ").append(pendingAssetsCount).append(" รายการ\n");
        }

        if (unreadNotificationsCount > 0) {
            context.append("- การแจ้งเตือนที่ยังไม่ได้อ่าน: ").append(unreadNotificationsCount).append(" รายการ\n");
        } else {
            context.append("- การแจ้งเตือนที่ยังไม่ได้อ่าน: ไม่มี\n");
        }

        if (committeeMembers != null && !committeeMembers.isEmpty()) {
            context.append("- คณะกรรมการทั้งหมดในระบบ (").append(committeeMembers.size()).append(" คน):\n");
            for (CommitteeMemberInfo member : committeeMembers) {
                context.append("  * ID: ").append(member.getId())
                       .append(", ชื่อ: ").append(member.getFullName())
                       .append(", หน่วยงาน: ").append(member.getAffiliation() != null ? member.getAffiliation() : "-")
                       .append(", สังกัด: ").append(member.getDepartment() != null ? member.getDepartment() : "-");
                if (member.getPhone() != null && !member.getPhone().isEmpty()) {
                    context.append(", โทร: ").append(member.getPhone());
                }
                if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                    context.append(", อีเมล: ").append(member.getEmail());
                }
                context.append("\n");
            }
        } else {
            context.append("- คณะกรรมการ: ไม่มีในระบบ\n");
        }

        return context.toString();
    }
}
