package com.dataprocess.platform.dto;

public record LedgerCard(
        String businessType,
        int batchCount,
        int recordCount,
        String latestImportTime
) {
}
