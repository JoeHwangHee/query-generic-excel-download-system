package com.qexcel.model;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 쿼리 실행 결과. 컬럼 순서를 보존하기 위해 행을 LinkedHashMap 으로 담는다.
 */
public class QueryResult {

    /** ResultSetMetaData 순서대로의 컬럼명 */
    private final List<String> columns;
    /** 행 목록 (컬럼명 -> 값), 컬럼 순서 보존 */
    private final List<LinkedHashMap<String, Object>> rows;

    public QueryResult(List<String> columns, List<LinkedHashMap<String, Object>> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<LinkedHashMap<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }
}
