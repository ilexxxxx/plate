package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.StatisticsSummary;
import com.dataprocess.platform.model.CheckResult;
import com.dataprocess.platform.model.ImportBatch;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.util.DataValueUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatisticsService {
    private final PlatformRepository repository;
    private final ObjectMapper objectMapper;

    public StatisticsService(PlatformRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public StatisticsSummary summary(Long batchId) {
        Long actualBatchId = batchId != null ? batchId : repository.latestBatch().map(ImportBatch::id).orElse(null);
        if (actualBatchId == null) {
            return empty();
        }
        List<Map<String, Object>> rows = repository.listPreferredRows(actualBatchId);
        int fieldCount = rows.isEmpty() ? 0 : rows.get(0).size();
        int totalCells = rows.stream().mapToInt(Map::size).sum();
        int nullCount = rows.stream().flatMap(row -> row.values().stream()).mapToInt(v -> DataValueUtils.isBlank(v) ? 1 : 0).sum();
        int duplicateCount = duplicateCount(rows);
        double nullRate = totalCells == 0 ? 0 : nullCount * 100.0 / totalCells;
        double passRate = latestPassRate(actualBatchId);
        List<ImportBatch> recent = new ArrayList<>(repository.listBatches().stream().limit(7).toList());
        Collections.reverse(recent);
        return new StatisticsSummary(actualBatchId, rows.size(), fieldCount, rows.size(), round(nullRate), duplicateCount, passRate,
                recent.stream().map(batch -> "#" + batch.id()).toList(),
                recent.stream().map(ImportBatch::rowCount).toList(),
                repository.groupedCounts("SELECT source_type, COUNT(*) FROM data_source GROUP BY source_type ORDER BY source_type"),
                repository.groupedCounts("SELECT business_type, COUNT(*) FROM import_batch GROUP BY business_type ORDER BY business_type"));
    }

    private StatisticsSummary empty() {
        return new StatisticsSummary(null, 0, 0, 0, 0, 0, 0, List.of(), List.of(), Map.of(), Map.of());
    }

    private int duplicateCount(List<Map<String, Object>> rows) {
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        for (Map<String, Object> row : rows) {
            if (!seen.add(DataValueUtils.canonicalJson(objectMapper, row))) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private double latestPassRate(Long batchId) {
        List<CheckResult> results = repository.latestCheckResults(batchId);
        if (results.isEmpty()) {
            return 0.0;
        }
        return round(results.stream().mapToDouble(CheckResult::passRate).average().orElse(0.0));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
