package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource; // เพิ่ม import นี้
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // อีเมลผู้ส่ง (ต้องตรงกับใน application.properties)
    private final String SENDER_EMAIL = "ictbookingroom@gmail.com";

    // --- Method 1: ส่งอีเมลแจ้งเตือนปกติ (HTML) ---
    @Async
    public void sendMeetingNotification(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(SENDER_EMAIL); // แก้เป็น Outlook
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            System.out.println("Email sent to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error sending email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Method 2: ส่งอีเมลพร้อมปุ่มกดรับนัด (Calendar Invite) ---
    @Async
    public void sendCalendarInvite(String to, String subject, String htmlBody, String icsContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart = true เพื่อรองรับไฟล์แนบ
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(SENDER_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // เนื้อหาอีเมล HTML

            // แปลง String icsContent เป็นไฟล์แนบในหน่วยความจำ
            ByteArrayResource resource = new ByteArrayResource(icsContent.getBytes("UTF-8"));

            // แนบไฟล์โดยระบุ Content-Type เป็น text/calendar; method=REQUEST
            // นี่คือหัวใจสำคัญที่ทำให้ Outlook/Gmail ขึ้นปุ่ม Accept/Decline
            helper.addAttachment("invite.ics", resource, "text/calendar; method=REQUEST; charset=UTF-8");

            mailSender.send(message);
            System.out.println("Calendar invite sent to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send calendar invite to " + to + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error sending calendar invite to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}