package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.StatisticsSummary;
import com.dataprocess.platform.model.CheckResult;
import com.dataprocess.platform.model.ImportBatch;
import com.dataprocess.platform.model.StatReport;
import com.dataprocess.platform.repository.PlatformRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ReportExportService {
    private final PlatformRepository repository;
    private final StatisticsService statisticsService;
    private final SystemLogService logService;

    public ReportExportService(PlatformRepository repository, StatisticsService statisticsService, SystemLogService logService) {
        this.repository = repository;
        this.statisticsService = statisticsService;
        this.logService = logService;
    }

    public StatReport export(Long batchId, String scope, String format, String user) throws IOException {
        Long actualBatchId = batchId != null ? batchId : repository.latestBatch().map(ImportBatch::id).orElse(null);
        if (actualBatchId == null) {
            throw new IllegalArgumentException("暂无可导出的导入批次");
        }
        String normalizedFormat = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format) ? "xlsx" : "csv";
        List<Map<String, Object>> rows = rowsForScope(actualBatchId, scope);
        Files.createDirectories(Path.of("data", "exports"));
        String reportName = nameForScope(scope) + "-批次" + actualBatchId;
        String fileName = reportName + "-" + System.currentTimeMillis() + "." + normalizedFormat;
        Path target = Path.of("data", "exports", fileName);
        if ("xlsx".equals(normalizedFormat)) {
            writeExcel(target, rows);
        } else {
            writeCsv(target, rows);
        }
        long id = repository.addReport(reportName, normalizedFormat, target.toAbsolutePath().toString(), user);
        logService.log(user, "导出报告", reportName, normalizedFormat);
        return repository.findReport(id).orElseThrow();
    }

    public List<StatReport> listReports() {
        return repository.listReports();
    }

    public Optional<StatReport> findReport(long id) {
        return repository.findReport(id);
    }

    private List<Map<String, Object>> rowsForScope(long batchId, String scope) {
        return switch (scope == null ? "original" : scope) {
            case "cleaned" -> {
                List<Map<String, Object>> cleaned = repository.listLatestCleanedRows(batchId);
                yield cleaned.isEmpty() ? repository.listImportRows(batchId) : cleaned;
            }
            case "statistics" -> List.of(statisticsRow(statisticsService.summary(batchId)));
            case "quality" -> repository.latestCheckResults(batchId).stream().map(this::qualityRow).toList();
            default -> repository.listImportRows(batchId);
        };
    }

    private Map<String, Object> statisticsRow(StatisticsSummary summary) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("总数据量", summary.totalRows());
        row.put("字段数", summary.fieldCount());
        row.put("增量数据量", summary.incrementalRows());
        row.put("空值率", summary.nullRate());
        row.put("重复数", summary.duplicateCount());
        row.put("质量检验通过率", summary.qualityPassRate());
        return row;
    }

    private Map<String, Object> qualityRow(CheckResult result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("检查项", result.checkType());
        row.put("检查说明", result.description());
        row.put("是否通过", result.passed() ? "通过" : "未通过");
        row.put("问题数量", result.errorCount());
        row.put("通过率", result.passRate());
        row.put("生成时间", result.createTime());
        return row;
    }

    private String nameForScope(String scope) {
        return switch (scope == null ? "original" : scope) {
            case "cleaned" -> "清洗后数据";
            case "statistics" -> "统计结果";
            case "quality" -> "质量核验报告";
            default -> "原始数据";
        };
    }

    private void writeCsv(Path target, List<Map<String, Object>> rows) throws IOException {
        List<String> headers = headers(rows);
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", headers)).append(System.lineSeparator());
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escape(Objects.toString(row.get(headers.get(i)), "")));
            }
            builder.append(System.lineSeparator());
        }
        Files.writeString(target, builder.toString(), StandardCharsets.UTF_8);
    }

    private void writeExcel(Path target, List<Map<String, Object>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出数据");
            List<String> headers = headers(rows);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row excelRow = sheet.createRow(r + 1);
                for (int c = 0; c < headers.size(); c++) {
                    excelRow.createCell(c).setCellValue(Objects.toString(rows.get(r).get(headers.get(c)), ""));
                }
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                workbook.write(out);
            }
        }
    }

    private List<String> headers(List<Map<String, Object>> rows) {
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        rows.forEach(row -> headers.addAll(row.keySet()));
        return headers.isEmpty() ? List.of("提示") : new ArrayList<>(headers);
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
