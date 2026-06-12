package com.qexcel.model;

/**
 * 쿼리의 '?' 위치에 바인딩되는 입력값의 데이터 형식.
 */
public enum ParamType {
    /** 단순 텍스트 */
    TEXT,
    /** 숫자 */
    NUMBER,
    /** 날짜형식 텍스트 (dateFormat 으로 세부 포맷 지정) */
    DATE
}
