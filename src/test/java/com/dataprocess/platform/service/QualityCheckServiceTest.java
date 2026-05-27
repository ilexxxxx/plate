package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.QualityCheckItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QualityCheckServiceTest {

    @Test
    void evaluatesCompletenessDuplicatesAndFormatAccuracy() {
        QualityCheckService service = new QualityCheckService(new ObjectMapper());
        Map<String, Object> first = row("张三", "13800138000", "bad-email", "");
        Map<String, Object> duplicate = row("张三", "13800138000", "bad-email", "");

        List<QualityCheckItem> items = service.evaluateRows(7L, List.of(first, duplicate));

        assertThat(items).extracting(QualityCheckItem::checkType)
                .containsExactly("完整性检查", "一致性检查", "重复性检查", "基础准确性检查");
        assertThat(items.get(0).errorCount()).isEqualTo(2);
        assertThat(items.get(2).errorCount()).isEqualTo(1);
        assertThat(items.get(3).errorCount()).isEqualTo(2);
    }

    private Map<String, Object> row(String name, String phone, String email, String date) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("姓名", name);
        row.put("手机号", phone);
        row.put("email", email);
        row.put("日期", date);
        return row;
    }
}
