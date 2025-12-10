package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Safelist;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final String TEMPLATE_NAME = "meeting_report";
    private static final String PDF_FILENAME = "meeting-report.pdf";
    private static final String RESOURCES_PATH = "src/main/resources/";

    @Autowired
    private TemplateEngine templateEngine;

    @PostMapping("/generate-ebook")
    public ResponseEntity<?> generateEbook(@RequestBody MeetingRequest data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Context context = buildContext(data);

            // Generate HTML
            String htmlContent = generateHtmlContent(context);

            // --- DEBUG: ปริ้น HTML ออกมาดูว่า Tag li ถูกปิดครบไหม ---
            System.out.println("DEBUG HTML CONTENT:\n" + htmlContent);
            // -----------------------------------------------------

            byte[] pdfBytes = generatePdfFromHtml(htmlContent, outputStream);

            return ResponseEntity.ok()
                    .headers(createPdfHeaders())
                    .body(pdfBytes);

        } catch (Exception e) {
            return handleError(e);
        }
    }

    private Context buildContext(MeetingRequest data) {
        Context context = new Context();

        setHeaderVariables(context, data);
        setAgendaVariables(context, data);
        setResolutionVariables(context, data);

        return context;
    }

    private void setHeaderVariables(Context context, MeetingRequest data) {
        context.setVariable("meetingTitle",
                data.getMeetingTitle() != null ? data.getMeetingTitle().trim() : "");
        context.setVariable("meetingNo",
                data.getMeetingNo() != null ? data.getMeetingNo() : "-");
        context.setVariable("location",
                data.getLocation() != null ? data.getLocation().trim() : "-");
        context.setVariable("meetingTime",
                data.getMeetingTime() != null ? data.getMeetingTime().toString().substring(0, 5) : "-");
        context.setVariable("formattedDate", formatThaiDate(data.getMeetingDate()));
        context.setVariable("attendees", data.getAttendees());
    }

    private void setAgendaVariables(Context context, MeetingRequest data) {
        context.setVariable("agendaOneSubAgendas", parseSubAgendas(data.getAgendaOneData()));
        context.setVariable("agendaTwoSubAgendas", parseSubAgendas(data.getAgendaTwoData()));
        context.setVariable("agendaThreeSubAgendas", parseSubAgendas(data.getAgendaThreeData()));

        context.setVariable("agendaOne", parseGenericText(data.getAgendaOneData()));
        context.setVariable("agendaTwo", parseGenericText(data.getAgendaTwoData()));
        context.setVariable("agendaThree", parseGenericText(data.getAgendaThreeData()));

        context.setVariable("agendaFourItems", parseItems(data.getAgendaFourData()));
        context.setVariable("agendaFourAssets", parseAssets(data.getAgendaFourData()));
        context.setVariable("agendaFiveItems", parseItems(data.getAgendaFiveData()));
        context.setVariable("agendaFiveAssets", parseAssets(data.getAgendaFiveData()));
    }

    private void setResolutionVariables(Context context, MeetingRequest data) {
        boolean hasResolution = hasAnyResolution(data);

        context.setVariable("hasResolution", hasResolution);
        context.setVariable("resDetail", parseResolutionText(data.getResolutionDetail()));
        context.setVariable("resFour", parseResolutionText(data.getResolutionFourData()));
        context.setVariable("resFive", parseResolutionText(data.getResolutionFiveData()));
    }

    private boolean hasAnyResolution(MeetingRequest data) {
        return (data.getResolutionDetail() != null && !data.getResolutionDetail().isEmpty()) ||
                (data.getResolutionFourData() != null && !data.getResolutionFourData().isEmpty()) ||
                (data.getResolutionFiveData() != null && !data.getResolutionFiveData().isEmpty());
    }

    private String formatThaiDate(java.time.LocalDate date) {
        if (date == null) {
            return "-";
        }
        Locale localeTH = new Locale.Builder().setLanguage("th").setRegion("TH").build();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", localeTH);
        return date.format(formatter);
    }

    private String generateHtmlContent(Context context) {
        String html = templateEngine.process(TEMPLATE_NAME, context);
        return sanitizeHtml(html);
    }

    private String sanitizeHtml(String html) {
        try {
            // Parse the entire HTML document with Jsoup
            Document document = Jsoup.parse(html);

            // Set output to XHTML format
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            document.outputSettings().escapeMode(EscapeMode.xhtml);
            document.outputSettings().charset("UTF-8");

            // Get the full HTML with properly closed tags
            String cleanHtml = document.html();

            // Remove empty paragraph and div tags that might cause issues
            cleanHtml = cleanHtml.replaceAll("<p>\\s*</p>", "");
            cleanHtml = cleanHtml.replaceAll("<p\\s*/>", "");
            cleanHtml = cleanHtml.replaceAll("<div>\\s*</div>", "");
            cleanHtml = cleanHtml.replaceAll("<div\\s*/>", "");

            return cleanHtml;
        } catch (Exception e) {
            e.printStackTrace();
            return html;
        }
    }

    private byte[] generatePdfFromHtml(String htmlContent, ByteArrayOutputStream outputStream) throws Exception {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        configurePdfBuilder(builder, htmlContent);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }

    private String convertToXhtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        try {
            // 1. Parse HTML ด้วย Jsoup เป็น Document ก่อน (ไม่ clean ก่อน)
            Document document = Jsoup.parseBodyFragment(html);

            // 2. ตั้งค่า output เป็น XML (XHTML) syntax
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            document.outputSettings().escapeMode(EscapeMode.xhtml);
            document.outputSettings().charset("UTF-8");
            document.outputSettings().prettyPrint(true);

            // 3. ดึง HTML ออกมา (Jsoup จะปิด tag ให้อัตโนมัติ)
            String finalHtml = document.body().html();

            // 4. ลบ empty tags ที่ไม่จำเป็น
            finalHtml = finalHtml.replaceAll("<p>\\s*</p>", "");
            finalHtml = finalHtml.replaceAll("<p\\s*/>", "");
            finalHtml = finalHtml.replaceAll("<div>\\s*</div>", "");
            finalHtml = finalHtml.replaceAll("<div\\s*/>", "");

            // 5. เช็คกรณีพิเศษ: ถ้าขึ้นต้นด้วย <li> แต่ไม่มี <ul> หรือ <ol> ครอบ
            if (finalHtml.trim().startsWith("<li") && !finalHtml.contains("<ul") && !finalHtml.contains("<ol")) {
                finalHtml = "<ul>" + finalHtml + "</ul>";
            }

            return finalHtml;

        } catch (Exception e) {
            e.printStackTrace();
            // กรณี error ให้ return HTML ที่ escape แล้ว
            return Jsoup.clean(html, Safelist.relaxed());
        }
    }

    private void configurePdfBuilder(PdfRendererBuilder builder, String htmlContent) {
        builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
        builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
        builder.defaultTextDirection(BaseRendererBuilder.TextDirection.LTR);

        String baseUrl = new java.io.File(RESOURCES_PATH).toURI().toString();
        builder.withHtmlContent(htmlContent, baseUrl);
    }

    private HttpHeaders createPdfHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", PDF_FILENAME);
        return headers;
    }

    private ResponseEntity<?> handleError(Exception e) {
        e.printStackTrace();
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "PDF Generation Failed");
        errorResponse.put("message", e.getMessage());
        // เพิ่ม message ย่อยถ้ามี
        if (e.getCause() != null) {
            errorResponse.put("cause", e.getCause().getMessage());
        }
        return ResponseEntity.status(500).body(errorResponse);
    }

    private List<Map<String, String>> parseItems(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null || json.isEmpty())
            return result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.has("items") && root.get("items").isArray()) {
                for (JsonNode item : root.get("items")) {
                    Map<String, String> map = new HashMap<>();
                    map.put("order", item.path("order").asText("-"));
                    map.put("name", item.path("name").asText("-").trim());
                    map.put("region", item.path("region").asText("-").trim());
                    result.add(map);
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private List<Map<String, String>> parseAssets(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null || json.isEmpty())
            return result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.has("dialogData") && root.get("dialogData").isArray()) {
                int index = 1;
                DecimalFormat df = new DecimalFormat("#,##0.00");
                for (JsonNode item : root.get("dialogData")) {
                    Map<String, String> map = new HashMap<>();
                    map.put("no", String.valueOf(index++));
                    map.put("fileNo", item.path("fileNo").asText("-").trim());
                    map.put("asset", item.path("asset").asText("-").trim());
                    String amountStr = item.path("amount").asText("0");
                    try {
                        double amount = Double.parseDouble(amountStr.replace(",", ""));
                        map.put("amount", df.format(amount));
                    } catch (Exception ex) {
                        map.put("amount", amountStr);
                    }
                    String status = item.path("status").asText("");
                    map.put("status", mapStatus(status));
                    result.add(map);
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private String mapStatus(String status) {
        switch (status) {
            case "seize":
                return "ยึดทรัพย์";
            case "reject":
                return "ยกคำร้อง";
            case "pending":
                return "รอตรวจสอบ";
            default:
                return status;
        }
    }

    private String parseGenericText(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return "-ไม่มีรายละเอียด-";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            if (root.has("subAgendas")) {
                ArrayNode subAgendas = (ArrayNode) root.get("subAgendas");
                StringBuilder sb = new StringBuilder();

                for (JsonNode sub : subAgendas) {
                    if (sub.has("detail")) {
                        sb.append(sub.get("detail").asText());
                    }
                }
                if (sb.length() == 0) {
                    return "-ไม่มีรายละเอียด-";
                }
                return convertToXhtml(sb.toString());
            }
            // กรณีไม่ใช่ subAgendas ผ่าน Jsoup ด้วย
            return convertToXhtml(json);
        } catch (Exception e) {
            // กรณี Parse JSON ไม่ได้ ผ่าน Jsoup ด้วย
            return convertToXhtml(json);
        }
    }

    private List<Map<String, String>> parseSubAgendas(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null || json.isEmpty())
            return result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.has("subAgendas") && root.get("subAgendas").isArray()) {
                int idx = 1;
                for (JsonNode sub : root.get("subAgendas")) {
                    Map<String, String> map = new HashMap<>();
                    String detail = sub.path("detail").asText("-");

                    if (detail != null && !detail.equals("-")) {
                        detail = detail.replaceAll("\\r?\\n", " ").trim();
                        detail = convertToXhtml(detail);
                    }
                    map.put("detail", detail);
                    map.put("order", String.valueOf(idx++));
                    result.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String parseResolutionText(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (json.trim().startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);
                if (root.has("detail")) {
                    return convertToXhtml(root.get("detail").asText());
                }
            } catch (Exception e) {
            }
        }
        // กรณีไม่ใช่ JSON ผ่าน Jsoup ด้วย
        return convertToXhtml(json);
    }
}