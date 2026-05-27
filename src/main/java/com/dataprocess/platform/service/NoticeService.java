package com.dataprocess.platform.service;

import com.dataprocess.platform.model.Notice;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {
    private final PlatformRepository repository;

    public NoticeService(PlatformRepository repository) {
        this.repository = repository;
    }

    public List<Notice> list() {
        return repository.listNotices();
    }
}
