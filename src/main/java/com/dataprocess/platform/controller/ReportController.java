package com.dataprocess.platform.controller;

import com.dataprocess.platform.model.StatReport;
import com.dataprocess.platform.service.ReportExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class ReportController extends BaseController {
    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @PostMapping("/reports/export")
    public String export(@RequestParam(required = false) Long batchId,
                         @RequestParam(defaultValue = "original") String scope,
                         @RequestParam(defaultValue = "csv") String format,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) throws Exception {
        reportExportService.export(batchId, scope, format, currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "报告已导出");
        return "redirect:/statistics";
    }

    @GetMapping("/reports/download/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable long id) throws IOException {
        StatReport report = reportExportService.findReport(id).orElseThrow();
        Path path = Path.of(report.filePath());
        InputStream inputStream = Files.newInputStream(path);
        String fileName = URLEncoder.encode(path.getFileName().toString(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentLength(Files.size(path))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }
}
