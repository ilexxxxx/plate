package com.dataprocess.platform.controller;

import com.dataprocess.platform.model.CleanRuleConfig;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.service.FusionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FusionController extends BaseController {
    private final FusionService fusionService;
    private final PlatformRepository repository;

    public FusionController(FusionService fusionService, PlatformRepository repository) {
        this.fusionService = fusionService;
        this.repository = repository;
    }

    @GetMapping("/fusion")
    public String page(Model model) {
        model.addAttribute("batches", repository.listBatches());
        model.addAttribute("config", fusionService.config());
        model.addAttribute("records", fusionService.recent().stream().limit(12).toList());
        return "fusion";
    }

    @PostMapping("/fusion/rules")
    public String rules(@RequestParam(defaultValue = "false") boolean handleNulls,
                        @RequestParam(defaultValue = "false") boolean trimSpaces,
                        @RequestParam(defaultValue = "false") boolean removeDuplicates,
                        @RequestParam(defaultValue = "false") boolean normalizePhone,
                        @RequestParam(defaultValue = "false") boolean normalizeEmail,
                        @RequestParam(defaultValue = "false") boolean normalizeDate,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        fusionService.saveConfig(new CleanRuleConfig(1, handleNulls, trimSpaces, removeDuplicates,
                normalizePhone, normalizeEmail, normalizeDate, ""), currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "清洗规则已保存");
        return "redirect:/fusion";
    }

    @PostMapping("/fusion/run")
    public String run(@RequestParam long batchId, HttpSession session, RedirectAttributes redirectAttributes) {
        fusionService.run(batchId, currentUsername(session));
        redirectAttributes.addFlashAttribute("message", "批次 #" + batchId + " 清洗完成");
        return "redirect:/fusion";
    }
}
