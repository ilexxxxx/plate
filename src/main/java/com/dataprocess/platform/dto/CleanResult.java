package com.dataprocess.platform.dto;

import java.util.List;
import java.util.Map;

public record CleanResult(
        int beforeCount,
        int afterCount,
        int cleanedCount,
        int duplicateCount,
        int errorCount,
        List<Map<String, Object>> rows
) {
}
