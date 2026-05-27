package com.dataprocess.platform.model;

public record StatReport(
        Long id,
        String reportName,
        String exportFormat,
        String filePath,
        String createUser,
        String createTime
) {
}
