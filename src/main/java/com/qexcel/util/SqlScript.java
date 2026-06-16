package com.qexcel.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 여러 SQL 구문이 {@code ;} 로 이어진 스크립트를 다루는 유틸.
 *
 * <p>상태 기반 토크나이저로 문자열 리터럴({@code '...'}, {@code ''} 이스케이프),
 * 식별자 따옴표({@code "..."}, 백틱 {@code `...`}), 라인 주석({@code -- ... \n}),
 * 블록 주석({@code /* ... *&#47;}) 안의 {@code ;} 와 {@code ?} 를 코드가 아닌 것으로
 * 간주한다. {@link SqlValidator} 가 이 토크나이저를 공유한다.</p>
 */
public final class SqlScript {

    private static final int NORMAL = 0;
    private static final int SQUOTE = 1;
    private static final int DQUOTE = 2;
    private static final int BACKTICK = 3;
    private static final int LINE_COMMENT = 4;
    private static final int BLOCK_COMMENT = 5;

    private SqlScript() {
    }

    /**
     * 최상위 {@code ;} 기준으로 구문을 분리한다. 따옴표/주석 안의 {@code ;} 는 무시하며,
     * 빈 구문과 말미 {@code ;} 는 제거한다. 각 구문은 trim 된 원본 텍스트다.
     */
    public static List<String> split(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }
        boolean[] code = codeMask(sql);
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (code[i] && sql.charAt(i) == ';') {
                String stmt = sql.substring(start, i).trim();
                if (!stmt.isEmpty()) {
                    out.add(stmt);
                }
                start = i + 1;
            }
        }
        String last = sql.substring(start).trim();
        if (!last.isEmpty()) {
            out.add(last);
        }
        return out;
    }

    /**
     * 문자열/식별자 리터럴과 주석을 공백으로 치환해 코드 영역만 남긴다.
     * {@code ?} 개수 세기 등 토큰 분석의 오인식을 막는 용도.
     */
    public static String stripNonCode(String sql) {
        if (sql == null) {
            return "";
        }
        boolean[] code = codeMask(sql);
        StringBuilder sb = new StringBuilder(sql.length());
        for (int i = 0; i < sql.length(); i++) {
            sb.append(code[i] ? sql.charAt(i) : ' ');
        }
        return sb.toString();
    }

    /** 각 문자가 코드 영역(NORMAL 상태, 따옴표/주석 구분자가 아님)인지 표시한 마스크. */
    private static boolean[] codeMask(String sql) {
        int n = sql.length();
        boolean[] code = new boolean[n];
        int state = NORMAL;
        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';
            switch (state) {
                case NORMAL -> {
                    if (c == '\'') {
                        state = SQUOTE;
                    } else if (c == '"') {
                        state = DQUOTE;
                    } else if (c == '`') {
                        state = BACKTICK;
                    } else if (c == '-' && next == '-') {
                        state = LINE_COMMENT;
                        i++;
                    } else if (c == '/' && next == '*') {
                        state = BLOCK_COMMENT;
                        i++;
                    } else {
                        code[i] = true;
                    }
                }
                case SQUOTE -> {
                    if (c == '\'') {
                        if (next == '\'') {
                            i++; // '' 이스케이프
                        } else {
                            state = NORMAL;
                        }
                    }
                }
                case DQUOTE -> {
                    if (c == '"') {
                        if (next == '"') {
                            i++; // "" 이스케이프
                        } else {
                            state = NORMAL;
                        }
                    }
                }
                case BACKTICK -> {
                    if (c == '`') {
                        if (next == '`') {
                            i++; // `` 이스케이프
                        } else {
                            state = NORMAL;
                        }
                    }
                }
                case LINE_COMMENT -> {
                    if (c == '\n') {
                        state = NORMAL;
                        code[i] = true; // 개행은 공백처럼 코드로 둔다
                    }
                }
                case BLOCK_COMMENT -> {
                    if (c == '*' && next == '/') {
                        i++;
                        state = NORMAL;
                    }
                }
                default -> code[i] = true;
            }
        }
        return code;
    }
}
