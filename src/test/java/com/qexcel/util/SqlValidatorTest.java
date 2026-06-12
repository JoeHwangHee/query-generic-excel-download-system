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
    }
}
