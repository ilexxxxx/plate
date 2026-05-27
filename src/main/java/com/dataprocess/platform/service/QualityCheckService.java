package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.QualityCheckItem;
import com.dataprocess.platform.model.ImportBatch;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.util.DataValueUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QualityCheckService {
    private final PlatformRepository repository;
    private final SystemLogService logService;
    private final ObjectMapper objectMapper;

    public QualityCheckService(ObjectMapper objectMapper) {
        this(null, null, objectMapper);
    }

    @Autowired
    public QualityCheckService(PlatformRepository repository, SystemLogService logService, ObjectMapper objectMapper) {
        this.repository = repository;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    public List<QualityCheckItem> evaluateRows(Long batchId, List<Map<String, Object>> rows) {
        int totalCells = rows.stream().mapToInt(Map::size).sum();
        int blankCount = rows.stream()
                .flatMap(row -> row.values().stream())
                .mapToInt(value -> DataValueUtils.isBlank(value) ? 1 : 0)
                .sum();
        int duplicateCount = duplicateCount(rows);
        int formatIssues = formatIssueCount(rows);

        return List.of(
                item(batchId, "完整性检查", "统计空值数量和空值率", blankCount, totalCells),
                item(batchId, "一致性检查", "检查手机号、邮箱、日期字段的明显格式不一致", formatIssues, Math.max(1, totalCells)),
                item(batchId, "重复性检查", "按整行 JSON 内容统计重复记录", duplicateCount, Math.max(1, rows.size())),
                item(batchId, "基础准确性检查", "校验手机号、邮箱、日期字段基础格式", formatIssues, Math.max(1, totalCells))
        );
    }

    public List<QualityCheckItem> runFullCheck(Long batchId, String user) {
        ensureRepository();
        Long actualBatchId = batchId != null ? batchId : repository.latestBatch().map(ImportBatch::id).orElse(null);
        if (actualBatchId == null) {
            return List.of();
        }
        List<QualityCheckItem> items = evaluateRows(actualBatchId, repository.listPreferredRows(actualBatchId));
        for (QualityCheckItem item : items) {
            repository.addCheckResult(actualBatchId, item.checkType(), item.description(), item.passed(),
                    item.passRate(), item.errorCount(), item.result());
        }
        if (logService != null) {
            logService.log(user, "执行质检", "批次#" + actualBatchId, "生成 " + items.size() + " 项结果");
        }
        return items;
    }

    public List<QualityCheckItem> latestItems(Long batchId) {
        ensureRepository();
        Long actualBatchId = batchId != null ? batchId : repository.latestBatch().map(ImportBatch::id).orElse(null);
        if (actualBatchId == null) {
            return List.of();
        }
        return repository.latestCheckResults(actualBatchId).stream()
                .map(result -> new QualityCheckItem(result.batchId(), result.checkType(), result.description(),
                        result.passed(), result.errorCount(), result.passRate(), result.result()))
                .toList();
    }

    private QualityCheckItem item(Long batchId, String type, String description, int errors, int denominator) {
        double passRate = denominator <= 0 ? 100.0 : Math.max(0.0, (denominator - errors) * 100.0 / denominator);
        boolean passed = errors == 0;
        return new QualityCheckItem(batchId, type, description, passed, errors, round(passRate), passed ? "通过" : "发现问题");
    }

    private int duplicateCount(List<Map<String, Object>> rows) {
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        for (Map<String, Object> row : rows) {
            String canonical = DataValueUtils.canonicalJson(objectMapper, row);
            if (!seen.add(canonical)) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private int formatIssueCount(List<Map<String, Object>> rows) {
        int issues = 0;
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (DataValueUtils.isBlank(entry.getValue())) {
                    continue;
                }
                String field = entry.getKey();
                String value = String.valueOf(entry.getValue());
                if (DataValueUtils.isPhoneField(field) && !DataValueUtils.validPhone(value)) {
                    issues++;
                } else if (DataValueUtils.isEmailField(field) && !DataValueUtils.validEmail(value)) {
                    issues++;
                } else if (DataValueUtils.isDateField(field) && !DataValueUtils.validDate(value)) {
                    issues++;
                }
            }
        }
        return issues;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void ensureRepository() {
        if (repository == null) {
            throw new IllegalStateException("当前 QualityCheckService 未连接数据库");
        }
    }
}
