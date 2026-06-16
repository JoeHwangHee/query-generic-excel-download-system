package com.qexcel.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 저장되는 쿼리 정의. queries.md 안에 JSON 형태로 직렬화된다.
 */
public class QueryDef {

    /** 쿼리명 (파일명/식별자로 사용) */
    private String queryName;
    /** '?' 위치파라미터를 포함한 SQL (SELECT 만 허용) */
    private String sql;
    /** '?' 순서대로의 파라미터 정의 */
    private List<ParamDef> params = new ArrayList<>();
    /** 배치 실행 조건 */
    private ScheduleType schedule = ScheduleType.NONE;
    /** 실행 대상 DB 설정명 ({@link DbConnectionDef#getName()} 참조) */
    private String dbName;
    /**
     * 결과의 DATE/DATETIME 컬럼을 엑셀에 출력할 때 적용할 날짜 포맷.
     * (입력 파싱용 {@link ParamDef#getDateFormat()} 과는 별개)
     */
    private DateFormatType outputDateFormat = DateFormatType.YYYY_MM_DD;

    public QueryDef() {
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<ParamDef> getParams() {
        return params;
    }

    public void setParams(List<ParamDef> params) {
        this.params = params;
    }

    public ScheduleType getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleType schedule) {
        this.schedule = schedule;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public DateFormatType getOutputDateFormat() {
        return outputDateFormat;
    }

    public void setOutputDateFormat(DateFormatType outputDateFormat) {
        this.outputDateFormat = outputDateFormat;
    }
}
