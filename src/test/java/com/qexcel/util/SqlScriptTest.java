package com.qexcel.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlScriptTest {

    @Test
    void splitsTopLevelStatements() {
        List<String> stmts = SqlScript.split("SELECT 1; SELECT 2");
        assertEquals(List.of("SELECT 1", "SELECT 2"), stmts);
    }

    @Test
    void ignoresTrailingSemicolonAndBlankStatements() {
        // 말미 세미콜론과 빈 구문(;;)은 제거된다
        assertEquals(List.of("SELECT 1"), SqlScript.split("SELECT 1;"));
        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.split("SELECT 1;; SELECT 2;"));
    }

    @Test
    void ignoresSemicolonInsideStringLiteral() {
        // 문자열 리터럴 안의 ';' 는 구문 경계가 아니다
        List<String> stmts = SqlScript.split("SELECT ';' AS a; SELECT 2");
        assertEquals(List.of("SELECT ';' AS a", "SELECT 2"), stmts);
    }

    @Test
    void ignoresSemicolonInsideComments() {
        // 라인/블록 주석 안의 ';' 는 구문 경계가 아니다 → 2개 구문(주석은 앞 구문 텍스트에 포함)
        List<String> line = SqlScript.split("SELECT 1 -- a;b\n; SELECT 2");
        assertEquals(2, line.size());
        assertEquals("SELECT 2", line.get(1));

        List<String> block = SqlScript.split("SELECT 1 /* a;b */; SELECT 2");
        assertEquals(2, block.size());
        assertEquals("SELECT 2", block.get(1));
    }

    @Test
    void splitsTempTableThenSelect() {
        List<String> stmts = SqlScript.split(
                "CREATE TEMPORARY TABLE tmp AS SELECT * FROM t; SELECT * FROM tmp");
        assertEquals(2, stmts.size());
        assertEquals("CREATE TEMPORARY TABLE tmp AS SELECT * FROM t", stmts.get(0));
        assertEquals("SELECT * FROM tmp", stmts.get(1));
    }

    @Test
    void stripNonCodeBlanksLiteralsAndComments() {
        // 문자열/주석 안의 '?' 는 코드가 아니므로 공백으로 치환된다
        String stripped = SqlScript.stripNonCode("SELECT '??' /* ? */ FROM t WHERE a=?");
        assertEquals(1, stripped.chars().filter(ch -> ch == '?').count());
    }
}
