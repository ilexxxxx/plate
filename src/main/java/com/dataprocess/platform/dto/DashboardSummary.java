package com.dataprocess.platform.dto;

public record DashboardSummary(
        int sourceCount,
        int batchCount,
        int recordCount,
        int fieldCount,
        double qualityPassRate
) {
}
