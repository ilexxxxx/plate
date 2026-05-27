package com.dataprocess.platform.dto;

import java.util.List;
import java.util.Map;

public record StatisticsSummary(
        Long batchId,
        int totalRows,
        int fieldCount,
        int incrementalRows,
        double nullRate,
        int duplicateCount,
        double qualityPassRate,
        List<String> recentBatchLabels,
        List<Integer> recentBatchRows,
        Map<String, Integer> sourceTypeCounts,
        Map<String, Integer> businessTypeCounts
) {
}
