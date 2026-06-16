package com.qexcel.db;

import com.qexcel.model.QueryResult;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        // 인메모리 DB가 DB_CLOSE_DELAY=-1 로 JVM 동안 유지되므로 테스트 간 테이블이 남는다.
        // 매 setUp 을 멱등하게 만들어 격리한다.
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE IF EXISTS emp");
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
        // 별칭(AS) 컬럼 순서가 결과에 그대로 보존되어야 한다.
        // 운영 DB(MySQL)는 따옴표 없는 별칭의 대소문자를 보존하지만 H2 는 대문자로 올리므로,
        // 테스트에서는 별칭을 따옴표로 감싸 대소문자를 명시 보존시킨다.
        QueryResult result = runner.execute(
                ds, "SELECT id AS \"no\", name AS \"nm\" FROM emp WHERE id = ?", List.of(2));

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

    @Test
    void executeScriptRunsMultipleSelectsInOrder() throws Exception {
        // 구문별로 '?' 파라미터가 순서대로 분배되어야 한다(구문1: 1개, 구문2: 1개).
        // H2 는 따옴표 없는 별칭을 대문자로 올리므로 별칭을 따옴표로 명시한다.
        List<QueryResult> results = runner.executeScript(
                ds,
                List.of("SELECT name AS \"nm\" FROM emp WHERE id = ?",
                        "SELECT id AS \"no\" FROM emp WHERE name = ?"),
                List.of(1, "이"));

        assertEquals(2, results.size());
        assertEquals("김", results.get(0).getRows().get(0).get("nm"));
        assertEquals(2, ((Number) results.get(1).getRows().get(0).get("no")).intValue());
    }

    @Test
    void executeScriptCreatesTemporaryTableVisibleToLaterSelect() throws Exception {
        // 임시테이블 DDL 은 같은 커넥션에서 먼저 실행되고, SELECT 결과만 반환된다
        List<QueryResult> results = runner.executeScript(
                ds,
                List.of("CREATE LOCAL TEMPORARY TABLE tmp AS SELECT id FROM emp WHERE id = ?",
                        "SELECT id AS \"no\" FROM tmp"),
                List.of(2));

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getRowCount());
        assertEquals(2, ((Number) results.get(0).getRows().get(0).get("no")).intValue());
    }

    @Test
    void collectsColumnSqlTypes() throws Exception {
        QueryResult result = runner.execute(
                ds, "SELECT CAST(? AS DATE) AS d, id FROM emp WHERE id = ?", List.of("2026-06-16", 1));
        // 첫 컬럼은 DATE 타입으로 수집되어야 한다
        assertEquals(Types.DATE, result.getColumnTypes().get(0));
        assertTrue(result.getColumnTypes().size() == 2);
    }
}
