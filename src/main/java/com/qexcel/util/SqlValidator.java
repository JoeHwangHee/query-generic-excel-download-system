package com.qexcel.util;

import java.util.regex.Pattern;

/**
 * 사용자가 입력한 임의 SQL에 대한 최소한의 안전성 검증.
 * - SELECT 문만 허용 (DML/DDL 차단)
 * - '?' 위치파라미터 개수 계산 (문자열 리터럴 안의 '?'는 제외)
 */
public final class SqlValidator {

    private static final Pattern LEADING_SELECT =
            Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);

    // 세미콜론으로 분리된 추가 구문(멀티 스테이트먼트) 차단용
    private static final Pattern FORBIDDEN =
            Pattern.compile("\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|merge|call|exec)\\b",
                    Pattern.CASE_INSENSITIVE);

    private SqlValidator() {
    }

    /** SELECT 단일 구문인지 검증. 위반 시 IllegalArgumentException. */
    public static void validateSelectOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 비어 있습니다.");
        }
        String stripped = stripStringLiterals(sql);
        if (!LEADING_SELECT.matcher(stripped).find()) {
            throw new IllegalArgumentException("SELECT 문만 허용됩니다.");
        }
        // 마지막 세미콜론 제거 후 내부에 또 다른 구문/금지 키워드가 있는지 확인
        String body = stripped.strip();
        if (body.endsWith(";")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.contains(";")) {
            throw new IllegalArgumentException("여러 SQL 구문은 허용되지 않습니다.");
        }
        if (FORBIDDEN.matcher(body).find()) {
            throw new IllegalArgumentException("허용되지 않는 키워드가 포함되어 있습니다.");
        }
    }

    /** 문자열 리터럴 밖의 '?' 개수를 센다. */
    public static int countPlaceholders(String sql) {
        String stripped = stripStringLiterals(sql);
        int count = 0;
        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    /** '...' 작은따옴표 문자열 리터럴을 공백으로 치환해 오인식을 방지한다. */
    static String stripStringLiterals(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        boolean inStr = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                // '' 이스케이프 처리
                if (inStr && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                inStr = !inStr;
                sb.append(' ');
            } else {
                sb.append(inStr ? ' ' : c);
            }
        }
        return sb.toString();
    }
}
