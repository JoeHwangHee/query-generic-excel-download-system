package com.qexcel.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DbConnectionExceptionTest {

    @Test
    void keepsMessage() {
        DbConnectionException ex = new DbConnectionException("접속 불가");
        assertEquals("접속 불가", ex.getMessage());
    }

    @Test
    void keepsCause() {
        Throwable cause = new RuntimeException("원인");
        DbConnectionException ex = new DbConnectionException("접속 불가", cause);
        assertEquals("접속 불가", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
