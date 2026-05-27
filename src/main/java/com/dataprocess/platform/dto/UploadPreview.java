package com.dataprocess.platform.dto;

public record UploadPreview(
        String token,
        String fileName,
        Long sourceId,
        String businessType,
        ParsedTable table
) {
}
