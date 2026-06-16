package com.qexcel.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 런타임 클래스패스에 JDBC 드라이버가 존재하는지 확인한다(#1).
 * 폐쇄망 오프라인 빌드에 드라이버 jar 가 누락되면 여기서 잡힌다.
 */
class DriverAvailabilityTest {

    @Test
    void mariadbDriverIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.mariadb.jdbc.Driver"));
    }

    @Test
    void mysqlDriverIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName("com.mysql.cj.jdbc.Driver"));
    }
}
