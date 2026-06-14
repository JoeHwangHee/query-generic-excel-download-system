package com.qexcel.db;

import com.qexcel.model.QueryResult;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericQueryRunnerTest {

    private final GenericQueryRunner runner = new GenericQueryRunner();
    private HikariDataSource ds;

    @BeforeEach
    void setUp() throws Exception {
        ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:runner;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setDriverClassName("org.h2.Driver");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE emp (id INT, name VARCHAR(20))");
            st.execute("INSERT INTO emp VALUES (1, '김'), (2, '이')");
        }
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    @Test
    void executesParameterizedQueryPreservingColumnOrder() throws Exception {
        // 별칭(AS) 컬럼 순서가 결과에 그대로 보존되어야 한다
        QueryResult result = runner.execute(
                ds, "SELECT id AS no, name AS nm FROM emp WHERE id = ?", List.of(2));

        assertEquals(List.of("no", "nm"), result.getColumns());
        assertEquals(1, result.getRowCount());
        assertEquals(2, ((Number) result.getRows().get(0).get("no")).intValue());
        assertEquals("이", result.getRows().get(0).get("nm"));
    }

    @Test
    void returnsEmptyResultWhenNoRowsMatch() throws Exception {
        QueryResult result = runner.execute(
                ds, "SELECT id FROM emp WHERE id = ?", List.of(999));
        assertEquals(0, result.getRowCount());
    }
}
