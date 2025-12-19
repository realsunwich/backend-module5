package com.example.demo.controller;

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

    // ลบ ObjectMapper ออกจาก Constructor เพราะไม่ได้ใช้แล้ว
    public InvoiceController(TyphoonService typhoonService) {
        this.typhoonService = typhoonService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeInvoice(@RequestParam("file") MultipartFile file) {
        try {
            // 1. รับ JSON String จาก Service (Service ทำมาเป็น JSON String แล้ว)
            String result = typhoonService.analyzeInvoiceImage(file).block();

            System.out.println("Result to send: " + result);

            // 2. ส่ง String กลับไปตรงๆ แต่บอก Browser ว่านี่คือ JSON นะ
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON) // สำคัญ: บังคับ Header เป็น application/json
                    .body(result);

        } catch (Exception e) {
            e.printStackTrace();
            // กรณี Error ก็สร้าง JSON string ง่ายๆ ส่งกลับไป
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}