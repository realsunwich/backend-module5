package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TyphoonService {

    @Value("${typhoon.api.key}")
    private String typhoonApiKey;

    @Value("${typhoon.model.name:typhoon-ocr}")
    private String modelName;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TyphoonService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.opentyphoon.ai")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    /**
     * อ่านข้อมูลทั้งหมดจากรูป โดยไม่ทำการ parse (ให้ AI ตัวอื่นทำต่อ)
     */
    public Mono<String> extractRawTextFromImage(MultipartFile file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String imageUrl = "data:" + file.getContentType() + ";base64," + base64Image;

            // ปรับ Prompt ให้ Typhoon OCR อ่านข้อมูลทั้งหมดอย่างละเอียดและแม่นยำ
            String promptText = """
                    You are a high-precision OCR system. Extract ALL text from this Thai invoice/receipt image with EXTREME ACCURACY.

                    CRITICAL RULES:
                    1. Read each character EXACTLY as shown - DO NOT guess or auto-correct
                    2. Preserve all Thai diacritics and tone marks precisely (ั ้ ๊ ่ ็ etc.)
                    3. For numbers, extract EVERY digit exactly (especially Tax IDs must be complete)
                    4. Read addresses and names character-by-character to avoid mistakes
                    5. If text is unclear, include what you see rather than making assumptions

                    Extract everything including:
                    - Document headers (ใบกำกับภาษี, ใบเสร็จรับเงิน, etc.)
                    - Document numbers and dates
                    - Seller information (name, address, tax ID, phone)
                    - Buyer information (name, address, tax ID)
                    - Item descriptions (read carefully - don't substitute words)
                    - All numbers: quantities, prices, tax amounts
                    - Any notes or remarks

                    IMPORTANT FOR ACCURACY:
                    - Tax IDs are 13 digits - count carefully
                    - Thai names may have unique spellings - don't "correct" them
                    - Preserve exact spacing and formatting
                    - Include ALL visible text even if it seems redundant

                    Output the raw text exactly as you see it.
                    """;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.0); // Lowest temp for maximum accuracy and deterministic output

            Map<String, Object> textContent = Map.of("type", "text", "text", promptText);
            Map<String, Object> imageContent = Map.of("type", "image_url", "image_url", Map.of("url", imageUrl));
            Map<String, Object> userMessage = Map.of("role", "user", "content", List.of(textContent, imageContent));

            requestBody.put("messages", List.of(userMessage));

            return webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + typhoonApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        try {
                            JsonNode root = objectMapper.readTree(response);
                            if (root.has("error")) {
                                throw new RuntimeException("API Error: " + root.path("error").toPrettyString());
                            }

                            JsonNode choices = root.path("choices");
                            if (choices.isEmpty())
                                return "";

                            String contentString = choices.get(0).path("message").path("content").asText();

                            // Handle Typhoon's nested JSON output
                            if (contentString.trim().startsWith("{") && contentString.contains("natural_text")) {
                                try {
                                    return objectMapper.readTree(contentString).path("natural_text").asText();
                                } catch (Exception e) {
                                    return contentString;
                                }
                            }

                            // Return raw extracted text
                            return contentString;

                        } catch (Exception e) {
                            throw new RuntimeException("Error parsing response: " + e.getMessage(), e);
                        }
                    });

        } catch (IOException e) {
            return Mono.error(new RuntimeException("Error processing image file", e));
        }
    }

    /**
     * Legacy method - ใช้ regex parsing แบบเดิม (เก็บไว้เผื่อต้องการใช้)
     */
    @Deprecated
    public Mono<String> analyzeInvoiceImage(MultipartFile file) {
        return extractRawTextFromImage(file)
                .map(this::convertMarkdownToJsonStructure);
    }

    // --- Core Logic: Robust Markdown Parsing ---

    private String convertMarkdownToJsonStructure(String text) {
        ObjectNode result = objectMapper.createObjectNode();

        // DEBUG: เก็บ raw markdown ไว้ดู
        result.put("debug_raw_markdown", text);

        // 1. Document Type - Support both Thai and English headers
        result.put("document_type", extractFirstMatch(text,
                "\\*\\*ประเภท\\*\\*:[\\s]*([^\\n]+)",
                "\\*\\*Type\\*\\*:[\\s]*([^\\n]+)",
                "# (ใบกำกับภาษี[^\\n]*|ใบเสร็จรับเงิน[^\\n]*)",
                "(ใบกำกับภาษี|ใบเสร็จรับเงิน|ใบส่งของ)"));

        // 2. Invoice Number - Enhanced patterns for Thai invoices
        String invNo = extractFirstMatch(text,
                "\\*\\*เลขที่\\*\\*:[\\s]*([^\\n]+)",
                "\\*\\*Document No\\*\\*:[\\s]*([^\\n]+)",
                "เลขที่[\\s:]*([A-Za-z0-9\\-/]+)",
                "เลขประจำตัวผู้เสียภาษี[\\s:]*([0-9]{13})",
                "(?:No\\.|Bill No|เลขที่)[\\s:.]*([A-Za-z0-9\\-/]+)");
        result.put("invoice_number", cleanValue(invNo));

        // 3. Date - Support Thai date formats
        String date = extractFirstMatch(text,
                "\\*\\*วันที่\\*\\*:[\\s]*([^\\n]+)",
                "\\*\\*Date\\*\\*:[\\s]*([^\\n]+)",
                "วันที่[\\s:]*([0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{2,4})",
                "Date[\\s:]*([0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{2,4})");
        result.put("issue_date", cleanValue(date));

        // 4. Seller Info - Support Thai headers
        ObjectNode seller = objectMapper.createObjectNode();

        String sellerName = extractFirstMatch(text,
                "# ร้านค้า[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "# ผู้ขาย[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "# Seller[\\s\\S]{0,600}?\\*\\*Name\\*\\*:[\\s]*([^\\n]+)",
                "# Seller[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "ร้านค้า[^\\n]*?[:\\s]+([ก-๙A-Za-z0-9\\s]+(?:จำกัด|มหาชน|บริษัท)?[^\\n]{5,60})");
        seller.put("name", cleanValue(sellerName));

        String sellerAddr = extractFirstMatch(text,
                "# ร้านค้า[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)",
                "# ผู้ขาย[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)",
                "# Seller[\\s\\S]{0,800}?\\*\\*Address\\*\\*:[\\s]*([^\\n]+)",
                "# Seller[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)",
                "ที่อยู่[\\s:]*([^\\n]{20,200})");
        seller.put("address", cleanValue(sellerAddr));

        String sellerTax = extractFirstMatch(text,
                "# ร้านค้า[\\s\\S]{0,1000}?\\*\\*เลขประจำตัวผู้เสียภาษี\\*\\*:[\\s]*([0-9\\-]{13,17})",
                "# ผู้ขาย[\\s\\S]{0,1000}?\\*\\*เลขประจำตัวผู้เสียภาษี\\*\\*:[\\s]*([0-9\\-]{13,17})",
                "# Seller[\\s\\S]{0,1000}?\\*\\*Tax ID\\*\\*:[\\s]*([0-9\\-]{13,17})",
                "เลขประจำตัวผู้เสียภาษี[\\s:]*([0-9\\-]{13,17})",
                "Tax ID[\\s:]*([0-9\\-]{13,17})");
        seller.put("tax_id", normalizeTaxId(sellerTax));

        String sellerPhone = extractFirstMatch(text,
                "# ร้านค้า[\\s\\S]{0,800}?\\*\\*โทรศัพท์\\*\\*:[\\s]*([^\\n]+)",
                "# Seller[\\s\\S]{0,800}?\\*\\*Phone\\*\\*:[\\s]*([^\\n]+)",
                "โทร[\\s:.]*([0-9\\-]{8,15})");
        seller.put("phone", cleanValue(sellerPhone));
        result.set("seller", seller);

        // 5. Buyer Info
        ObjectNode buyer = objectMapper.createObjectNode();

        String buyerName = extractFirstMatch(text,
                "# ลูกค้า[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "# ผู้ซื้อ[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "# Buyer[\\s\\S]{0,600}?\\*\\*Name\\*\\*:[\\s]*([^\\n]+)",
                "# Buyer[\\s\\S]{0,600}?\\*\\*ชื่อ\\*\\*:[\\s]*([^\\n]+)",
                "ลูกค้า[\\s:]*([ก-๙A-Za-z0-9\\s]{3,60})");
        buyer.put("name", cleanValue(buyerName));

        String buyerAddr = extractFirstMatch(text,
                "# ลูกค้า[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)",
                "# ผู้ซื้อ[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)",
                "# Buyer[\\s\\S]{0,800}?\\*\\*Address\\*\\*:[\\s]*([^\\n]+)",
                "# Buyer[\\s\\S]{0,800}?\\*\\*ที่อยู่\\*\\*:[\\s]*([^\\n]+)");
        buyer.put("address", cleanValue(buyerAddr));

        String buyerTax = extractFirstMatch(text,
                "# ลูกค้า[\\s\\S]{0,1000}?\\*\\*เลขประจำตัวผู้เสียภาษี\\*\\*:[\\s]*([0-9\\-]{13,17})",
                "# ผู้ซื้อ[\\s\\S]{0,1000}?\\*\\*เลขประจำตัวผู้เสียภาษี\\*\\*:[\\s]*([0-9\\-]{13,17})",
                "# Buyer[\\s\\S]{0,1000}?\\*\\*Tax ID\\*\\*:[\\s]*([0-9\\-]{13,17})");
        buyer.put("tax_id", normalizeTaxId(buyerTax));
        result.set("buyer", buyer);

        // 6. Payment Info - Enhanced extraction for Thai invoices
        ObjectNode payment = objectMapper.createObjectNode();

        // Subtotal / Amount before tax
        String subtotal = extractFirstMatch(text,
                "\\*\\*ราคาสินค้า\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "\\*\\*Amount Before Tax\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "มูลค่าก่อนภาษี[\\s:]*([0-9,]+\\.?[0-9]*)",
                "ราคาสินค้า[\\s:]*([0-9,]+\\.?[0-9]*)",
                "ทั้งหมด[\\s:]*([0-9,]+\\.?[0-9]*)");
        payment.put("amount_before_tax", normalizePrice(subtotal));

        // VAT Amount
        String vat = extractFirstMatch(text,
                "\\*\\*ภาษีมูลค่าเพิ่ม\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "\\*\\*VAT Amount\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "ภาษีมูลค่าเพิ่ม[\\s:]*([0-9,]+\\.?[0-9]*)",
                "VAT[\\s:]*([0-9,]+\\.?[0-9]*)");
        payment.put("vat_amount", normalizePrice(vat));

        // Grand Total
        String total = extractFirstMatch(text,
                "\\*\\*รวมทั้งสิ้น\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "\\*\\*Total Amount\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "รวมทั้งสิ้น[\\s:]*([0-9,]+\\.?[0-9]*)",
                "ราคารวมทั้งสิ้น[\\s:]*([0-9,]+\\.?[0-9]*)",
                "Grand Total[\\s:]*([0-9,]+\\.?[0-9]*)",
                "Total[\\s:]*([0-9,]+\\.?[0-9]*)");
        payment.put("total_amount", normalizePrice(total));

        // Discount
        String discount = extractFirstMatch(text,
                "\\*\\*ส่วนลด\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "\\*\\*Discount\\*\\*:[\\s]*([0-9,]+\\.?[0-9]*)",
                "ส่วนลด[\\s:]*([0-9,]+\\.?[0-9]*)");
        payment.put("discount", normalizePrice(discount));

        result.set("payment", payment);

        // 7. Extract Table Items
        ArrayNode items = extractTableItems(text);
        result.set("items", items);

        return result.toString();
    }

    // --- Helper Methods ---

    /**
     * Priority Search: รับหลาย Regex แล้วคืนค่าจากตัวแรกที่เจอ
     * ช่วยให้รองรับ Format ที่หลากหลายของใบกำกับภาษีแต่ละแบบ
     */
    private String extractFirstMatch(String text, String... patterns) {
        if (text == null)
            return null;
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    if (m.groupCount() >= 1) {
                        return m.group(1).trim();
                    }
                    return m.group().trim();
                }
            } catch (Exception e) {
                // Ignore bad patterns
            }
        }
        return null;
    }

    /**
     * ดึงข้อมูลจาก Markdown Table - รองรับทั้ง Thai และ English headers
     */
    private ArrayNode extractTableItems(String text) {
        ArrayNode items = objectMapper.createArrayNode();
        try {
            // หาบรรทัดที่เป็น Table Row (ขึ้นต้นด้วย |) - รองรับ 4-5 columns
            Pattern p = Pattern.compile("^\\|(.+?)\\|(.+?)\\|(.+?)\\|(.+?)(?:\\|(.+?))?\\|?$", Pattern.MULTILINE);
            Matcher m = p.matcher(text);

            while (m.find()) {
                String col1 = m.group(1).trim();
                String col2 = m.group(2).trim();
                String col3 = m.group(3).trim();
                String col4 = m.group(4).trim();

                // Skip separator rows (contains ---)
                if (col1.contains("---") || col2.contains("---")) {
                    continue;
                }

                // Skip headers (Thai and English)
                if (col1.toLowerCase().contains("description")
                    || col1.toLowerCase().contains("รายการ")
                    || col1.toLowerCase().contains("สินค้า")
                    || col1.toLowerCase().contains("item")
                    || col2.toLowerCase().contains("quantity")
                    || col2.toLowerCase().contains("จำนวน")
                    || col2.toLowerCase().contains("qty")) {
                    continue;
                }

                // Skip empty rows
                if (col1.isEmpty() || col1.equals(" ") || col1.equals("-")) {
                    continue;
                }

                // Skip rows that look like totals/summaries
                if (col1.toLowerCase().contains("total")
                    || col1.toLowerCase().contains("รวม")
                    || col1.toLowerCase().contains("subtotal")) {
                    continue;
                }

                ObjectNode item = objectMapper.createObjectNode();
                item.put("description", cleanValue(col1));
                item.put("quantity", cleanValue(col2));
                item.put("unit_price", normalizePrice(col3));
                item.put("total", normalizePrice(col4));
                items.add(item);
            }
        } catch (Exception e) {
            // If table parsing fails, return empty array
            System.err.println("Table extraction error: " + e.getMessage());
        }
        return items;
    }

    private String cleanValue(String input) {
        if (input == null)
            return null;
        return input.replaceAll("[*_]", "").trim();
    }

    private String normalizePrice(String input) {
        if (input == null || input.trim().isEmpty())
            return "0.00";

        // Remove everything except numbers and dots
        String cleaned = input.replaceAll("[^0-9.]", "");

        if (cleaned.isEmpty())
            return "0.00";

        try {
            double value = Double.parseDouble(cleaned);
            return String.format("%.2f", value);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

    /**
     * Normalize Thai Tax ID to 13 digits (remove dashes/spaces)
     */
    private String normalizeTaxId(String input) {
        if (input == null || input.trim().isEmpty())
            return null;

        // Remove all non-digit characters
        String cleaned = input.replaceAll("[^0-9]", "");

        // Validate it's 13 digits
        if (cleaned.length() == 13) {
            return cleaned;
        }

        // Return original if not valid
        return input.trim();
    }
}