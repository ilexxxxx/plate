package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.CleanResult;
import com.dataprocess.platform.model.CleanRuleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataCleanServiceTest {

    @Test
    void appliesEnabledRulesAndKeepsFirstDuplicateRow() {
        DataCleanService service = new DataCleanService(new ObjectMapper());
        Map<String, Object> first = row(" 张三 ", "+86 138-0013-8000", "USER@EXAMPLE.COM", "2026/05/24", null);
        Map<String, Object> duplicate = row(" 张三 ", "+86 138-0013-8000", "USER@EXAMPLE.COM", "2026/05/24", null);

        CleanResult result = service.cleanRows(List.of(first, duplicate), CleanRuleConfig.defaults());

        assertThat(result.beforeCount()).isEqualTo(2);
        assertThat(result.afterCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0))
                .containsEntry("姓名", "张三")
                .containsEntry("手机号", "13800138000")
                .containsEntry("email", "user@example.com")
                .containsEntry("日期", "2026-05-24")
                .containsEntry("备注", "");
    }

    private Map<String, Object> row(String name, String phone, String email, String date, Object note) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("姓名", name);
        row.put("手机号", phone);
        row.put("email", email);
        row.put("日期", date);
        row.put("备注", note);
        return row;
    }
}
