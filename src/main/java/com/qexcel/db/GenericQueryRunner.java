package com.qexcel.db;

import com.qexcel.model.QueryResult;
import com.qexcel.util.SqlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 임의의 '?' 위치파라미터 SQL을 실행하고, 컬럼 순서를 보존한
 * List&lt;LinkedHashMap&gt; 형태로 결과를 돌려준다.
 *
 * <p>MyBatis의 resultType=map 은 HashMap 이라 컬럼 순서가 깨지므로, 동적 컬럼
 * 결과는 순수 JDBC + ResultSetMetaData 로 처리한다.</p>
 */
public class GenericQueryRunner {

    private static final Logger log = LoggerFactory.getLogger(GenericQueryRunner.class);

    private static final Pattern LEADING_SELECT =
            Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 단일 SELECT 실행(하위호환).
     *
     * @param dataSource 실행 대상 DB 의 DataSource (쿼리마다 다를 수 있어 인자로 받는다)
     * @param sql        '?' 위치파라미터 포함 SELECT
     * @param params     '?' 순서대로 바인딩할 값 (문자열/숫자)
     */
    public QueryResult execute(DataSource dataSource, String sql, List<Object> params) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            QueryResult result = runSelect(conn, sql, params);
            log.info("쿼리 실행 완료: {}행, {}ms", result.getRowCount(), System.currentTimeMillis() - start);
            return result;
        }
    }

    /**
     * 여러 구문을 <b>하나의 커넥션</b>에서 순서대로 실행한다.
     * SELECT 는 결과를 수집해 반환하고, CREATE TEMPORARY TABLE 등 비-SELECT 는 실행만 한다.
     * 임시테이블이 같은 세션(커넥션)에서 이후 SELECT 에 보이도록 보장한다.
     *
     * @param statements {@code SqlScript.split} 으로 분리된 구문들(순서 보존)
     * @param params     전체 구문의 '?' 순서대로의 바인딩 값. 구문별 '?' 개수만큼 순서대로 분배된다.
     * @return SELECT 구문들의 결과(순서 보존)
     */
    public List<QueryResult> executeScript(DataSource dataSource, List<String> statements,
                                           List<Object> params) throws SQLException {
        long start = System.currentTimeMillis();
        boolean hasNonSelect = statements.stream().anyMatch(s -> !isSelect(s));

        List<QueryResult> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            if (hasNonSelect) {
                // 임시테이블 DDL 쓰기 허용. HikariCP 는 반납 시 풀 기본값(readOnly)으로 복원한다.
                conn.setReadOnly(false);
            }
            int paramIdx = 0;
            for (String stmt : statements) {
                int need = SqlValidator.countPlaceholders(stmt);
                List<Object> slice = params.subList(paramIdx, paramIdx + need);
                paramIdx += need;
                if (isSelect(stmt)) {
                    results.add(runSelect(conn, stmt, slice));
                } else {
                    runUpdate(conn, stmt, slice);
                }
            }
        }
        log.info("스크립트 실행 완료: {}구문, {}개 SELECT 결과, {}ms",
                statements.size(), results.size(), System.currentTimeMillis() - start);
        return results;
    }

    private static boolean isSelect(String stmt) {
        return LEADING_SELECT.matcher(stmt).find();
    }

    private QueryResult runSelect(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>(colCount);
                List<Integer> columnTypes = new ArrayList<>(colCount);
                for (int c = 1; c <= colCount; c++) {
                    columns.add(meta.getColumnLabel(c)); // 별칭(AS) 우선
                    columnTypes.add(meta.getColumnType(c));
                }

                List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= colCount; c++) {
                        row.put(columns.get(c - 1), rs.getObject(c));
                    }
                    rows.add(row);
                }
                return new QueryResult(columns, columnTypes, rows);
            }
        }
    }

    private void runUpdate(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            ps.execute();
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
