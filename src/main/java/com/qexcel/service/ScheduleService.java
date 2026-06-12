package com.qexcel.service;

import com.qexcel.model.ScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 배치 조건 계산.
 *
 * <p>요구사항 4: "오늘이 특정조건에 들어가는 날이거나 그 다음날이면" 실행 대상.
 * "그 다음날" 규칙은 지정일에 앱을 켜지 못한 경우를 보완하기 위한 것으로,
 * 여기서는 단순히 오늘 또는 어제가 배치일인지로 판정한다.
 * (주말/공휴일 스킵 규칙이 확정되면 isDue 로직을 보강.)</p>
 */
public class ScheduleService {

    /** 주어진 날짜가 해당 스케줄의 배치일인가? */
    public boolean isScheduledDay(ScheduleType type, LocalDate date) {
        return switch (type) {
            case NONE -> false;
            case EVERY_MONDAY -> date.getDayOfWeek() == DayOfWeek.MONDAY;
            case FIRST_MONDAY -> date.equals(firstMonday(date));
            case DAY_1 -> date.getDayOfMonth() == 1;
            case DAY_26 -> date.getDayOfMonth() == 26;
            case LAST_WEEK_MONDAY -> date.equals(lastMonday(date));
        };
    }

    /**
     * 오늘 기준으로 배치 대상인지 여부.
     * "오늘이 배치일" 또는 "어제가 배치일(미실행 보완)" 이면서
     * 아직 이번 회차를 실행하지 않은 경우 true.
     *
     * @param lastRun 마지막 실행일 (없으면 null)
     */
    public boolean isDue(ScheduleType type, LocalDate today, LocalDate lastRun) {
        if (type == ScheduleType.NONE) {
            return false;
        }
        LocalDate due = null;
        if (isScheduledDay(type, today)) {
            due = today;
        } else if (isScheduledDay(type, today.minusDays(1))) {
            due = today.minusDays(1); // 그 다음날 보완 실행
        }
        if (due == null) {
            return false;
        }
        // 해당 배치일 당일 이후로 이미 실행했다면 재알림하지 않음
        return lastRun == null || lastRun.isBefore(due);
    }

    private LocalDate firstMonday(LocalDate ref) {
        return ref.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    }

    private LocalDate lastMonday(LocalDate ref) {
        return ref.with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY));
    }
}
