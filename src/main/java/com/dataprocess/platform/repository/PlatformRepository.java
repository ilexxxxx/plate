package com.dataprocess.platform.repository;

import com.dataprocess.platform.dto.LedgerCard;
import com.dataprocess.platform.model.*;
import com.dataprocess.platform.util.TimeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@Repository
public class PlatformRepository {
    private static final TypeReference<LinkedHashMap<String, Object>> ROW_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PlatformRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    public long insert(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    public int count(String sql, Object... params) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count == null ? 0 : count;
    }

    public Optional<SysUser> findUser(String username) {
        List<SysUser> users = jdbcTemplate.query("SELECT * FROM sys_user WHERE username = ?",
                (rs, rowNum) -> user(rs), username);
        return users.stream().findFirst();
    }

    public Optional<SysUser> findUserById(long id) {
        List<SysUser> users = jdbcTemplate.query("SELECT * FROM sys_user WHERE id = ?",
                (rs, rowNum) -> user(rs), id);
        return users.stream().findFirst();
    }

    public List<SysUser> listUsers() {
        return jdbcTemplate.query("SELECT * FROM sys_user ORDER BY id", (rs, rowNum) -> user(rs));
    }

    public void createUser(String username, String password, String role) {
        String now = TimeUtils.now();
        insert("INSERT INTO sys_user(username,password,role,status,create_time,update_time) VALUES(?,?,?,?,?,?)",
                username, password, role, "enabled", now, now);
    }

    public void resetPassword(long userId, String password) {
        jdbcTemplate.update("UPDATE sys_user SET password = ?, update_time = ? WHERE id = ?",
                password, TimeUtils.now(), userId);
    }

    public List<DataSourceInfo> listDataSources() {
        return jdbcTemplate.query("SELECT * FROM data_source ORDER BY id DESC", (rs, rowNum) -> source(rs));
    }

    public long addDataSource(String name, String type, String connInfo, String status) {
        return insert("INSERT INTO data_source(source_name,source_type,conn_info,status,create_time) VALUES(?,?,?,?,?)",
                name, type, connInfo, status, TimeUtils.now());
    }

    public void updateDataSourceStatus(long id, String status) {
        jdbcTemplate.update("UPDATE data_source SET status = ? WHERE id = ?", status, id);
    }

    public long addImportBatch(Long sourceId, String fileName, String fileType, String businessType,
                               int rowCount, int fieldCount, String status, String createUser) {
        return insert("""
                        INSERT INTO import_batch(source_id,file_name,file_type,business_type,row_count,field_count,status,create_user,create_time)
                        VALUES(?,?,?,?,?,?,?,?,?)
                        """,
                sourceId, fileName, fileType, businessType, rowCount, fieldCount, status, createUser, TimeUtils.now());
    }

    public void addImportRecord(long batchId, int rowIndex, Map<String, Object> row) {
        insert("INSERT INTO import_record(batch_id,row_index,data_json,create_time) VALUES(?,?,?,?)",
                batchId, rowIndex, writeJson(row), TimeUtils.now());
    }

    public List<ImportBatch> listBatches() {
        return jdbcTemplate.query("SELECT * FROM import_batch ORDER BY id DESC", (rs, rowNum) -> batch(rs));
    }

