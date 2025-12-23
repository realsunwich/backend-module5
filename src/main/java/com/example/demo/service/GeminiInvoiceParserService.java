package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service สำหรับใช้ Gemini AI วิเคราะห์ข้อมูลจากใบกำกับภาษี/ใบเสร็จ
 * รับข้อมูล raw text จาก Typhoon OCR แล้วแปลงเป็น structured JSON
 */
@Service
public class GeminiInvoiceParserService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * วิเคราะห์ข้อมูลจาก raw text ที่ได้จาก Typhoon OCR
     * ให้ Gemini ทำหน้าที่ดึงข้อมูลและใส่ในตัวแปรที่ถูกต้อง
     */
    public String parseInvoiceData(String rawText) {
        try {
            // System Instruction สำหรับ Gemini
            String systemPromptText = """
                    You are an expert invoice/receipt data extraction AI.
                    Your task is to analyze the raw text extracted from Thai invoices/receipts and structure it into a proper JSON format.

                    IMPORTANT RULES:
                    1. Extract data EXACTLY as shown in the text - DO NOT modify or "fix" anything
                    2. Do NOT make up or guess any values
                    3. If a field is not found, set it to null
                    4. Preserve Thai language characters EXACTLY (don't change spellings)
                    5. Convert all prices to decimal format (e.g., "1,234.56" → "1234.56")
                    6. For Tax IDs: keep all 13 digits exactly as shown (just remove dashes/spaces)
                    7. For names: preserve exact spelling - even if they seem unusual
                    8. Return ONLY valid JSON - no markdown, no explanation

                    VALIDATION CHECKS:
                    - Tax ID should be exactly 13 digits (if less, something is wrong - recheck the text)
                    - Dates should be in DD/MM/YYYY format
                    - All prices should be positive numbers
                    - Item quantities should make sense

                    JSON Structure to return:
                    {
                      "document_type": "ใบกำกับภาษี/ใบเสร็จรับเงิน/ใบส่งของ or null",
                      "invoice_number": "document number or null",
                      "issue_date": "date in DD/MM/YYYY format or null",
                      "seller": {
                        "name": "seller name or null",
                        "address": "complete address or null",
                        "tax_id": "13-digit tax ID or null",
                        "phone": "phone number or null"
                      },
                      "buyer": {
                        "name": "buyer name or null",
                        "address": "buyer address or null",
                        "tax_id": "13-digit tax ID or null"
                      },
                      "items": [
                        {
                          "description": "item description",
                          "quantity": "quantity as string",
                          "unit_price": "price in decimal format",
                          "total": "total in decimal format"
                        }
                      ],
                      "payment": {
                        "amount_before_tax": "decimal format or null",
                        "vat_amount": "decimal format or null",
                        "total_amount": "decimal format or null",
                        "discount": "decimal format or null"
                      }
                    }
                    """;

            // User message
            String userMessage = """
                    Please analyze the following text extracted from a Thai invoice/receipt and return structured JSON data:

                    """ + rawText + """


                    Extract all relevant information and format it according to the JSON structure specified.
                    Return ONLY the JSON object, no additional text.
                    """;

            // สร้าง Request Body สำหรับ Gemini API
            // ใช้ systemInstruction (v1beta รองรับ)
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", systemPromptText);

            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", Collections.singletonList(sysPart));

            Map<String, Object> userPart = new HashMap<>();
            userPart.put("text", userMessage);

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", Collections.singletonList(userPart));

            Map<String, Object> body = new HashMap<>();
            body.put("systemInstruction", systemInstruction);  // v1beta format
            body.put("contents", Collections.singletonList(userContent));

            // เพิ่ม generation config เพื่อให้ได้ JSON ที่ดี
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1); // Low temperature for consistency
            generationConfig.put("candidateCount", 1);
            body.put("generationConfig", generationConfig);

            // เรียก Gemini API
            String finalUrl = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse response และดึง JSON ที่ Gemini สร้าง
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                throw new RuntimeException("Gemini API Error: " + root.path("error").toPrettyString());
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                return createEmptyInvoiceJson();
            }

            String generatedText = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // ลบ markdown code block ถ้ามี (```json ... ```)
            generatedText = generatedText.trim();
            if (generatedText.startsWith("```json")) {
                generatedText = generatedText.substring(7);
            }
            if (generatedText.startsWith("```")) {
                generatedText = generatedText.substring(3);
            }
            if (generatedText.endsWith("```")) {
                generatedText = generatedText.substring(0, generatedText.length() - 3);
            }
            generatedText = generatedText.trim();

            // Validate ว่าเป็น JSON ที่ valid
            try {
                JsonNode validationNode = objectMapper.readTree(generatedText);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(validationNode);
            } catch (Exception e) {
                System.err.println("Invalid JSON from Gemini: " + generatedText);
                return createEmptyInvoiceJson();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing invoice with Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * สร้าง Empty JSON Structure เมื่อไม่สามารถ parse ได้
     */
    private String createEmptyInvoiceJson() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putNull("document_type");
        root.putNull("invoice_number");
        root.putNull("issue_date");

        ObjectNode seller = objectMapper.createObjectNode();
        seller.putNull("name");
        seller.putNull("address");
        seller.putNull("tax_id");
        seller.putNull("phone");
        root.set("seller", seller);

        ObjectNode buyer = objectMapper.createObjectNode();
        buyer.putNull("name");
        buyer.putNull("address");
        buyer.putNull("tax_id");
        root.set("buyer", buyer);

        ArrayNode items = objectMapper.createArrayNode();
        root.set("items", items);

        ObjectNode payment = objectMapper.createObjectNode();
        payment.putNull("amount_before_tax");
        payment.putNull("vat_amount");
        payment.putNull("total_amount");
        payment.putNull("discount");
        root.set("payment", payment);

        return root.toString();
    }
}
