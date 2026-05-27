package com.dataprocess.platform.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class DataValueUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
                    .toFormatter()
    );

    private DataValueUtils() {
    }

    public static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }

    public static boolean isPhoneField(String field) {
        String lower = lower(field);
        return lower.contains("phone") || lower.contains("mobile") || field.contains("手机号") || field.contains("电话");
    }

    public static boolean isEmailField(String field) {
        String lower = lower(field);
        return lower.contains("email") || lower.contains("mail") || field.contains("邮箱");
    }

    public static boolean isDateField(String field) {
        String lower = lower(field);
        return lower.contains("date") || lower.contains("time") || field.contains("日期") || field.contains("时间");
    }

    public static String normalizePhone(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.startsWith("86") && digits.length() == 13) {
            digits = digits.substring(2);
        }
        return PHONE_PATTERN.matcher(digits).matches() ? digits : value;
    }

    public static boolean validPhone(String value) {
        return PHONE_PATTERN.matcher(value.replaceAll("\\D", "")).matches();
    }

    public static String normalizeEmail(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return EMAIL_PATTERN.matcher(normalized).matches() ? normalized : value;
    }

    public static boolean validEmail(String value) {
        return EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public static String normalizeDate(String value) {
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter.toString().contains("HourOfDay")) {
                    return LocalDateTime.parse(trimmed, formatter).toLocalDate().toString();
                }
                return LocalDate.parse(trimmed, formatter).toString();
            } catch (DateTimeParseException ignored) {
            }
        }
        return value;
    }

    public static boolean validDate(String value) {
        String normalized = normalizeDate(value);
        return !normalized.equals(value) || value.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static String canonicalJson(ObjectMapper objectMapper, Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(row));
        } catch (JsonProcessingException e) {
            return row.toString();
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
