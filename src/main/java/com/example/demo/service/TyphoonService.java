package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${typhoon.model.name:typhoon-ocr}") // Default เป็น typhoon-ocr
    private String modelName;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TyphoonService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.opentyphoon.ai")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // เพิ่ม Buffer เป็น
                                                                                                    // 20MB เผื่อ
                                                                                                    // Markdown ยาว
                .build();
    }

    public Mono<String> analyzeInvoiceImage(MultipartFile file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String imageUrl = "data:" + file.getContentType() + ";base64," + base64Image;

            // Prompt สำหรับ Typhoon OCR: แยกข้อมูลใบกำกับภาษี/ใบเสร็จรับเงิน
            String promptText = """
                    Extract all information from this Thai tax invoice/receipt document and return in structured format.

                    Please identify and extract:
                    1. Document type (ใบกำกับภาษี, ใบเสร็จรับเงิน)
                    2. Invoice/Receipt number (เลขที่)
                    3. Tax invoice number (เลขประจำตัวผู้เสียภาษีอากร)
                    4. Issue date (วันที่)
                    5. Tax ID (ติดต่อ)

                    Seller Information (ร้านค้า/ให้บริการ):
                    - Name (บริษัท, ชื่อ)
                    - Address (ที่อยู่)
                    - Tax ID (เลขประจำตัวผู้เสียภาษีอากร)
                    - Contact (ติดต่อ)

                    Buyer Information (รายละเอียดลูกค้า):
                    - Name (ลูกค้า, ผู้ซื้อ)
                    - Address (ที่อยู่)
                    - Tax ID (เลขประจำตัวผู้เสียภาษีอากร)

                    Items (รายการสินค้า):
                    - Description (รายการสินค้า)
                    - Quantity (จำนวน)
                    - Unit price (ราคาต่อหน่วย)
                    - Amount (จำนวนเงิน)

                    Payment Summary:
                    - Subtotal (ทั้งหมด)
                    - Discount (ส่วนลด)
                    - Amount before tax (ค่าสินค้า)
                    - Subtotal (รวมราคาสุทธิ)
                    - VAT amount and rate (ภาษีมูลค่าเพิ่ม, %)
                    - Total amount (ราคารวมภาษีมูลค่าเพิ่ม)

                    Additional information:
                    - Note/Remarks (หมายเหตุ)
                    - Signature field (ผู้รับสงบาม)

                    Return all text content in Markdown format with clear sections.
                    """;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("max_tokens", 4096); // เผื่อผลลัพธ์ยาว

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
                            System.out.println("DEBUG Raw Response: " + response);

                            JsonNode root = objectMapper.readTree(response);

                            // 1. เช็ค Error
                            if (root.has("error")) {
                                throw new RuntimeException("API Error: " + root.path("error").toPrettyString());
                            }

                            // 2. เช็ค choices
                            JsonNode choices = root.path("choices");
                            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                                return "{}";
                            }

                            JsonNode firstChoice = choices.get(0);
                            if (firstChoice == null)
                                return "{}";

                            String contentString = firstChoice.path("message").path("content").asText();

                            // 3. แกะ natural_text (ถ้ามี) - Typhoon OCR ชอบส่งมาใน format นี้
                            String markdownText = "";
                            if (contentString != null && contentString.trim().startsWith("{")
                                    && contentString.contains("natural_text")) {
                                try {
                                    JsonNode inner = objectMapper.readTree(contentString);
                                    markdownText = inner.path("natural_text").asText();
                                } catch (Exception e) {
                                    markdownText = contentString;
                                }
                            } else {
                                markdownText = contentString;
                            }

                            // 4. แปลง Markdown เป็น JSON ที่ Frontend ต้องการ
                            return convertMarkdownToJsonStructure(markdownText);

                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException("Error parsing response: " + e.getMessage(), e);
                        }
                    });

        } catch (IOException e) {
            return Mono.error(new RuntimeException("Error processing image file", e));
        }
    }

    // --- Helper Methods: แปลง Markdown เป็น JSON ---

    private String convertMarkdownToJsonStructure(String text) {
        ObjectNode result = objectMapper.createObjectNode();

        // เก็บข้อความดิบไว้ดู (Optional) - เปิดเพื่อ debug
        result.put("raw_text", text);

        // ข้อมูลเอกสาร - ดึงจากบรรทัดแรก
        result.put("document_type", extractSimple(text, "ใบกำกับภาษี|ใบเสร็จรับเงิน|Tax Invoice|Receipt"));

        // ดึงเลขที่เอกสาร - รองรับทั้งตัวเลข 14 หลักและรูปแบบอื่นๆ
        String invoiceNumber = extractSimple(text, "(?:เลขที่|No\\.|Invoice No|Document No)[\\s:]+([0-9]{12,14})");
        if (invoiceNumber == null) {
            invoiceNumber = extractSimple(text, "[0-9]{12,14}");
        }
        result.put("invoice_number", invoiceNumber);

        // ดึงวันที่
        String issueDate = extractSimple(text, "(?:วันที่|Date)[\\s:]+([0-9]{2}/[0-9]{2}/[0-9]{4})");
        if (issueDate == null) {
            issueDate = extractSimple(text, "[0-9]{2}/[0-9]{2}/[0-9]{4}");
        }
        result.put("issue_date", issueDate);

        // ข้อมูลผู้ขาย (Seller/Vendor)
        ObjectNode seller = objectMapper.createObjectNode();

        // ดึงส่วน seller section ก่อน - ถ้าไม่เจอให้ใช้ทั้งหมด
        String sellerSection = extractSection(text, "ร้านค้าผู้ให้บริการ", "รายละเอียดลูกค้า");
        if (sellerSection == null) {
            sellerSection = extractSection(text, "Seller", "Buyer");
        }
        if (sellerSection == null) {
            // ถ้ายังไม่เจอ ให้ดึงส่วนแรกของ text
            sellerSection = text.substring(0, Math.min(500, text.length()));
        }

        // ดึงชื่อบริษัท/ร้าน - รองรับหลายรูปแบบ
        String sellerName = null;

        // ลองหา pattern "บริษัท xxx จำกัด (xxxxx)"
        sellerName = extractFromSection(sellerSection,
                "(บริษัท[\\s]+[\\u0E00-\\u0E7Fa-zA-Z0-9\\s\\.]+(?:จำกัด)?(?:[\\s]*\\([^)\\n]{1,20}\\))?)");

        // ถ้าไม่เจอ ลองหาจาก ** markdown
        if (sellerName == null) {
            sellerName = extractFromSection(sellerSection, "\\*\\*([^*\\n]+(?:บริษัท|จำกัด)[^*\\n]+)\\*\\*");
        }

        // ถ้ายังไม่เจอ ลองหาคำว่า "บริษัท" ธรรมดา
        if (sellerName == null) {
            sellerName = extractFromSection(sellerSection,
                    "บริษัท[\\s]+([\\u0E00-\\u0E7Fa-zA-Z0-9\\s\\.\\(\\)]{3,50})");
        }

        seller.put("name", sellerName);

        // ดึงที่อยู่ผู้ขาย - รองรับหลายรูปแบบ
        String sellerAddress = extractFromSection(sellerSection,
                "เลขที่[\\s]+([0-9]+[^\\n]+?(?:กรุงเทพมหานคร|จังหวัด)[^0-9]*[0-9]{5})");
        if (sellerAddress == null) {
            sellerAddress = extractFromSection(sellerSection, "\\nเลขที่[\\s]+([0-9]+[^\\n]+)");
        }
        seller.put("address", sellerAddress);

        // ดึง Tax ID ของผู้ขาย
        String sellerTaxId = extractFromSection(sellerSection, "เลขประจำตัวผู้เสียภาษีอากร[\\s]+([0-9]{12,13})");
        if (sellerTaxId == null) {
            sellerTaxId = extractFromSection(sellerSection, "Tax[\\s]+ID[\\s:]+([0-9]{12,13})");
        }
        seller.put("tax_id", sellerTaxId);

        // ดึงเบอร์ติดต่อ
        String contact = extractFromSection(sellerSection, "(?:ติดต่อ|Tel|โทร|Contact)[\\s:]+([0-9\\-]{8,15})");
        seller.put("contact", contact);

        result.set("seller", seller);

        // ข้อมูลผู้ซื้อ (Buyer/Customer)
        ObjectNode buyer = objectMapper.createObjectNode();

        // ดึงส่วน buyer section
        String buyerSection = extractSection(text, "รายละเอียดลูกค้า", "table");
        if (buyerSection == null) {
            buyerSection = extractSection(text, "Buyer", "Items");
        }
        if (buyerSection == null) {
            buyerSection = extractSection(text, "ลูกค้า", "รายการสินค้า");
        }

        if (buyerSection != null) {
            // ดึงชื่อลูกค้า - หลายรูปแบบ
            String buyerName = extractFromSection(buyerSection, "\\*\\*ลูกค้า[\\s]+([^*\\n]+)\\*\\*");
            if (buyerName == null) {
                buyerName = extractFromSection(buyerSection, "ลูกค้า[\\s]+([\\u0E00-\\u0E7Fa-zA-Z0-9\\s]+?)(?=\\n)");
            }
            if (buyerName == null) {
                buyerName = extractFromSection(buyerSection, "Customer[\\s:]+([A-Za-z\\u0E00-\\u0E7F0-9\\s]+?)(?=\\n)");
            }
            buyer.put("name", buyerName);

            // ดึงที่อยู่ลูกค้า - รองรับหลายรูปแบบ
            String buyerAddress = extractFromSection(buyerSection,
                    "([0-9]+/[0-9]+[^\\n]+?(?:กทม\\.|กรุงเทพมหานคร|จังหวัด)[^0-9]*[0-9]{5})");
            if (buyerAddress == null) {
                buyerAddress = extractFromSection(buyerSection, "\\n([0-9]+/[0-9]+[^\\n]+)");
            }
            buyer.put("address", buyerAddress);

            // ดึง Tax ID ของลูกค้า
            String buyerTaxId = extractFromSection(buyerSection, "เลขประจำตัวผู้เสียภาษีอากร[\\s]+([0-9]{13})");
            if (buyerTaxId == null) {
                buyerTaxId = extractFromSection(buyerSection, "Tax[\\s]+ID[\\s:]+([0-9]{13})");
            }
            buyer.put("tax_id", buyerTaxId);
        } else {
            buyer.putNull("name");
            buyer.putNull("address");
            buyer.putNull("tax_id");
        }

        result.set("buyer", buyer);

        // ยอดเงิน (Payment Summary)
        ObjectNode payment = objectMapper.createObjectNode();

        // ดึงยอดเงินทั้งหมด - รองรับหลายรูปแบบ
        String subtotal = extractAfter(text, "ทั้งหมด", "[0-9,]+\\.[0-9]{2}");
        if (subtotal == null) {
            subtotal = extractAfter(text, "Subtotal", "[0-9,]+\\.[0-9]{2}");
        }
        if (subtotal == null) {
            subtotal = extractAfter(text, "มูลค่าสินค้า", "[0-9,]+\\.[0-9]{2}");
        }
        payment.put("subtotal", subtotal != null ? subtotal : "0.00");

        // ดึงส่วนลด
        String discount = extractAfter(text, "ส่วนลด", "[0-9,]+\\.[0-9]{2}");
        if (discount == null) {
            discount = extractAfter(text, "Discount", "[0-9,]+\\.[0-9]{2}");
        }
        payment.put("discount", discount != null ? discount : "0.00");

        // ดึงยอดก่อน VAT (มูลค่าก่อนภาษี/ค่าสินค้า)
        String beforeVat = extractAfter(text, "รวมราคาสุทธิ", "[0-9,]+\\.[0-9]{2}");
        if (beforeVat == null) {
            beforeVat = extractAfter(text, "ค่าสินค้า", "[0-9,]+\\.[0-9]{2}");
        }
        if (beforeVat == null) {
            beforeVat = extractAfter(text, "มูลค่าก่อนภาษี", "[0-9,]+\\.[0-9]{2}");
        }
        if (beforeVat == null) {
            beforeVat = extractAfter(text, "Amount before", "[0-9,]+\\.[0-9]{2}");
        }
        // ถ้ายังไม่เจอ ให้ใช้ค่า subtotal - discount
        if (beforeVat == null && subtotal != null) {
            beforeVat = subtotal;
        }
        payment.put("amount_before_tax", beforeVat != null ? beforeVat : "0.00");

        // ดึง VAT amount - รองรับหลายรูปแบบ
        String vat = extractAfter(text, "ภาษีมูลค่าเพิ่ม", "[0-9,]+\\.[0-9]{2}");
        if (vat == null) {
            vat = extractAfter(text, "VAT", "[0-9,]+\\.[0-9]{2}");
        }
        if (vat == null) {
            vat = extractAfter(text, "7%", "[0-9,]+\\.[0-9]{2}");
        }
        payment.put("vat_amount", vat != null ? vat : "0.00");

        // ดึง VAT rate
        String vatRate = extractSimple(text, "(?:ภาษีมูลค่าเพิ่ม|VAT)[\\s]*([0-9]+)%");
        if (vatRate != null) {
            payment.put("vat_rate", vatRate + "%");
        } else {
            payment.put("vat_rate", "7%");
        }

        // ดึงยอดรวมสุทธิ (ราคารวมภาษี/Net Amount)
        String total = extractAfter(text, "ราคาไม่รวมภาษีมูลค่าเพิ่ม", "[0-9,]+\\.[0-9]{2}");
        if (total == null) {
            total = extractAfter(text, "ราคารวมภาษีมูลค่าเพิ่ม", "[0-9,]+\\.[0-9]{2}");
        }
        if (total == null) {
            total = extractAfter(text, "รวมเงิน", "[0-9,]+\\.[0-9]{2}");
        }
        if (total == null) {
            total = extractAfter(text, "Net Amount", "[0-9,]+\\.[0-9]{2}");
        }
        if (total == null) {
            total = extractAfter(text, "Total", "[0-9,]+\\.[0-9]{2}");
        }
        if (total == null) {
            total = extractAfter(text, "Grand Total", "[0-9,]+\\.[0-9]{2}");
        }
        payment.put("total_amount", total != null ? total : "0.00");

        result.set("payment", payment);

        // หมายเหตุ - ดึงเนื้อหาหลังคำว่า "หมายเหตุ" (ถ้ามี)
        String remarksSection = extractSection(text, "หมายเหตุ", "ทั้งหมด");
        String remarks = null;
        if (remarksSection != null && remarksSection.length() > 20) {
            // ถ้ามีเนื้อหาหลังคำว่า "หมายเหตุ" มากกว่า 20 ตัวอักษร
            remarks = remarksSection.replaceFirst("(?i)หมายเหตุ[\\s:]*", "").trim();
            if (remarks.isEmpty() || remarks.equals("หมายเหตุ")) {
                remarks = null;
            }
        }
        result.put("remarks", remarks);

        return result.toString();
    }

    // Helper methods ใหม่ที่ใช้งานง่ายกว่า
    private String extractSimple(String text, String pattern) {
        if (text == null)
            return null;
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group().trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractAfter(String text, String keyword, String pattern) {
        if (text == null)
            return null;
        try {
            int pos = text.indexOf(keyword);
            if (pos >= 0) {
                String substr = text.substring(pos);
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(substr);
                if (m.find()) {
                    return m.group().trim();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractSection(String text, String startKeyword, String endKeyword) {
        if (text == null)
            return null;
        try {
            int startPos = text.indexOf(startKeyword);
            if (startPos < 0)
                return null;

            int endPos = text.indexOf(endKeyword, startPos + startKeyword.length());
            if (endPos < 0) {
                // ถ้าไม่เจอ end keyword ให้เอาไปจนสุดท้าย (จำกัดที่ 1000 ตัวอักษร)
                endPos = Math.min(startPos + 1000, text.length());
            }

            return text.substring(startPos, endPos);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFromSection(String section, String pattern) {
        if (section == null)
            return null;
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(section);
            if (m.find()) {
                // ถ้ามี capturing group ให้ return group แรก
                if (m.groupCount() >= 1) {
                    return m.group(1).trim();
                }
                // ถ้าไม่มี capturing group ให้ return ทั้งหมด
                return m.group().trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

}