package com.dataprocess.platform.controller;

import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.service.QualityCheckService;
import com.dataprocess.platform.service.ReportExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class QualityCheckController extends BaseController {
    private final QualityCheckService qualityCheckService;
    private final PlatformRepository repository;
    private final ReportExportService reportExportService;

    public QualityCheckController(QualityCheckService qualityCheckService,
                                  PlatformRepository repository,
                                  ReportExportService reportExportService) {
        this.qualityCheckService = qualityCheckService;
        this.repository = repository;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/quality")
    public String page(@RequestParam(required = false) Long batchId, Model model) {
        model.addAttribute("batches", repository.listBatches());
        model.addAttribute("selectedBatchId", batchId);
        model.addAttribute("items", qualityCheckService.latestItems(batchId));
        model.addAttribute("reports", reportExportService.listReports().stream().limit(8).toList());
        return "quality";
    }

    @PostMapping("/quality/check")
    public String check(@RequestParam(required = false) Long batchId, HttpSession session, RedirectAttributes redirectAttributes) {
        qualityCheckService.runFullCheck(batchId, currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "一键全检已完成");
        return "redirect:/quality" + (batchId == null ? "" : "?batchId=" + batchId);
    }

    @PostMapping("/quality/report")
    public String report(@RequestParam(required = false) Long batchId,
                         @RequestParam String format,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) throws Exception {
        reportExportService.export(batchId, "quality", format, currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "质检报告已生成");
        return "redirect:/quality";
    }
}
