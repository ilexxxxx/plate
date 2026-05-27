package com.dataprocess.platform.controller;

import com.dataprocess.platform.service.LedgerService;
import com.dataprocess.platform.service.ReportExportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LedgerController {
    private final LedgerService ledgerService;
    private final ReportExportService reportExportService;

    public LedgerController(LedgerService ledgerService, ReportExportService reportExportService) {
        this.ledgerService = ledgerService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/ledger")
    public String page(@RequestParam(required = false) String businessType,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        model.addAttribute("cards", ledgerService.cards());
        model.addAttribute("batches", ledgerService.batches(businessType, keyword));
        model.addAttribute("businessType", businessType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("reports", reportExportService.listReports());
        return "ledger";
    }
}
