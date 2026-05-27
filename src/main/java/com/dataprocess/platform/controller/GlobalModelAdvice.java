package com.dataprocess.platform.controller;

import com.dataprocess.platform.model.SysUser;
import com.dataprocess.platform.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    private final NoticeService noticeService;

    public GlobalModelAdvice(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @ModelAttribute("notices")
    public Object notices() {
        return noticeService.list();
    }

    @ModelAttribute("currentUser")
    public SysUser currentUser(HttpSession session) {
        Object user = session.getAttribute("loginUser");
        return user instanceof SysUser sysUser ? sysUser : null;
    }
}
