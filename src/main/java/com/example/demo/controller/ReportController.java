package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private PdfFont fontRegular;
    private PdfFont fontBold;

    @PostMapping("/generate-ebook")
    public ResponseEntity<?> generateEbook(@RequestBody MeetingRequest data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // สร้าง PdfWriter + PdfDocument + Document
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);

            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(60, 55, 60, 55);

            initFonts();

            // HEADER
            String title = data.getMeetingTitle();
            addCenterTitle(document, title, fontBold, 14);
            addCenterTitle(document,
                    "ครั้งที่ " + (data.getMeetingNo() != null ? data.getMeetingNo() : "-"),
                    fontBold, 14);

            String dateStr = "-";
            if (data.getMeetingDate() != null) {
                Locale localeTH = new Locale.Builder()
                        .setLanguage("th")
                        .setRegion("TH")
                        .build();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", localeTH);

                dateStr = data.getMeetingDate().format(formatter);
            }

            String dateTimeLocationText = "เมื่อวันที่ " + dateStr +
                    " เวลา " + (data.getMeetingTime() != null
                            ? data.getMeetingTime().toString().substring(0, 5) + " น."
                            : "-")
                    +
                    " ณ " + (data.getLocation() != null ? data.getLocation() : "-");

            addCenterParagraph(document, dateTimeLocationText, fontRegular);

            // ผู้มาประชุม
            addSectionHeader(document, "ผู้มาประชุม", fontBold);

            if (data.getAttendees() != null && !data.getAttendees().isEmpty()) {
                int i = 1;
                for (Map<String, Object> attendee : data.getAttendees()) {
                    String prename = (String) attendee.getOrDefault("prename", "");
                    String firstname = (String) attendee.getOrDefault("firstname", "");
                    String lastname = (String) attendee.getOrDefault("lastname", "");
                    String position = (String) attendee.getOrDefault("department", "");
                    if (position.isEmpty()) {
                        position = (String) attendee.getOrDefault("affiliation", "");
                    }

                    String fullName = i++ + ". " + prename + firstname + " " + lastname;
                    if (!position.isEmpty())
                        fullName += " (" + position + ")";

                    addIndentedParagraph(document, fullName, fontRegular);
                }
            } else {
                addIndentedParagraph(document, "-", fontRegular);
            }

            // วาระต่าง ๆ
            addAgendaSectionStyled(document, "ระเบียบวาระที่ ๑",
                    extractTextFromJson(data.getAgendaOneData()), fontBold, fontRegular);

            addAgendaSectionStyled(document, "ระเบียบวาระที่ ๒",
                    extractTextFromJson(data.getAgendaTwoData()), fontBold, fontRegular);

            addAgendaSectionStyled(document, "ระเบียบวาระที่ ๓",
                    extractTextFromJson(data.getAgendaThreeData()), fontBold, fontRegular);

            addAgendaTable(document, "ระเบียบวาระที่ ๔",
                    data.getAgendaFourData(), fontBold, fontRegular);

            addAgendaTable(document, "ระเบียบวาระที่ ๕",
                    data.getAgendaFiveData(), fontBold, fontRegular);

            // มติที่ประชุม
            if (data.getResolutionDetail() != null
                    || data.getResolutionFourData() != null
                    || data.getResolutionFiveData() != null) {
                addSectionHeader(document, "มติที่ประชุม", fontBold);

                if (data.getResolutionDetail() != null) {
                    String resDetail = parseResolutionText(data.getResolutionDetail());
                    addIndentedParagraph(document, resDetail, fontRegular);
                }

                if (data.getResolutionFourData() != null)
                    addIndentedParagraph(document, "วาระที่ 4 " + data.getResolutionFourData(), fontRegular);

                if (data.getResolutionFiveData() != null)
                    addIndentedParagraph(document, "วาระที่ 5 " + data.getResolutionFiveData(), fontRegular);
            }

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "meeting-report.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    private void initFonts() throws Exception {
        ClassPathResource regularRes = new ClassPathResource("fonts/Sarabun-Regular.ttf");
        ClassPathResource boldRes = new ClassPathResource("fonts/Sarabun-Bold.ttf");

        // โหลดฟอนต์แบบ embed เต็มรูปแบบเพื่อรองรับสระและวรรณยุกต์ภาษาไทย
        // ใช้ IDENTITY_H encoding และ PREFER_EMBEDDED เพื่อรองรับ OpenType features
        fontRegular = PdfFontFactory.createFont(
                regularRes.getInputStream().readAllBytes(),
                PdfEncodings.IDENTITY_H,
                EmbeddingStrategy.PREFER_EMBEDDED);

        fontBold = PdfFontFactory.createFont(
                boldRes.getInputStream().readAllBytes(),
                PdfEncodings.IDENTITY_H,
                EmbeddingStrategy.PREFER_EMBEDDED);
    }

    private void addCenterTitle(Document doc, String text, PdfFont font, int fontSize) {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(fontSize)
                .setFixedLeading(fontSize + 2)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2)
                .setMarginBottom(2);
        doc.add(p);
    }

    private void addCenterParagraph(Document doc, String text, PdfFont font) {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(11)
                .setFixedLeading(13)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        doc.add(p);
    }

    private void addIndentedParagraph(Document doc, String text, PdfFont font) {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(11)
                .setFixedLeading(13)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(2)
                .setMarginLeft(15);
        doc.add(p);
    }

    private void addSectionHeader(Document doc, String text, PdfFont font) {
        Paragraph p = new Paragraph(text)
                .setFont(font)
                .setFontSize(11)
                .setMarginTop(10)
                .setMarginBottom(5);
        doc.add(p);
    }

    private void addAgendaSectionStyled(Document doc, String title, String content,
            PdfFont headerFont, PdfFont bodyFont) {

        Paragraph header = new Paragraph(title)
                .setFont(headerFont)
                .setFontSize(11)
                .setMarginTop(6)
                .setMarginBottom(3);
        doc.add(header);

        String[] lines = content.split("\n");

        for (String line : lines) {
            Paragraph body = new Paragraph(line)
                    .setFont(bodyFont)
                    .setFontSize(11)
                    .setFixedLeading(13)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginLeft(15)
                    .setMarginBottom(3);

            doc.add(body);
        }
    }

    private void addAgendaTable(Document doc, String agendaTitle, String json,
            PdfFont headerFont, PdfFont bodyFont) {
        Paragraph header = new Paragraph(agendaTitle)
                .setFont(headerFont)
                .setFontSize(11)

                .setMarginTop(10)
                .setMarginBottom(5);
        doc.add(header);

        if (json == null || json.isEmpty() || json.equals("{}")) {
            addIndentedParagraph(doc, "-ไม่มีรายละเอียด-", bodyFont);
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            if (root.has("items")) {
                JsonNode items = root.get("items");

                if (items.isArray() && items.size() > 0) {
                    for (JsonNode item : items) {
                        String order = item.path("order").asText("-");
                        String name = item.path("name").asText("-");
                        String region = item.path("region").asText("");

                        String line = order + " " + name;
                        if (!region.isEmpty()) {
                            line += " (" + region + ")";
                        }

                        addIndentedParagraph(doc, line, bodyFont);
                    }
                }
            }

            if (!root.has("dialogData")) {
                addIndentedParagraph(doc, extractTextFromJson(json), bodyFont);
                return;
            }

            JsonNode dialogData = root.get("dialogData");

            if (!dialogData.isArray() || dialogData.size() == 0) {
                addIndentedParagraph(doc, "-ไม่มีรายการทรัพย์สิน-", bodyFont);
                return;
            }

            Table table = new Table(new float[] { 1, 3, 5, 3, 3 });
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(5);

            addTableHeader(table, "ลำดับ", headerFont);
            addTableHeader(table, "เลขที่เอกสาร", headerFont);
            addTableHeader(table, "รายการทรัพย์สิน", headerFont);
            addTableHeader(table, "มูลค่า (บาท)", headerFont);
            addTableHeader(table, "สถานะ", headerFont);

            int i = 1;
            for (JsonNode item : dialogData) {
                addTableCell(table, String.valueOf(i++), bodyFont);
                addTableCell(table, item.path("fileNo").asText("-"), bodyFont);
                addTableCell(table, item.path("asset").asText("-"), bodyFont);
                addTableCell(table, item.path("amount").asText("-"), bodyFont);
                addTableCell(table, mapStatus(item.path("status").asText("-")), bodyFont);
            }

            doc.add(table);

        } catch (Exception e) {
            addIndentedParagraph(doc, "(Error parsing agenda)", bodyFont);
        }
    }

    private void addTableHeader(Table table, String text, PdfFont font) {
        Cell cell = new Cell().add(
                new Paragraph(text).setFont(font).setFontSize(11));
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setPadding(6);
        table.addHeaderCell(cell);
    }

    private void addTableCell(Table table, String text, PdfFont font) {
        Cell cell = new Cell().add(
                new Paragraph(text).setFont(font).setFontSize(10));
        cell.setPadding(5);
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);
    }

    private String extractTextFromJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty() || jsonString.equals("{}")) {
            return "-ไม่มีรายละเอียด-";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            if (!root.has("subAgendas"))
                return jsonString;

            JsonNode sub = root.get("subAgendas");
            if (!sub.isArray() || sub.size() == 0)
                return "-ไม่มีรายละเอียด-";

            StringBuilder sb = new StringBuilder();
            for (JsonNode item : sub) {
                String no = item.path("subAgendaNo").asText(""); // <== ใช้นี่
                String detail = item.path("detail").asText("");

                sb.append("วาระย่อยที่ ").append(no)
                        .append(" ").append(detail)
                        .append("\n");
            }
            return sb.toString().trim();

        } catch (Exception e) {
            return jsonString;
        }
    }

    private String parseResolutionText(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return root.has("detail")
                    ? root.get("detail").asText()
                    : json;
        } catch (Exception e) {
            return json;
        }
    }

    private String mapStatus(String status) {
        switch (status) {
            case "seize":
                return "ยึดทรัพย์";
            case "pending":
                return "รอตรวจสอบ";
            case "reject":
                return "ยกคำร้อง";
            default:
                return status;
        }
    }
}