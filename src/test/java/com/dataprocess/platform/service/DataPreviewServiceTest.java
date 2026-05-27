package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.ParsedTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DataPreviewServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesCsvWithGeneratedBlankHeadersAndPreviewRows() throws Exception {
        Path csv = tempDir.resolve("customers.csv");
        Files.writeString(csv, "姓名,,email\n 张三 ,13800138000,ZHANG@EXAMPLE.COM\n李四,,li@example.com\n", StandardCharsets.UTF_8);

        DataPreviewService service = new DataPreviewService(new ObjectMapper());

        ParsedTable parsed = service.parse(csv);

        assertThat(parsed.headers()).containsExactly("姓名", "字段2", "email");
        assertThat(parsed.totalRows()).isEqualTo(2);
        assertThat(parsed.previewRows()).hasSize(2);
        assertThat(parsed.previewRows().get(0)).containsEntry("姓名", " 张三 ");
        assertThat(parsed.fieldTypes()).containsEntry("字段2", "文本");
    }
}
