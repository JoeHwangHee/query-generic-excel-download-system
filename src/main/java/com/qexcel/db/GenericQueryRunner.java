package com.qexcel.db;

import com.qexcel.model.QueryResult;
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

/**
 * 임의의 '?' 위치파라미터 SQL을 실행하고, 컬럼 순서를 보존한
 * List&lt;LinkedHashMap&gt; 형태로 결과를 돌려준다.
 *
 * <p>MyBatis의 resultType=map 은 HashMap 이라 컬럼 순서가 깨지므로, 동적 컬럼
 * 결과는 순수 JDBC + ResultSetMetaData 로 처리한다.</p>
 */
public class GenericQueryRunner {

    private static final Logger log = LoggerFactory.getLogger(GenericQueryRunner.class);

    /**
     * @param dataSource 실행 대상 DB 의 DataSource (쿼리마다 다를 수 있어 인자로 받는다)
     * @param sql        '?' 위치파라미터 포함 SELECT
     * @param params     '?' 순서대로 바인딩할 값 (문자열/숫자)
     */
    public QueryResult execute(DataSource dataSource, String sql, List<Object> params) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>(colCount);
                for (int c = 1; c <= colCount; c++) {
                    // 별칭(AS) 우선
                    columns.add(meta.getColumnLabel(c));
                }

                List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= colCount; c++) {
                        row.put(columns.get(c - 1), rs.getObject(c));
                    }
                    rows.add(row);
                }

                log.info("쿼리 실행 완료: {}행, {}ms", rows.size(), System.currentTimeMillis() - start);
                return new QueryResult(columns, rows);
            }
        }
    }
}
