package com.dataprocess.platform.service;

import com.dataprocess.platform.model.SystemLog;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemLogService {
    private final PlatformRepository repository;

    public SystemLogService(PlatformRepository repository) {
        this.repository = repository;
    }

    public void log(String user, String action, String target, String result) {
        repository.addLog(user == null ? "system" : user, action, target, result);
    }

    public List<SystemLog> recent(int limit) {
        return repository.recentLogs(limit);
    }
}