    public List<ImportBatch> listBatches(String businessType, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM import_batch WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (businessType != null && !businessType.isBlank()) {
            sql.append(" AND business_type = ?");
            params.add(businessType);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (file_name LIKE ? OR status LIKE ? OR create_user LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> batch(rs), params.toArray());
    }

    public Optional<ImportBatch> latestBatch() {
        List<ImportBatch> batches = jdbcTemplate.query("SELECT * FROM import_batch ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> batch(rs));
        return batches.stream().findFirst();
    }

    public Optional<ImportBatch> findBatch(long id) {
        List<ImportBatch> batches = jdbcTemplate.query("SELECT * FROM import_batch WHERE id = ?",
                (rs, rowNum) -> batch(rs), id);
        return batches.stream().findFirst();
    }

    public List<Map<String, Object>> listImportRows(long batchId) {
        return jdbcTemplate.query("SELECT data_json FROM import_record WHERE batch_id = ? ORDER BY row_index",
                (rs, rowNum) -> readRow(rs.getString("data_json")), batchId);
    }

    public CleanRuleConfig getCleanRuleConfig() {
        List<CleanRuleConfig> configs = jdbcTemplate.query("SELECT * FROM clean_rule_config WHERE id = 1",
                (rs, rowNum) -> new CleanRuleConfig(
                        rs.getLong("id"),
                        rs.getInt("handle_nulls") == 1,
                        rs.getInt("trim_spaces") == 1,
                        rs.getInt("remove_duplicates") == 1,
                        rs.getInt("normalize_phone") == 1,
                        rs.getInt("normalize_email") == 1,
                        rs.getInt("normalize_date") == 1,
                        rs.getString("update_time")));
        return configs.stream().findFirst().orElse(CleanRuleConfig.defaults());
    }

    public void saveCleanRuleConfig(CleanRuleConfig config) {
        jdbcTemplate.update("""
                        INSERT INTO clean_rule_config(id,handle_nulls,trim_spaces,remove_duplicates,normalize_phone,normalize_email,normalize_date,update_time)
                        VALUES(1,?,?,?,?,?,?,?)
                        ON CONFLICT(id) DO UPDATE SET
                        handle_nulls=excluded.handle_nulls,
                        trim_spaces=excluded.trim_spaces,
                        remove_duplicates=excluded.remove_duplicates,
                        normalize_phone=excluded.normalize_phone,
                        normalize_email=excluded.normalize_email,
                        normalize_date=excluded.normalize_date,
                        update_time=excluded.update_time
                        """,
                bit(config.handleNulls()), bit(config.trimSpaces()), bit(config.removeDuplicates()),
                bit(config.normalizePhone()), bit(config.normalizeEmail()), bit(config.normalizeDate()), TimeUtils.now());
    }

    public long addFusionRecord(long batchId, String type, int beforeCount, int afterCount,
                                int cleanedCount, int errorCount, String status) {
        return insert("""
                        INSERT INTO fusion_record(batch_id,fusion_type,before_count,after_count,cleaned_count,error_count,status,create_time)
                        VALUES(?,?,?,?,?,?,?,?)
                        """,
                batchId, type, beforeCount, afterCount, cleanedCount, errorCount, status, TimeUtils.now());
    }

    public void addCleanedRecord(long fusionId, long batchId, int rowIndex, Map<String, Object> row) {
        insert("INSERT INTO cleaned_record(fusion_id,batch_id,row_index,data_json,create_time) VALUES(?,?,?,?,?)",
                fusionId, batchId, rowIndex, writeJson(row), TimeUtils.now());
    }

    public List<FusionRecord> listFusionRecords() {
        return jdbcTemplate.query("SELECT * FROM fusion_record ORDER BY id DESC", (rs, rowNum) -> fusion(rs));
    }

    public Optional<FusionRecord> latestFusion(long batchId) {
        List<FusionRecord> records = jdbcTemplate.query("SELECT * FROM fusion_record WHERE batch_id = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> fusion(rs), batchId);
        return records.stream().findFirst();
    }

    public List<Map<String, Object>> listLatestCleanedRows(long batchId) {
        Optional<FusionRecord> latest = latestFusion(batchId);
        if (latest.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("SELECT data_json FROM cleaned_record WHERE fusion_id = ? ORDER BY row_index",
                (rs, rowNum) -> readRow(rs.getString("data_json")), latest.get().id());
    }

    public List<Map<String, Object>> listPreferredRows(long batchId) {
        List<Map<String, Object>> cleaned = listLatestCleanedRows(batchId);
        return cleaned.isEmpty() ? listImportRows(batchId) : cleaned;
    }

    public void addCheckResult(long batchId, String checkType, String description, boolean passed,
                               double passRate, int errorCount, String result) {
        insert("""
                        INSERT INTO check_result(batch_id,check_type,description,passed,pass_rate,error_count,result,create_time)
                        VALUES(?,?,?,?,?,?,?,?)
                        """,
                batchId, checkType, description, bit(passed), passRate, errorCount, result, TimeUtils.now());
    }

    public List<CheckResult> latestCheckResults(Long batchId) {
        if (batchId == null) {
            return List.of();
        }
        List<String> times = jdbcTemplate.query("SELECT create_time FROM check_result WHERE batch_id = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString(1), batchId);
        if (times.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("SELECT * FROM check_result WHERE batch_id = ? AND create_time = ? ORDER BY id",
                (rs, rowNum) -> check(rs), batchId, times.get(0));
    }

    public long addReport(String name, String format, String filePath, String user) {
        return insert("INSERT INTO stat_report(report_name,export_format,file_path,create_user,create_time) VALUES(?,?,?,?,?)",
                name, format, filePath, user, TimeUtils.now());
    }

    public List<StatReport> listReports() {
        return jdbcTemplate.query("SELECT * FROM stat_report ORDER BY id DESC", (rs, rowNum) -> report(rs));
    }

    public Optional<StatReport> findReport(long id) {
        List<StatReport> reports = jdbcTemplate.query("SELECT * FROM stat_report WHERE id = ?",
                (rs, rowNum) -> report(rs), id);
        return reports.stream().findFirst();
    }

    public List<Notice> listNotices() {
        return jdbcTemplate.query("SELECT * FROM notice ORDER BY id DESC", (rs, rowNum) -> notice(rs));
    }

    public void addLog(String user, String action, String target, String result) {
        insert("INSERT INTO sys_log(user,action,target,result,log_time) VALUES(?,?,?,?,?)",
                user, action, target, result, TimeUtils.now());
    }

    public List<SystemLog> recentLogs(int limit) {
        return jdbcTemplate.query("SELECT * FROM sys_log ORDER BY id DESC LIMIT ?",
                (rs, rowNum) -> log(rs), limit);
    }

    public Map<String, Integer> groupedCounts(String sql) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> counts.put(rs.getString(1), rs.getInt(2)));
        return counts;
    }

    public List<LedgerCard> ledgerCards() {
        List<String> types = List.of("用户", "订单", "产品", "运营", "客服", "系统");
        List<LedgerCard> cards = new ArrayList<>();
        for (String type : types) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT COUNT(*) AS batch_count, COALESCE(SUM(row_count),0) AS record_count, MAX(create_time) AS latest_time
                    FROM import_batch WHERE business_type = ?
                    """, type);
            Map<String, Object> row = rows.get(0);
            cards.add(new LedgerCard(type,
                    number(row.get("batch_count")).intValue(),
                    number(row.get("record_count")).intValue(),
                    Objects.toString(row.get("latest_time"), "-")));
        }
        return cards;
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON序列化失败", e);
        }
    }

    private LinkedHashMap<String, Object> readRow(String json) {
        try {
            return objectMapper.readValue(json, ROW_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON解析失败", e);
        }
    }

    private SysUser user(ResultSet rs) throws java.sql.SQLException {
        return new SysUser(rs.getLong("id"), rs.getString("username"), rs.getString("password"),
                rs.getString("role"), rs.getString("status"), rs.getString("create_time"), rs.getString("update_time"));
    }

    private DataSourceInfo source(ResultSet rs) throws java.sql.SQLException {
        return new DataSourceInfo(rs.getLong("id"), rs.getString("source_name"), rs.getString("source_type"),
                rs.getString("conn_info"), rs.getString("status"), rs.getString("create_time"));
    }

    private ImportBatch batch(ResultSet rs) throws java.sql.SQLException {
        return new ImportBatch(rs.getLong("id"), nullableLong(rs.getObject("source_id")),
                rs.getString("file_name"), rs.getString("file_type"), rs.getString("business_type"),
                rs.getInt("row_count"), rs.getInt("field_count"), rs.getString("status"),
                rs.getString("create_user"), rs.getString("create_time"));
    }

    private FusionRecord fusion(ResultSet rs) throws java.sql.SQLException {
        return new FusionRecord(rs.getLong("id"), rs.getLong("batch_id"), rs.getString("fusion_type"),
                rs.getInt("before_count"), rs.getInt("after_count"), rs.getInt("cleaned_count"),
                rs.getInt("error_count"), rs.getString("status"), rs.getString("create_time"));
    }

    private CheckResult check(ResultSet rs) throws java.sql.SQLException {
        return new CheckResult(rs.getLong("id"), rs.getLong("batch_id"), rs.getString("check_type"),
                rs.getString("description"), rs.getInt("passed") == 1, rs.getDouble("pass_rate"),
                rs.getInt("error_count"), rs.getString("result"), rs.getString("create_time"));
    }

    private StatReport report(ResultSet rs) throws java.sql.SQLException {
        return new StatReport(rs.getLong("id"), rs.getString("report_name"), rs.getString("export_format"),
                rs.getString("file_path"), rs.getString("create_user"), rs.getString("create_time"));
    }

    private Notice notice(ResultSet rs) throws java.sql.SQLException {
        return new Notice(rs.getLong("id"), rs.getString("title"), rs.getString("content"),
                rs.getString("type"), rs.getString("publish_time"));
    }

    private SystemLog log(ResultSet rs) throws java.sql.SQLException {
        return new SystemLog(rs.getLong("id"), rs.getString("user"), rs.getString("action"),
                rs.getString("target"), rs.getString("result"), rs.getString("log_time"));
    }

    private int bit(boolean value) {
        return value ? 1 : 0;
    }

    private Long nullableLong(Object value) {
        return value == null ? null : number(value).longValue();
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return value == null ? 0 : Long.parseLong(value.toString());
    }
}
