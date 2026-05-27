package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.ParsedTable;
import com.dataprocess.platform.util.DataValueUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class DataPreviewService {
    private final ObjectMapper objectMapper;

    public DataPreviewService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedTable parse(Path file) throws IOException {
        String name = file.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return parseCsv(file);
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(file);
        }
        throw new IllegalArgumentException("仅支持 CSV、XLSX、XLS 文件");
    }

    private ParsedTable parseCsv(Path file) throws IOException {
        String content = decodeCsv(Files.readAllBytes(file));
        try (Reader reader = new StringReader(content);
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(false).build().parse(reader)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new IllegalArgumentException("文件为空，无法预览");
            }
            List<String> headers = normalizeHeaders(records.get(0).toList());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                Map<String, Object> row = rowFromValues(headers, records.get(i).toList());
                if (!allBlank(row)) {
                    rows.add(row);
                }
            }
            return parsed(file, headers, rows);
        }
    }

    private ParsedTable parseExcel(Path file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(file))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel 工作表为空，无法预览");
            }
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            int width = Math.max(0, headerRow.getLastCellNum());
            List<String> rawHeaders = new ArrayList<>();
            for (int i = 0; i < width; i++) {
                rawHeaders.add(formatter.formatCellValue(headerRow.getCell(i)));
            }
            List<String> headers = normalizeHeaders(rawHeaders);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row sheetRow = sheet.getRow(r);
                if (sheetRow == null) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (int c = 0; c < headers.size(); c++) {
                    values.add(formatter.formatCellValue(sheetRow.getCell(c)));
                }
                Map<String, Object> row = rowFromValues(headers, values);
                if (!allBlank(row)) {
                    rows.add(row);
                }
            }
            return parsed(file, headers, rows);
        }
    }

    private ParsedTable parsed(Path file, List<String> headers, List<Map<String, Object>> rows) {
        List<Map<String, Object>> preview = rows.stream()
                .limit(20)
                .map(row -> (Map<String, Object>) new LinkedHashMap<String, Object>(row))
                .toList();
        return new ParsedTable(headers, preview, rows.size(), inferFieldTypes(headers, rows), file.getFileName().toString(), rows);
    }

    private String decodeCsv(byte[] bytes) throws IOException {
        try {
            return decode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException ignored) {
            return decode(bytes, Charset.forName("GBK"));
        }
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private List<String> normalizeHeaders(List<String> rawHeaders) {
        List<String> headers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String header = rawHeaders.get(i) == null ? "" : rawHeaders.get(i).trim();
            if (header.isBlank()) {
                header = "字段" + (i + 1);
            }
            String unique = header;
            int suffix = 2;
            while (seen.contains(unique)) {
                unique = header + "_" + suffix++;
            }
            seen.add(unique);
            headers.add(unique);
        }
        return headers;
    }

    private Map<String, Object> rowFromValues(List<String> headers, List<String> values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i) : "");
        }
        return row;
    }

    private Map<String, String> inferFieldTypes(List<String> headers, List<Map<String, Object>> rows) {
        Map<String, String> types = new LinkedHashMap<>();
        for (String header : headers) {
            if (DataValueUtils.isDateField(header)) {
                types.put(header, "日期");
            } else {
                types.put(header, "文本");
            }
        }
        return types;
    }

    private boolean allBlank(Map<String, Object> row) {
        return row.values().stream().allMatch(DataValueUtils::isBlank);
    }
}
