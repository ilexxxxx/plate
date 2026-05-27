package com.dataprocess.platform.controller;

import com.dataprocess.platform.dto.OperationResult;
import com.dataprocess.platform.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController extends BaseController {
    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/account")
    public String page(HttpSession session, Model model) {
        model.addAttribute("user", currentUser(session));
        model.addAttribute("users", userService.listUsers());
        return "account";
    }

    @PostMapping("/account/reset")
    public String reset(@RequestParam long userId,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        OperationResult result = userService.resetPassword(currentUser(session), userId, password);
        redirectAttributes.addFlashAttribute(result.success() ? "message" : "error", result.message());
        return "redirect:/account";
    }
}
