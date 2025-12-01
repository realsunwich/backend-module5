package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FileUploadController {

    // กำหนดโฟลเดอร์ที่จะเก็บไฟล์ (ควรอยู่นอกโปรเจกต์ หรือ config ใน
    // application.properties)
    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        try {
            // 1. สร้างโฟลเดอร์ถ้ายังไม่มี
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. สร้างชื่อไฟล์ใหม่เพื่อไม่ให้ซ้ำ (UUID + นามสกุลเดิม)
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + fileExtension;

            // 3. บันทึกไฟล์ลง Disk
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 4. ส่ง URL หรือ Path กลับไปให้ Frontend (เช่น /uploads/abc-123.pdf)
            // หมายเหตุ: ต้อง Config ResourceHandler เพื่อให้เข้าถึง path นี้ได้
            String fileUrl = "/uploads/" + newFilename;

            return ResponseEntity.ok(new FileResponse(fileUrl, originalFilename));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }

    // Helper Class สำหรับ Response
    static class FileResponse {
        public String url;
        public String originalName;

        public FileResponse(String url, String originalName) {
            this.url = url;
            this.originalName = originalName;
        }
    }
}