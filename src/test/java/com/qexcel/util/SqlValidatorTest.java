package com.qexcel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlValidatorTest {

    @Test
    void selectIsAllowed() {
        assertDoesNotThrow(() ->
                SqlValidator.validateSelectOnly("SELECT * FROM t WHERE a = ? AND b = ?"));
    }

    @Test
    void nonSelectIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateSelectOnly("DELETE FROM t"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateSelectOnly("SELECT 1; DROP TABLE t"));
    }

    @Test
    void countsPlaceholdersIgnoringStringLiterals() {
        assertEquals(2, SqlValidator.countPlaceholders("SELECT * FROM t WHERE a=? AND b=?"));
        // 문자열 리터럴 안의 '?'는 제외
        assertEquals(1, SqlValidator.countPlaceholders("SELECT '??' AS x FROM t WHERE a=?"));
        // 주석 안의 '?'도 제외
        assertEquals(1, SqlValidator.countPlaceholders("SELECT a /* ? */ FROM t WHERE a=?"));
    }

    @Test
    void runnableAllowsMultipleSelects() {
        assertDoesNotThrow(() -> SqlValidator.validateRunnable("SELECT a FROM t; SELECT b FROM u"));
    }

    @Test
    void runnableAllowsTempTableThenSelect() {
        assertDoesNotThrow(() -> SqlValidator.validateRunnable(
                "CREATE TEMPORARY TABLE tmp AS SELECT * FROM t; SELECT * FROM tmp"));
        assertDoesNotThrow(() -> SqlValidator.validateRunnable(
                "CREATE LOCAL TEMPORARY TABLE tmp (id INT); SELECT * FROM tmp"));
    }

    @Test
    void runnableRejectsNonTempDdlAndDml() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateRunnable("CREATE TABLE t (id INT); SELECT * FROM t"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateRunnable("SELECT 1; DROP TABLE t"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateRunnable("INSERT INTO t VALUES (1)"));
    }

    @Test
    void runnableRequiresAtLeastOneSelect() {
        // SELECT 없이 임시테이블만 있으면 내보낼 결과가 없어 거부
        assertThrows(IllegalArgumentException.class,
                () -> SqlValidator.validateRunnable("CREATE TEMPORARY TABLE tmp (id INT)"));
        assertThrows(IllegalArgumentException.class, () -> SqlValidator.validateRunnable("   "));
    }
}
