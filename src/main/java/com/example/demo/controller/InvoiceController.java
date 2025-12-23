package com.example.demo.controller;

import com.example.demo.service.GeminiInvoiceParserService;
import com.example.demo.service.TyphoonService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoice")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {

    private final TyphoonService typhoonService;
    private final GeminiInvoiceParserService geminiParser;

    public InvoiceController(TyphoonService typhoonService, GeminiInvoiceParserService geminiParser) {
        this.typhoonService = typhoonService;
        this.geminiParser = geminiParser;
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
}