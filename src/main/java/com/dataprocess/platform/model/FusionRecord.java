package com.dataprocess.platform.model;

public record FusionRecord(
        Long id,
        Long batchId,
        String fusionType,
        int beforeCount,
        int afterCount,
        int cleanedCount,
        int errorCount,
        String status,
        String createTime
) {
}
