package com.example.demo.controller;

import com.example.demo.dto.InvoiceRatingRequest;
import com.example.demo.dto.InvoiceRatingResponse;
import com.example.demo.service.GeminiInvoiceParserService;
import com.example.demo.service.InvoiceRatingService;
import com.example.demo.service.TyphoonService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {

    private final TyphoonService typhoonService;
    private final GeminiInvoiceParserService geminiParser;
    private final InvoiceRatingService invoiceRatingService;

    public InvoiceController(TyphoonService typhoonService, GeminiInvoiceParserService geminiParser,
                           InvoiceRatingService invoiceRatingService) {
        this.typhoonService = typhoonService;
        this.geminiParser = geminiParser;
        this.invoiceRatingService = invoiceRatingService;
    }

    /**
     * Workflow ใหม่: Typhoon OCR อ่านข้อมูลทั้งหมด → Gemini วิเคราะห์และใส่ค่าตัวแปร
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeInvoice(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Starting Invoice Analysis ===");

            // Step 1: ใช้ Typhoon OCR อ่านข้อมูลทั้งหมดจากรูป
            System.out.println("Step 1: Extracting raw text with Typhoon OCR...");
            String rawText = typhoonService.extractRawTextFromImage(file).block();

            System.out.println("Raw text extracted:");
            System.out.println(rawText);
            System.out.println("---");

            // Step 2: ใช้ Gemini วิเคราะห์และใส่ค่าในตัวแปรที่ถูกต้อง
            System.out.println("Step 2: Parsing with Gemini AI...");
            String structuredJson = geminiParser.parseInvoiceData(rawText);

            System.out.println("Structured JSON result:");
            System.out.println(structuredJson);
            System.out.println("=== Analysis Complete ===");

            // Step 3: ส่งผลลัพธ์กลับ
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(structuredJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * ทดสอบ Typhoon OCR เท่านั้น - ดู raw text ที่อ่านได้
     */
    @PostMapping("/test-ocr")
    public ResponseEntity<String> testTyphoonOCR(@RequestParam("file") MultipartFile file) {
        try {
            String rawText = typhoonService.extractRawTextFromImage(file).block();

            // Return เป็น plain text เพื่อดูง่าย
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(rawText);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Workflow แบบเปรียบเทียบ (Cross-Validation):
     * 1. Typhoon OCR อ่านรูป → ได้ข้อความ A
     * 2. Gemini Vision อ่านรูป → ได้ข้อความ B
     * 3. Gemini เปรียบเทียบ A กับ B แบบเป็นกลาง → เลือกข้อมูลที่ถูกต้องที่สุด
     * 4. ส่ง JSON ที่ผ่านการ cross-validate กลับ
     */
    @PostMapping("/analyze-crossvalidate")
    public ResponseEntity<String> analyzeInvoiceWithCrossValidation(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Starting Cross-Validation Invoice Analysis ===");

            // Step 1: Typhoon OCR อ่านข้อมูลทั้งหมดจากรูป
            System.out.println("Step 1: Extracting text with Typhoon OCR...");
            String typhoonText = typhoonService.extractRawTextFromImage(file).block();

            System.out.println("Typhoon OCR result:");
            System.out.println(typhoonText);
            System.out.println("---");

            // Step 2: Gemini Vision อ่านข้อมูลจากรูปเดียวกันโดยตรง
            System.out.println("Step 2: Extracting text with Gemini Vision...");
            String geminiVisionText = geminiParser.extractRawTextFromImageVision(file);

            System.out.println("Gemini Vision result:");
            System.out.println(geminiVisionText);
            System.out.println("---");

            // Step 3: ให้ Gemini เปรียบเทียบผลลัพธ์ทั้งสองแบบเป็นกลาง และสร้าง JSON
            System.out.println("Step 3: Cross-validating and parsing with Gemini...");
            String structuredJson = geminiParser.analyzeInvoiceWithCrossValidation(typhoonText, geminiVisionText);

            System.out.println("Cross-validated JSON result:");
            System.out.println(structuredJson);
            System.out.println("=== Analysis Complete ===");

            // Step 4: ส่งผลลัพธ์กลับ
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(structuredJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Legacy endpoint - ใช้ Regex parsing แบบเดิม
     */
    @SuppressWarnings("deprecation")
    @PostMapping("/analyze-legacy")
    public ResponseEntity<String> analyzeInvoiceLegacy(@RequestParam MultipartFile file) {
        try {
            String result = typhoonService.analyzeInvoiceImage(file).block();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ==================== Invoice Rating Endpoints ====================

    /**
     * บันทึกการให้คะแนนและข้อมูล invoice
     */
    @PostMapping("/rating/save")
    public ResponseEntity<InvoiceRatingResponse> saveInvoiceRating(@RequestBody InvoiceRatingRequest request) {
        try {
            System.out.println("=== Saving Invoice Rating ===");
            System.out.println("Request: " + request);

            InvoiceRatingResponse response = invoiceRatingService.saveRating(request);

            if (response.isSuccess()) {
                System.out.println("Rating saved successfully with ID: " + response.getRatingId());
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Rating save failed: " + response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(InvoiceRatingResponse.error("Error saving rating: " + e.getMessage()));
        }
    }

    /**
     * ดึงสถิติการให้คะแนน
     */
    @GetMapping("/rating/statistics")
    public ResponseEntity<Map<String, Object>> getRatingStatistics() {
        try {
            Map<String, Object> stats = invoiceRatingService.getRatingStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ดึงสถิติการให้คะแนนแบบ text
     */
    @GetMapping("/rating/statistics/text")
    public ResponseEntity<String> getRatingStatisticsText() {
        try {
            String stats = invoiceRatingService.getRatingStatisticsText();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error retrieving statistics: " + e.getMessage());
        }
    }

    /**
     * ดึงการให้คะแนนล่าสุด 10 รายการ
     */
    @GetMapping("/rating/recent")
    public ResponseEntity<?> getRecentRatings() {
        try {
            return ResponseEntity.ok(invoiceRatingService.getRecentRatings());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * ดึงการให้คะแนนตามเลขที่ใบกำกับภาษี
     */
    @GetMapping("/rating/{invoiceNumber}")
    public ResponseEntity<?> getRatingByInvoiceNumber(@PathVariable String invoiceNumber) {
        try {
            return invoiceRatingService.getRatingByInvoiceNumber(invoiceNumber)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}