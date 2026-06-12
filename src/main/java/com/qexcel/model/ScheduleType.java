package com.qexcel.model;

/**
 * 쿼리의 배치 실행 조건.
 *
 * <p>주의: 원 요구사항의 "매주 1일 / 매주 26일" 표현은 주/월 단위가 혼재되어
 * 모호했으므로, 아래와 같이 해석하여 정의한다. 의미 확정 시 ScheduleService 의
 * 계산 로직만 수정하면 된다.</p>
 */
public enum ScheduleType {
    /** 배치 없음 */
    NONE,
    /** 매주 월요일 */
    EVERY_MONDAY,
    /** 매월 첫째 주 월요일 */
    FIRST_MONDAY,
    /** 매월 1일 */
    DAY_1,
    /** 매월 26일 */
    DAY_26,
    /** 매월 마지막 주 월요일 */
    LAST_WEEK_MONDAY
}
