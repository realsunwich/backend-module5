package com.example.demo.controller;

import com.example.demo.dto.MeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// Import สำหรับจัดระเบียบภาษาไทย (Text Shaping)
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

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private TemplateEngine templateEngine;

    @PostMapping("/generate-ebook")
    public ResponseEntity<?> generateEbook(@RequestBody MeetingRequest data) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            Context context = new Context();

            // --- Header Info ---
            context.setVariable("meetingTitle", data.getMeetingTitle());
            context.setVariable("meetingNo", data.getMeetingNo() != null ? data.getMeetingNo() : "-");
            context.setVariable("location", data.getLocation());
            context.setVariable("meetingTime",
                    data.getMeetingTime() != null ? data.getMeetingTime().toString().substring(0, 5) : "-");

            String dateStr = "-";
            if (data.getMeetingDate() != null) {
                Locale localeTH = new Locale.Builder().setLanguage("th").setRegion("TH").build();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", localeTH);
                dateStr = data.getMeetingDate().format(formatter);
            }
            context.setVariable("formattedDate", dateStr);
            context.setVariable("attendees", data.getAttendees());

            // --- Agendas 1-3 (HTML Content) ---
            context.setVariable("agendaOne", parseGenericText(data.getAgendaOneData()));
            context.setVariable("agendaTwo", parseGenericText(data.getAgendaTwoData()));
            context.setVariable("agendaThree", parseGenericText(data.getAgendaThreeData()));

            // --- Agenda 4 ---
            context.setVariable("agendaFourItems", parseItems(data.getAgendaFourData()));
            context.setVariable("agendaFourAssets", parseAssets(data.getAgendaFourData()));

            // --- Agenda 5 ---
            context.setVariable("agendaFiveItems", parseItems(data.getAgendaFiveData()));
            context.setVariable("agendaFiveAssets", parseAssets(data.getAgendaFiveData()));

            // --- Resolutions ---
            boolean hasResolution = (data.getResolutionDetail() != null && !data.getResolutionDetail().isEmpty()) ||
                    (data.getResolutionFourData() != null && !data.getResolutionFourData().isEmpty()) ||
                    (data.getResolutionFiveData() != null && !data.getResolutionFiveData().isEmpty());

            context.setVariable("hasResolution", hasResolution);
            context.setVariable("resDetail", parseResolutionText(data.getResolutionDetail()));
            context.setVariable("resFour", parseResolutionText(data.getResolutionFourData()));
            context.setVariable("resFive", parseResolutionText(data.getResolutionFiveData()));

            // --- Process PDF ---
            String htmlContent = templateEngine.process("meeting_report", context);

            PdfRendererBuilder builder = new PdfRendererBuilder();

            // ใช้ Text Shaping สำหรับภาษาไทย
            builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
            builder.useUnicodeBidiReorderer(new ICUBidiReorderer());

            // กำหนดทิศทาง Default
            builder.defaultTextDirection(BaseRendererBuilder.TextDirection.LTR);

            String baseUrl = new java.io.File("src/main/resources/").toURI().toString();
            builder.withHtmlContent(htmlContent, baseUrl);
            builder.toStream(os);
            builder.run();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "meeting-report.pdf");

            return ResponseEntity.ok().headers(headers).body(os.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
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
                    map.put("name", item.path("name").asText("-"));
                    map.put("region", item.path("region").asText("-"));
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
                    map.put("fileNo", item.path("fileNo").asText("-"));
                    map.put("asset", item.path("asset").asText("-"));

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

    // --- จุดที่ปรับปรุง: ใส่ div class='agenda-item' ครอบรายการย่อย ---
    private String parseGenericText(String json) {
        if (json == null || json.isEmpty() || json.equals("{}"))
            return "-ไม่มีรายละเอียด-";
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.has("subAgendas") && root.get("subAgendas").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode sub : root.get("subAgendas")) {
                    String detail = sub.path("detail").asText("");

                    // ใช้ div เพื่อให้ CSS .agenda-item ทำงานได้ (ควบคุมระยะห่างบรรทัด)
                    sb.append("<div class='agenda-item'>").append(detail).append("</div>");
                }
                String res = sb.toString();
                return res.isEmpty() ? "-ไม่มีรายละเอียด-" : res;
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }

    private String parseResolutionText(String json) {
        if (json == null || json.isEmpty())
            return null;
        if (json.trim().startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);
                if (root.has("detail"))
                    return root.get("detail").asText();
            } catch (Exception e) {
            }
        }
        return json;
    }
}