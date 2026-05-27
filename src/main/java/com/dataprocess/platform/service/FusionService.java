package com.dataprocess.platform.service;

import com.dataprocess.platform.model.CleanRuleConfig;
import com.dataprocess.platform.model.FusionRecord;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FusionService {
    private final DataCleanService dataCleanService;
    private final PlatformRepository repository;

    public FusionService(DataCleanService dataCleanService, PlatformRepository repository) {
        this.dataCleanService = dataCleanService;
        this.repository = repository;
    }

    public FusionRecord run(long batchId, String user) {
        return dataCleanService.cleanBatch(batchId, user);
    }

    public List<FusionRecord> recent() {
        return repository.listFusionRecords();
    }

    public CleanRuleConfig config() {
        return dataCleanService.getConfig();
    }

    public void saveConfig(CleanRuleConfig config, String user) {
        dataCleanService.saveConfig(config, user);
    }
}
