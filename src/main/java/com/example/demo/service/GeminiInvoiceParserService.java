package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.Base64;

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
     * ใช้ Gemini Vision API อ่านข้อมูลจากรูปโดยตรง (ไม่ผ่าน OCR)
     * ส่งรูปไปให้ Gemini วิเคราะห์เอง
     */
    public String extractRawTextFromImageVision(MultipartFile file) {
        try {
            // แปลงรูปเป็น base64
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();

            // Prompt สำหรับให้ Gemini อ่านข้อมูลจากรูป
            String promptText = """
                    You are a high-precision document reader. Extract ALL text from this Thai invoice/receipt image with EXTREME ACCURACY.

                    CRITICAL RULES:
                    1. Read each character EXACTLY as shown - DO NOT guess or auto-correct
                    2. Preserve all Thai diacritics and tone marks precisely
                    3. For numbers, extract EVERY digit exactly (especially Tax IDs must be complete 13 digits)
                    4. Read addresses and names character-by-character to avoid mistakes
                    5. If text is unclear, include what you see rather than making assumptions

                    CRITICAL VALIDATION FOR TAX IDs (13 DIGITS):
                    - Seller Tax ID format: 0-X-XXXX-XXXXX-XX-X (total 13 digits)
                    - Count each digit individually to verify 13 digits total
                    - Example: 0-1-0-5-5-5-7-0-0-0-8-1-9 = 13 digits ✓
                    - If you count only 12 digits, look more carefully - there must be 13

                    ADDRESS READING RULES:
                    - Thai addresses: XXX/XXX format (house/section)
                    - Read both parts carefully: 122/122 vs 12/122 are different
                    - Common pattern: three digits before slash

                    THAI NAME ACCURACY:
                    - Read character-by-character with all tone marks
                    - Don't auto-correct: ผู้นำรถ ≠ ผู้รักรัก

                    Extract everything including:
                    - Document headers and types
                    - Document numbers and dates
                    - Seller information (name, address, tax ID, phone)
                    - Buyer information (name, address, tax ID)
                    - Item descriptions (read carefully - don't substitute words)
                    - All numbers: quantities, prices, tax amounts
                    - Any notes or remarks

                    Output ONLY the raw text exactly as you see it, with no explanations or markdown.
                    """;

            // สร้าง Request Body สำหรับ Gemini Vision API
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", promptText);

            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", Arrays.asList(textPart, imagePart));

            Map<String, Object> body = new HashMap<>();
            body.put("contents", Collections.singletonList(userContent));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.0); // Maximum accuracy
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

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                throw new RuntimeException("Gemini Vision API Error: " + root.path("error").toPrettyString());
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                return "";
            }

            String extractedText = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            return extractedText.trim();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error extracting text with Gemini Vision: " + e.getMessage(), e);
        }
    }

    /**
     * วิเคราะห์ใบกำกับภาษีด้วยการเปรียบเทียบผลลัพธ์จาก 2 แหล่ง:
     * 1. Typhoon OCR
     * 2. Gemini Vision
     *
     * แล้วให้ Gemini เปรียบเทียบและเลือกข้อมูลที่ถูกต้องที่สุด (ไม่ลำเอียง)
     */
    public String analyzeInvoiceWithCrossValidation(String typhoonText, String geminiVisionText) {
        try {
            // System Instruction สำหรับการเปรียบเทียบอย่างเป็นกลาง
            String systemPromptText = """
                    You are an UNBIASED data validation expert for Thai invoice/receipt analysis.

                    Your task is to compare two OCR outputs from the SAME invoice image and produce the MOST ACCURATE structured JSON.

                    CRITICAL RULES FOR UNBIASED COMPARISON:
                    1. DO NOT favor either source - evaluate each field independently based on accuracy
                    2. For each field, compare both sources and choose the MORE ACCURATE one based on:
                       - Completeness (e.g., 13-digit tax ID vs 12-digit)
                       - Correct Thai spelling (preserve diacritics, avoid substitutions)
                       - Numerical accuracy (correct number of digits, decimal places)
                       - Format correctness (dates, addresses, etc.)
                    3. If both sources agree, use that value (high confidence)
                    4. If sources disagree, analyze which is more likely correct based on context

                    ULTRA-STRICT TAX ID VALIDATION:
                    5. Tax IDs MUST be exactly 13 digits - NO EXCEPTIONS
                    6. If one source has 13 digits and other has 12, ALWAYS choose the 13-digit version
                    7. If both have 12 digits, look for patterns to infer the missing digit:
                       - Compare with format: 0-X-XXXX-XXXXX-XX-X
                       - Check if first digit is present (usually 0 or 1)
                    8. Count digits manually: 0-1-0-5-5-5-7-0-0-0-8-1-9 = 13 ✓

                    ADDRESS VALIDATION:
                    9. For Thai addresses (XXX/XXX format):
                       - Compare both parts before and after slash
                       - If one says "12/122" and other says "122/122", choose based on:
                         * Common patterns (e.g., 122/122 is more common than 12/122)
                         * Context from other address numbers

                    THAI NAME VALIDATION:
                    10. For Thai names: Preserve exact characters including tone marks
                    11. Don't auto-correct unusual spellings
                    12. If sources disagree (e.g., ผู้นำรถ vs ผู้รักรัก), consider:
                        - Which has more consistent tone marks
                        - Which makes more semantic sense in context

                    13. If both sources are unclear or missing, set to null

                    OUTPUT: Return ONLY valid JSON with the most accurate data from cross-validation.
                    No markdown, no explanations, no bias towards either source.

                    JSON Structure:
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

            // User message พร้อมข้อมูลจากทั้ง 2 แหล่ง
            String userMessage = String.format("""
                    Compare the following two OCR outputs from the SAME Thai invoice/receipt image.
                    Analyze each field carefully and select the MOST ACCURATE data.

                    SOURCE 1 - Typhoon OCR:
                    %s

                    SOURCE 2 - Gemini Vision:
                    %s

                    Instructions:
                    1. Compare each field from both sources
                    2. Choose the more accurate value for each field (not just from one source)
                    3. Apply the validation rules (Tax ID = 13 digits, proper Thai spelling, etc.)
                    4. Return the structured JSON with the best cross-validated data

                    Return ONLY the JSON object, no additional text.
                    """, typhoonText, geminiVisionText);

            // สร้าง Request Body
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
            body.put("systemInstruction", systemInstruction);
            body.put("contents", Collections.singletonList(userContent));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1); // Low temp for consistent logic
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

            // Parse response
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

            // ลบ markdown code block ถ้ามี
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

            // Validate JSON และเพิ่ม validation metadata
            try {
                JsonNode dataNode = objectMapper.readTree(generatedText);

                // เพิ่ม validation metadata
                ObjectNode responseWithValidation = objectMapper.createObjectNode();
                responseWithValidation.set("data", dataNode);
                responseWithValidation.set("validation", createValidationMetadata(dataNode, typhoonText, geminiVisionText));
                responseWithValidation.put("confidence_score", calculateConfidenceScore(typhoonText, geminiVisionText));

                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseWithValidation);
            } catch (Exception e) {
                System.err.println("Invalid JSON from Gemini cross-validation: " + generatedText);
                return createEmptyInvoiceJson();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error in cross-validation: " + e.getMessage(), e);
        }
    }

    /**
     * สร้าง validation metadata สำหรับแต่ละ field
     */
    private ObjectNode createValidationMetadata(JsonNode dataNode, String typhoonText, String geminiVisionText) {
        ObjectNode validation = objectMapper.createObjectNode();

        // Validate seller tax_id
        String sellerTaxId = dataNode.path("seller").path("tax_id").asText("");
        if (!sellerTaxId.isEmpty()) {
            ObjectNode taxIdValidation = objectMapper.createObjectNode();
            taxIdValidation.put("value", sellerTaxId);

            if (sellerTaxId.length() != 13) {
                taxIdValidation.put("status", "error");
                taxIdValidation.put("message", "Tax ID ต้องมี 13 หลัก (ปัจจุบันมี " + sellerTaxId.length() + " หลัก)");

                // เสนอแนะการแก้ไข
                ArrayNode suggestions = objectMapper.createArrayNode();
                if (sellerTaxId.length() == 12) {
                    // ลองเพิ่ม 0 ข้างหน้า
                    suggestions.add("0" + sellerTaxId);
                }
                taxIdValidation.set("suggestions", suggestions);
            } else {
                taxIdValidation.put("status", "success");
                taxIdValidation.put("message", "ถูกต้อง");
            }

            validation.set("seller.tax_id", taxIdValidation);
        }

        // Validate buyer name (ตรวจสอบว่า Typhoon และ Gemini ตรงกันหรือไม่)
        String buyerName = dataNode.path("buyer").path("name").asText("");
        if (!buyerName.isEmpty()) {
            ObjectNode nameValidation = objectMapper.createObjectNode();
            nameValidation.put("value", buyerName);

            // เช็คว่า 2 แหล่งมีความแตกต่างหรือไม่
            boolean typhoonHasName = typhoonText.toLowerCase().contains(buyerName.toLowerCase().substring(0, Math.min(4, buyerName.length())));
            boolean geminiHasName = geminiVisionText.toLowerCase().contains(buyerName.toLowerCase().substring(0, Math.min(4, buyerName.length())));

            if (!typhoonHasName || !geminiHasName) {
                nameValidation.put("status", "warning");
                nameValidation.put("message", "ไม่แน่ใจในการอ่าน - ควรตรวจสอบ");
                nameValidation.put("confidence", 0.6);

                // เสนอชื่อทางเลือกจาก raw text
                ArrayNode alternatives = objectMapper.createArrayNode();
                // (สามารถเพิ่มการ parse alternatives จาก raw text ได้)
                nameValidation.set("alternatives", alternatives);
            } else {
                nameValidation.put("status", "success");
                nameValidation.put("confidence", 0.9);
            }

            validation.set("buyer.name", nameValidation);
        }

        // Validate buyer address
        String buyerAddress = dataNode.path("buyer").path("address").asText("");
        if (!buyerAddress.isEmpty() && buyerAddress.contains("/")) {
            ObjectNode addressValidation = objectMapper.createObjectNode();
            addressValidation.put("value", buyerAddress);

            String[] parts = buyerAddress.split("/");
            if (parts.length == 2 && parts[0].length() < 3) {
                // ตัวเลขหน้า slash น้อยกว่า 3 หลัก อาจจะขาดหลัก
                addressValidation.put("status", "warning");
                addressValidation.put("message", "อาจจะเป็น " + parts[0] + parts[0].charAt(parts[0].length() - 1) + "/" + parts[1]);

                ArrayNode suggestions = objectMapper.createArrayNode();
                // เสนอแนะที่อยู่ที่เป็นไปได้
                if (parts[0].length() == 2) {
                    suggestions.add(parts[0] + parts[0].charAt(1) + "/" + parts[1]); // 12 -> 122
                }
                addressValidation.set("suggestions", suggestions);
            } else {
                addressValidation.put("status", "success");
            }

            validation.set("buyer.address", addressValidation);
        }

        // Validate issue_date format
        String issueDate = dataNode.path("issue_date").asText("");
        if (!issueDate.isEmpty()) {
            ObjectNode dateValidation = objectMapper.createObjectNode();
            dateValidation.put("value", issueDate);

            if (!issueDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
                dateValidation.put("status", "error");
                dateValidation.put("message", "รูปแบบวันที่ไม่ถูกต้อง (ต้องเป็น DD/MM/YYYY)");
            } else {
                dateValidation.put("status", "success");
            }

            validation.set("issue_date", dateValidation);
        }

        return validation;
    }

    /**
     * คำนวณ confidence score จากการเปรียบเทียบ 2 แหล่ง
     */
    private double calculateConfidenceScore(String typhoonText, String geminiVisionText) {
        // เปรียบเทียบความยาวของข้อความ
        int minLength = Math.min(typhoonText.length(), geminiVisionText.length());
        int maxLength = Math.max(typhoonText.length(), geminiVisionText.length());

        // ถ้าความยาวใกล้เคียงกัน = confidence สูง
        double lengthSimilarity = (double) minLength / maxLength;

        // คำนวณ word overlap (คร่าวๆ)
        String[] typhoonWords = typhoonText.split("\\s+");
        String[] geminiWords = geminiVisionText.split("\\s+");

        int matchingWords = 0;
        for (String tw : typhoonWords) {
            for (String gw : geminiWords) {
                if (tw.equalsIgnoreCase(gw)) {
                    matchingWords++;
                    break;
                }
            }
        }

        double wordOverlap = (double) matchingWords / Math.max(typhoonWords.length, geminiWords.length);

        // Weighted average
        return (lengthSimilarity * 0.3 + wordOverlap * 0.7);
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
