package com.dataprocess.platform.model;

public record DataSourceInfo(
        Long id,
        String sourceName,
        String sourceType,
        String connInfo,
        String status,
        String createTime
) {
}
