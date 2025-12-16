package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads/assets");

    public FileStorageService() {
        try {
            // สร้างโฟลเดอร์ถ้ายังไม่มี
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    /**
     * บันทึกไฟล์และคืนค่า URL
     */
    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        // สร้างชื่อไฟล์ที่ไม่ซ้ำกัน
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // บันทึกไฟล์
        Path destinationFile = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        // คืนค่า relative path ที่จะใช้เป็น URL
        return "/uploads/assets/" + filename;
    }

    /**
     * บันทึกหลายไฟล์พร้อมกัน
     */
    public List<String> saveFiles(MultipartFile[] files) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String url = saveFile(file);
                    fileUrls.add(url);
                }
            }
        }
        return fileUrls;
    }

    /**
     * ลบไฟล์
     */
    public void deleteFile(String filename) throws IOException {
        Path file = uploadDir.resolve(filename);
        Files.deleteIfExists(file);
    }
}
