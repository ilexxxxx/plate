package com.dataprocess.platform.controller;

import com.dataprocess.platform.model.Notice;
import com.dataprocess.platform.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notices")
    public List<Notice> notices() {
        return noticeService.list();
    }
}
