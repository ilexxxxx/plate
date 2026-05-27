package com.dataprocess.platform.dto;

public record QualityCheckItem(
        Long batchId,
        String checkType,
        String description,
        boolean passed,
        int errorCount,
        double passRate,
        String result
) {
}
