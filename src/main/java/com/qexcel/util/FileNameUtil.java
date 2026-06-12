package com.qexcel.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 파일명 생성 유틸. Windows 금지문자를 제거하고 "쿼리명_실행일시" 규칙을 적용한다.
 */
public final class FileNameUtil {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    // Windows 파일명 금지문자: \ / : * ? " < > |
    private static final Pattern ILLEGAL = Pattern.compile("[\\\\/:*?\"<>|]");

    private FileNameUtil() {
    }

    public static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "query";
        }
        return ILLEGAL.matcher(name).replaceAll("_").trim();
    }

    /** 예) 월간거래내역_20260612_134501 (확장자 제외) */
    public static String baseName(String queryName, LocalDateTime when) {
        return sanitize(queryName) + "_" + TS.format(when);
    }
}
