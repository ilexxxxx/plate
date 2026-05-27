package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.CleanResult;
import com.dataprocess.platform.model.CleanRuleConfig;
import com.dataprocess.platform.model.FusionRecord;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.util.DataValueUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataCleanService {
    private final PlatformRepository repository;
    private final ObjectMapper objectMapper;
    private final SystemLogService logService;

    public DataCleanService(ObjectMapper objectMapper) {
        this(null, objectMapper, null);
    }

    @Autowired
    public DataCleanService(PlatformRepository repository, ObjectMapper objectMapper, SystemLogService logService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.logService = logService;
    }

    public CleanResult cleanRows(List<Map<String, Object>> rows, CleanRuleConfig config) {
        CleanRuleConfig rules = config == null ? CleanRuleConfig.defaults() : config;
        List<Map<String, Object>> cleanedRows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int changedCells = 0;
        int duplicateCount = 0;
        int errorCount = 0;

        for (Map<String, Object> source : rows) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                String field = entry.getKey();
                Object original = entry.getValue();
                Object value = original;

                if (rules.handleNulls() && value == null) {
                    value = "";
                }
                if (rules.trimSpaces() && value instanceof String text) {
                    value = text.trim();
                }
                if (value instanceof String text && !text.isBlank()) {
                    if (rules.normalizePhone() && DataValueUtils.isPhoneField(field)) {
                        String normalized = DataValueUtils.normalizePhone(text);
                        if (normalized.equals(text) && !DataValueUtils.validPhone(text)) {
                            errorCount++;
                        }
                        value = normalized;
                    } else if (rules.normalizeEmail() && DataValueUtils.isEmailField(field)) {
                        String normalized = DataValueUtils.normalizeEmail(text);
                        if (normalized.equals(text) && !DataValueUtils.validEmail(text)) {
                            errorCount++;
                        }
                        value = normalized;
                    } else if (rules.normalizeDate() && DataValueUtils.isDateField(field)) {
                        String normalized = DataValueUtils.normalizeDate(text);
                        if (normalized.equals(text) && !DataValueUtils.validDate(text)) {
                            errorCount++;
                        }
                        value = normalized;
                    }
                }
                if (!Objects.equals(original, value)) {
                    changedCells++;
                }
                cleaned.put(field, value);
            }
            if (rules.removeDuplicates()) {
                String canonical = DataValueUtils.canonicalJson(objectMapper, cleaned);
                if (!seen.add(canonical)) {
                    duplicateCount++;
                    continue;
                }
            }
            cleanedRows.add(cleaned);
        }
        return new CleanResult(rows.size(), cleanedRows.size(), changedCells + duplicateCount,
                duplicateCount, errorCount, cleanedRows);
    }

    public FusionRecord cleanBatch(long batchId, String user) {
        ensureRepository();
        CleanRuleConfig config = repository.getCleanRuleConfig();
        List<Map<String, Object>> rows = repository.listImportRows(batchId);
        CleanResult result = cleanRows(rows, config);
        long fusionId = repository.addFusionRecord(batchId, "基础清洗", result.beforeCount(), result.afterCount(),
                result.cleanedCount(), result.errorCount(), "success");
        for (int i = 0; i < result.rows().size(); i++) {
            repository.addCleanedRecord(fusionId, batchId, i + 1, result.rows().get(i));
        }
        if (logService != null) {
            logService.log(user, "执行清洗", "批次#" + batchId, "清洗后 " + result.afterCount() + " 行");
        }
        return repository.latestFusion(batchId).orElseThrow();
    }

    public CleanRuleConfig getConfig() {
        ensureRepository();
        return repository.getCleanRuleConfig();
    }

    public void saveConfig(CleanRuleConfig config, String user) {
        ensureRepository();
        repository.saveCleanRuleConfig(config);
        if (logService != null) {
            logService.log(user, "规则配置", "清洗规则", "保存成功");
        }
    }

    private void ensureRepository() {
        if (repository == null) {
            throw new IllegalStateException("当前 DataCleanService 未连接数据库");
        }
    }
}
