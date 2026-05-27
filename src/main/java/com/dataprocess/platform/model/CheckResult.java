package com.dataprocess.platform.model;

public record CheckResult(
        Long id,
        Long batchId,
        String checkType,
        String description,
        boolean passed,
        double passRate,
        int errorCount,
        String result,
        String createTime
) {
}
