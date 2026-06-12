package com.qexcel.model;

/**
 * ParamType.DATE 일 때 사용하는 날짜 문자열 포맷.
 * 달력에서 선택한 날짜를 이 포맷의 문자열로 변환하여 쿼리에 바인딩한다.
 */
public enum DateFormatType {
    YYYYMMDD("yyyyMMdd"),
    YYYY_MM_DD("yyyy-MM-dd");

    private final String pattern;

    DateFormatType(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
