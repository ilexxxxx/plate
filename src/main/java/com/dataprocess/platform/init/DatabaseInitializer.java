package com.dataprocess.platform.init;

import com.dataprocess.platform.model.CleanRuleConfig;
import com.dataprocess.platform.repository.PlatformRepository;
import com.dataprocess.platform.util.TimeUtils;
import jakarta.annotation.PostConstruct;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;
    private final PlatformRepository repository;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, PlatformRepository repository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    @PostConstruct
    public void initialize() throws IOException {
        createDirectories();
        createTables();
        seedData();
        createSampleFiles();
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(Path.of("data", "uploads"));
        Files.createDirectories(Path.of("data", "exports"));
        Files.createDirectories(Path.of("data", "samples"));
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT UNIQUE NOT NULL,
                  password TEXT NOT NULL,
                  role TEXT NOT NULL,
                  status TEXT NOT NULL,
                  create_time TEXT,
                  update_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS data_source (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  source_name TEXT,
                  source_type TEXT,
                  conn_info TEXT,
                  status TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS import_batch (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  source_id INTEGER NULL,
                  file_name TEXT,
                  file_type TEXT,
                  business_type TEXT,
                  row_count INTEGER,
                  field_count INTEGER,
                  status TEXT,
                  create_user TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS import_record (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  batch_id INTEGER,
                  row_index INTEGER,
                  data_json TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fusion_record (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  batch_id INTEGER,
                  fusion_type TEXT,
                  before_count INTEGER,
                  after_count INTEGER,
                  cleaned_count INTEGER,
                  error_count INTEGER,
                  status TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cleaned_record (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  fusion_id INTEGER,
                  batch_id INTEGER,
                  row_index INTEGER,
                  data_json TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS clean_rule_config (
                  id INTEGER PRIMARY KEY,
                  handle_nulls INTEGER,
                  trim_spaces INTEGER,
                  remove_duplicates INTEGER,
                  normalize_phone INTEGER,
                  normalize_email INTEGER,
                  normalize_date INTEGER,
                  update_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS check_result (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  batch_id INTEGER,
                  check_type TEXT,
                  description TEXT,
                  passed INTEGER,
                  pass_rate REAL,
                  error_count INTEGER,
                  result TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS stat_report (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  report_name TEXT,
                  export_format TEXT,
                  file_path TEXT,
                  create_user TEXT,
                  create_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_log (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user TEXT,
                  action TEXT,
                  target TEXT,
                  result TEXT,
                  log_time TEXT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notice (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT,
                  content TEXT,
                  type TEXT,
                  publish_time TEXT
                )
                """);
    }

    private void seedData() {
        if (repository.count("SELECT COUNT(*) FROM sys_user WHERE username = 'admin'") == 0) {
            repository.createUser("admin", "123456", "admin");
        }
        if (repository.count("SELECT COUNT(*) FROM data_source") == 0) {
            repository.addDataSource("演示MySQL客户库", "MySQL", "jdbc:mysql://localhost:3306/demo_customer", "在线");
            repository.addDataSource("运营CSV文件池", "CSV", "data/samples/sample.csv", "待导入");
            repository.addDataSource("产品Excel台账", "Excel", "data/samples/sample.xlsx", "待导入");
            repository.addDataSource("订单API接口", "API", "https://api.example.local/orders", "模拟");
            repository.addDataSource("历史Oracle仓库", "Oracle", "oracle://demo-history", "模拟");
        }
        if (repository.count("SELECT COUNT(*) FROM notice") == 0) {
            String now = TimeUtils.now();
            jdbcTemplate.update("INSERT INTO notice(title,content,type,publish_time) VALUES(?,?,?,?)",
                    "维护提醒", "本地演示数据库会在首次启动时自动生成，可直接使用 admin / 123456 登录。", "维护提醒", now);
            jdbcTemplate.update("INSERT INTO notice(title,content,type,publish_time) VALUES(?,?,?,?)",
                    "安全更新", "V1.0 为本地演示版，密码按任务书要求明文保存在 SQLite 中。", "安全更新", now);
            jdbcTemplate.update("INSERT INTO notice(title,content,type,publish_time) VALUES(?,?,?,?)",
                    "功能上线", "文件导入、清洗融合、统计分析、质量核验、报告导出和台账查阅已开放。", "功能上线", now);
        }
        if (repository.count("SELECT COUNT(*) FROM clean_rule_config WHERE id = 1") == 0) {
            repository.saveCleanRuleConfig(CleanRuleConfig.defaults());
        }
    }

    private void createSampleFiles() throws IOException {
        Path sampleDir = Path.of("data", "samples");
        Path csv = sampleDir.resolve("sample.csv");
        if (Files.notExists(csv)) {
            Files.writeString(csv, """
                    姓名,手机号,email,日期,业务分类
                    张三,+86 138-0013-8000,ZHANG@EXAMPLE.COM,2026/05/24,用户
                    李四,13900139000,li@example.com,2026-05-23,订单
                    王五,,bad-email,20260522,系统
                    """, StandardCharsets.UTF_8);
        }
        Path xlsx = sampleDir.resolve("sample.xlsx");
        if (Files.notExists(xlsx)) {
            writeWorkbook(new XSSFWorkbook(), xlsx);
        }
        Path xls = sampleDir.resolve("sample.xls");
        if (Files.notExists(xls)) {
            writeWorkbook(new HSSFWorkbook(), xls);
        }
    }

    private void writeWorkbook(Workbook workbook, Path path) throws IOException {
        Sheet sheet = workbook.createSheet("演示数据");
        String[][] rows = {
                {"姓名", "手机号", "email", "日期", "业务分类"},
                {"赵六", "+86 137-0013-7000", "zhao@example.com", "2026/05/21", "产品"},
                {"钱七", "13600136000", "qian@example.com", "2026-05-20", "运营"},
                {"孙八", "", "sun@example.com", "20260519", "客服"}
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < rows[i].length; j++) {
                row.createCell(j).setCellValue(rows[i][j]);
            }
        }
        for (int i = 0; i < rows[0].length; i++) {
            sheet.autoSizeColumn(i);
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            workbook.write(out);
        } finally {
            workbook.close();
        }
    }
}
