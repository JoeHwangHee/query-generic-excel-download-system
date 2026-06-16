package com.qexcel.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 사용자가 입력한 임의 SQL에 대한 최소한의 안전성 검증.
 * - 여러 구문은 {@code ;} 로 구분하며, 각 구문은 SELECT 또는 CREATE TEMPORARY TABLE 만 허용
 * - SELECT 가 최소 1개 있어야 함(내보낼 결과 필요)
 * - '?' 위치파라미터 개수 계산 (문자열 리터럴/주석 안의 '?'는 제외)
 */
public final class SqlValidator {

    private static final Pattern LEADING_SELECT =
            Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);

    // CREATE [GLOBAL|LOCAL] TEMPORARY TABLE ... (... AS SELECT / LIKE 포함)
    private static final Pattern TEMP_TABLE =
            Pattern.compile("^\\s*create\\s+(global\\s+|local\\s+)?temporary\\s+table\\b",
                    Pattern.CASE_INSENSITIVE);

    private SqlValidator() {
    }

    /**
     * 실행 가능한 스크립트인지 검증한다.
     * 각 구문은 SELECT 또는 CREATE TEMPORARY TABLE 이어야 하고, SELECT 가 1개 이상 있어야 한다.
     * 위반 시 {@link IllegalArgumentException}.
     */
    public static void validateRunnable(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 비어 있습니다.");
        }
        List<String> statements = SqlScript.split(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("실행할 SQL 구문이 없습니다.");
        }
        int selectCount = 0;
        for (String stmt : statements) {
            if (LEADING_SELECT.matcher(stmt).find()) {
                selectCount++;
            } else if (!TEMP_TABLE.matcher(stmt).find()) {
                throw new IllegalArgumentException("허용되지 않는 구문입니다: " + preview(stmt));
            }
        }
        if (selectCount == 0) {
            throw new IllegalArgumentException("내보낼 SELECT 구문이 최소 1개 필요합니다.");
        }
    }

    /**
     * 하위호환 별칭. 단일 SELECT 검증 호출부가 그대로 동작하도록 {@link #validateRunnable} 로 위임한다.
     * (DELETE/DROP 등은 여전히 거부된다.)
     */
    public static void validateSelectOnly(String sql) {
        validateRunnable(sql);
    }

    /** 문자열 리터럴/주석 밖의 '?' 개수를 센다. */
    public static int countPlaceholders(String sql) {
        String stripped = SqlScript.stripNonCode(sql);
        int count = 0;
        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    /** 오류 메시지에 쓸 구문 앞부분 미리보기(최대 40자). */
    private static String preview(String stmt) {
        String s = stmt.strip();
        return s.length() <= 40 ? s : s.substring(0, 40) + "...";
    }
}
