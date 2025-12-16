package com.example.demo.controller;

import com.example.demo.entity.Asset;
import com.example.demo.repository.AssetRepository;
import com.example.demo.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:3000", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.PATCH, RequestMethod.DELETE,
        RequestMethod.OPTIONS }, allowedHeaders = "*", allowCredentials = "true")
public class AssetController {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    // ดึงข้อมูลทั้งหมดไปแสดงบนแผนที่
    @GetMapping
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    // บันทึก Asset ใหม่ (พร้อมพิกัด)
    @PostMapping
    public Asset createAsset(@RequestBody Asset asset) {
        // ตั้งค่าเริ่มต้น
        if (asset.getStatus() == null) {
            asset.setStatus(Asset.AssetStatus.PENDING);
        }
        if (asset.getQuantity() == null) {
            asset.setQuantity(1);
        }
        return assetRepository.save(asset);
    }

    @PutMapping("/{id}")
    public Asset updateAsset(@PathVariable Long id, @RequestBody Asset assetDetails) {
        Asset asset = assetRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));

        asset.setName(assetDetails.getName());
        asset.setDescription(assetDetails.getDescription());
        asset.setLatitude(assetDetails.getLatitude());
        asset.setLongitude(assetDetails.getLongitude());

        // อัพเดทฟีลด์ใหม่
        if (assetDetails.getAssetType() != null) {
            asset.setAssetType(assetDetails.getAssetType());
        }
        if (assetDetails.getQuantity() != null) {
            asset.setQuantity(assetDetails.getQuantity());
        }

        // อัพเดทฟีลด์มูลค่า
        if (assetDetails.getTotalValue() != null) {
            asset.setTotalValue(assetDetails.getTotalValue());
        }
        if (assetDetails.getIndividualValues() != null) {
            asset.setIndividualValues(assetDetails.getIndividualValues());
        }
        if (assetDetails.getValueUnit() != null) {
            asset.setValueUnit(assetDetails.getValueUnit());
        }

        return assetRepository.save(asset);
    }

    // ยืนยันการพบหลักฐาน (PENDING -> CONFIRMED) พร้อมรับ notes และรูปภาพ
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Asset> confirmAsset(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile[] photos) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        if (asset.getStatus() != Asset.AssetStatus.PENDING) {
            return ResponseEntity.badRequest().build();
        }

        // อัพเดทสถานะ
        asset.setStatus(Asset.AssetStatus.CONFIRMED);
        asset.setConfirmedAt(LocalDateTime.now());

        // บันทึก notes
        if (notes != null && !notes.isEmpty()) {
            asset.setConfirmNotes(notes);
        }

        // บันทึกรูปภาพ
        if (photos != null && photos.length > 0) {
            try {
                List<String> photoUrls = fileStorageService.saveFiles(photos);
                // แปลง List เป็น JSON string
                String photoUrlsJson = objectMapper.writeValueAsString(photoUrls);
                asset.setPhotoUrls(photoUrlsJson);
            } catch (IOException e) {
                return ResponseEntity.status(500).build();
            }
        }

        Asset updatedAsset = assetRepository.save(asset);
        return ResponseEntity.ok(updatedAsset);
    }

    // เช็คอินหลักฐาน (CONFIRMED -> CHECKED_IN)
    @PatchMapping("/{id}/checkin")
    public ResponseEntity<Asset> checkInAsset(@PathVariable Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        if (asset.getStatus() != Asset.AssetStatus.CONFIRMED) {
            return ResponseEntity.badRequest().build();
        }

        asset.setStatus(Asset.AssetStatus.CHECKED_IN);
        asset.setCheckedInAt(LocalDateTime.now()); // บันทึกเวลาเช็คอิน
        Asset updatedAsset = assetRepository.save(asset);

        return ResponseEntity.ok(updatedAsset);
    }

    // อัพเดทสถานะหลักฐานแบบยืดหยุ่น (รองรับการเปลี่ยนสถานะใดๆ)
    @PatchMapping("/{id}/status")
    public ResponseEntity<Asset> updateAssetStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        try {
            Asset.AssetStatus newStatus = Asset.AssetStatus.valueOf(request.getStatus());
            asset.setStatus(newStatus);

            // ถ้าเป็นการเช็คอิน ให้บันทึกเวลา
            if (newStatus == Asset.AssetStatus.CHECKED_IN && asset.getCheckedInAt() == null) {
                asset.setCheckedInAt(LocalDateTime.now());
            }

            Asset updatedAsset = assetRepository.save(asset);
            return ResponseEntity.ok(updatedAsset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ยึดหลักฐาน (CONFIRMED -> CHECKED_IN)
    @PatchMapping("/{id}/seize")
    public ResponseEntity<Asset> seizeAsset(@PathVariable Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        if (asset.getStatus() != Asset.AssetStatus.CONFIRMED) {
            return ResponseEntity.badRequest().build();
        }

        asset.setStatus(Asset.AssetStatus.CHECKED_IN);
        asset.setCheckedInAt(LocalDateTime.now());
        Asset updatedAsset = assetRepository.save(asset);

        return ResponseEntity.ok(updatedAsset);
    }

    // เพิ่มรูปภาพหลักฐานเพิ่มเติม
    @PostMapping("/{id}/photos")
    public ResponseEntity<Asset> addPhotos(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile[] photos) {

        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        if (photos != null && photos.length > 0) {
            try {
                List<String> newPhotoUrls = fileStorageService.saveFiles(photos);

                // รวมรูปภาพเดิมกับรูปภาพใหม่
                List<String> existingUrls = new ArrayList<>();
                if (asset.getPhotoUrls() != null && !asset.getPhotoUrls().isEmpty()) {
                    try {
                        existingUrls = objectMapper.readValue(
                            asset.getPhotoUrls(),
                            new TypeReference<List<String>>() {}
                        );
                    } catch (IOException e) {
                        // If parsing fails, start fresh
                    }
                }

                existingUrls.addAll(newPhotoUrls);

                // แปลง List เป็น JSON string
                String photoUrlsJson = objectMapper.writeValueAsString(existingUrls);
                asset.setPhotoUrls(photoUrlsJson);

                Asset updatedAsset = assetRepository.save(asset);
                return ResponseEntity.ok(updatedAsset);
            } catch (IOException e) {
                return ResponseEntity.status(500).build();
            }
        }

        return ResponseEntity.badRequest().build();
    }

    // DTO สำหรับรับข้อมูลการอัพเดทสถานะ
    public static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}