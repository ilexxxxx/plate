package com.dataprocess.platform.model;

public record CleanRuleConfig(
        long id,
        boolean handleNulls,
        boolean trimSpaces,
        boolean removeDuplicates,
        boolean normalizePhone,
        boolean normalizeEmail,
        boolean normalizeDate,
        String updateTime
) {
    public static CleanRuleConfig defaults() {
        return new CleanRuleConfig(1, true, true, true, true, true, true, "");
    }
}
