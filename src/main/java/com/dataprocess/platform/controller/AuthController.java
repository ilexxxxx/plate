package com.dataprocess.platform.controller;

import com.dataprocess.platform.dto.OperationResult;
import com.dataprocess.platform.model.SysUser;
import com.dataprocess.platform.service.AuthService;
import com.dataprocess.platform.service.SystemLogService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController extends BaseController {
    private final AuthService authService;
    private final SystemLogService logService;

    public AuthController(AuthService authService, SystemLogService logService) {
        this.authService = authService;
        this.logService = logService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        return currentUser(session) == null ? "redirect:/login" : "redirect:/aggregation";
    }

    @GetMapping("/login")
    public String login(@CookieValue(value = "remember_username", required = false) String rememberedUsername,
                        @CookieValue(value = "remember_password", required = false) String rememberedPassword,
                        Model model) {
        model.addAttribute("rememberedUsername", rememberedUsername == null ? "" : rememberedUsername);
        model.addAttribute("rememberedPassword", rememberedPassword == null ? "" : rememberedPassword);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(defaultValue = "false") boolean remember,
                          HttpSession session,
                          HttpServletResponse response,
                          Model model) {
        OperationResult result = authService.login(username, password);
        if (!result.success()) {
            model.addAttribute("error", result.message());
            model.addAttribute("rememberedUsername", username);
            model.addAttribute("rememberedPassword", password);
            return "login";
        }
        SysUser user = authService.user(username);
        session.setAttribute("loginUser", user);
        remember(response, "remember_username", remember ? username : "", remember);
        remember(response, "remember_password", remember ? password : "", remember);
        return "redirect:/aggregation";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam(defaultValue = "false") boolean agreed,
                             Model model) {
        OperationResult result = authService.register(username, password, confirmPassword, agreed);
        if (!result.success()) {
            model.addAttribute("error", result.message());
            model.addAttribute("username", username);
            return "register";
        }
        model.addAttribute("message", result.message());
        return "login";
    }

    @GetMapping("/forgot")
    public String forgot() {
        return "forgot";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        String username = currentUsername(session);
        session.invalidate();
        logService.log(username, "退出", "账号", "成功");
        return "redirect:/login";
    }

    private void remember(HttpServletResponse response, String name, String value, boolean remember) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(remember ? 7 * 24 * 3600 : 0);
        response.addCookie(cookie);
    }
}
