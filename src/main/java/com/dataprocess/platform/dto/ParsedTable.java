package com.dataprocess.platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public record ParsedTable(
        List<String> headers,
        List<Map<String, Object>> previewRows,
        int totalRows,
        Map<String, String> fieldTypes,
        String fileName,
        @JsonIgnore List<Map<String, Object>> rows
) {
}
