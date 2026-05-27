package com.dataprocess.platform.controller;

import com.dataprocess.platform.dto.DashboardSummary;
import com.dataprocess.platform.dto.OperationResult;
import com.dataprocess.platform.dto.StatisticsSummary;
import com.dataprocess.platform.dto.UploadPreview;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AggregationController extends BaseController {
    private final DataSourceService dataSourceService;
    private final FileImportService fileImportService;
    private final PlatformRepository repository;
    private final StatisticsService statisticsService;
    private final SystemLogService logService;
    private final ReportExportService reportExportService;

    public AggregationController(DataSourceService dataSourceService,
                                 FileImportService fileImportService,
                                 PlatformRepository repository,
                                 StatisticsService statisticsService,
                                 SystemLogService logService,
                                 ReportExportService reportExportService) {
        this.dataSourceService = dataSourceService;
        this.fileImportService = fileImportService;
        this.repository = repository;
        this.statisticsService = statisticsService;
        this.logService = logService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/aggregation")
    public String page(Model model) {
        StatisticsSummary statistics = statisticsService.summary(null);
        model.addAttribute("summary", new DashboardSummary(
                repository.count("SELECT COUNT(*) FROM data_source"),
                repository.count("SELECT COUNT(*) FROM import_batch"),
                repository.count("SELECT COUNT(*) FROM import_record"),
                statistics.fieldCount(),
                statistics.qualityPassRate()));
        model.addAttribute("sources", dataSourceService.list());
        model.addAttribute("batches", repository.listBatches().stream().limit(8).toList());
        model.addAttribute("logs", logService.recent(10));
        model.addAttribute("reports", reportExportService.listReports().stream().limit(5).toList());
        model.addAttribute("businessTypes", businessTypes());
        return "aggregation";
    }

    @PostMapping("/aggregation/sources")
    public String addSource(@RequestParam String sourceName,
                            @RequestParam String sourceType,
                            @RequestParam String connInfo,
                            HttpSession session) {
        dataSourceService.add(sourceName, sourceType, connInfo, currentUsername(session));
        return "redirect:/aggregation";
    }

    @PostMapping("/aggregation/sources/{id}/refresh")
    public String refreshSource(@PathVariable long id, HttpSession session) {
        dataSourceService.refresh(id, currentUsername(session));
        return "redirect:/aggregation";
    }

    @ResponseBody
    @PostMapping("/aggregation/import/preview")
    public UploadPreview preview(@RequestParam MultipartFile file,
                                 @RequestParam(required = false) Long sourceId,
                                 @RequestParam(defaultValue = "系统") String businessType,
                                 HttpSession session) throws Exception {
        return fileImportService.preview(file, sourceId, businessType, currentUsername(session));
    }

    @ResponseBody
    @PostMapping("/aggregation/import/confirm")
    public OperationResult confirm(@RequestParam String token, HttpSession session) {
        long batchId = fileImportService.confirm(token, currentUsername(session));
        return OperationResult.success("导入成功，批次编号：" + batchId);
    }

    private String[] businessTypes() {
        return new String[]{"用户", "订单", "产品", "运营", "客服", "系统"};
    }
}
