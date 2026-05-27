package com.dataprocess.platform.controller;

import com.dataprocess.platform.dto.StatisticsSummary;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.service.ReportExportService;
import com.dataprocess.platform.service.StatisticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StatisticsController extends BaseController {
    private final StatisticsService statisticsService;
    private final PlatformRepository repository;
    private final ReportExportService reportExportService;
    private final ObjectMapper objectMapper;

    public StatisticsController(StatisticsService statisticsService,
                                PlatformRepository repository,
                                ReportExportService reportExportService,
                                ObjectMapper objectMapper) {
        this.statisticsService = statisticsService;
        this.repository = repository;
        this.reportExportService = reportExportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/statistics")
    public String page(@RequestParam(required = false) Long batchId, Model model) throws JsonProcessingException {
        StatisticsSummary summary = statisticsService.summary(batchId);
        model.addAttribute("summary", summary);
        model.addAttribute("batches", repository.listBatches());
        model.addAttribute("reports", reportExportService.listReports().stream().limit(8).toList());
        model.addAttribute("lineLabelsJson", objectMapper.writeValueAsString(summary.recentBatchLabels()));
        model.addAttribute("lineRowsJson", objectMapper.writeValueAsString(summary.recentBatchRows()));
        model.addAttribute("sourceLabelsJson", objectMapper.writeValueAsString(summary.sourceTypeCounts().keySet()));
        model.addAttribute("sourceValuesJson", objectMapper.writeValueAsString(summary.sourceTypeCounts().values()));
        model.addAttribute("businessLabelsJson", objectMapper.writeValueAsString(summary.businessTypeCounts().keySet()));
        model.addAttribute("businessValuesJson", objectMapper.writeValueAsString(summary.businessTypeCounts().values()));
        return "statistics";
    }

    @PostMapping("/statistics/export")
    public String export(@RequestParam(required = false) Long batchId,
                         @RequestParam String scope,
                         @RequestParam String format,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) throws Exception {
        reportExportService.export(batchId, scope, format, currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "导出任务已完成");
        return "redirect:/statistics";
    }
}
