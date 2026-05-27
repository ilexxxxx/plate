package com.dataprocess.platform.model;

public record ImportBatch(
        Long id,
        Long sourceId,
        String fileName,
        String fileType,
        String businessType,
        int rowCount,
        int fieldCount,
        String status,
        String createUser,
        String createTime
) {
    public String label() {
        return "#" + id + " " + fileName + " (" + rowCount + "行)";
    }
}
