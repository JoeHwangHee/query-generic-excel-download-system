package com.qexcel.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 배치 중복 실행 방지를 위한 쿼리별 마지막 실행일 기록.
 * run-history.json 으로 영속화된다. (쿼리 정의는 런타임 메모리에만 올라가지만,
 * 실행이력은 재기동 후에도 "오늘/익일 미실행 여부"를 판정해야 하므로 영속화한다.)
 */
public class RunHistory {

    /** queryName -> 마지막 실행일(ISO yyyy-MM-dd) */
    private Map<String, LocalDate> lastRunDates = new HashMap<>();

    public Map<String, LocalDate> getLastRunDates() {
        return lastRunDates;
    }

    public void setLastRunDates(Map<String, LocalDate> lastRunDates) {
        this.lastRunDates = lastRunDates;
    }

    public LocalDate getLastRun(String queryName) {
        return lastRunDates.get(queryName);
    }

    public void markRun(String queryName, LocalDate date) {
        lastRunDates.put(queryName, date);
    }
}
