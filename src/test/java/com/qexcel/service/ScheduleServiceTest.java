package com.qexcel.service;

import com.qexcel.model.ScheduleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleServiceTest {

    private final ScheduleService svc = new ScheduleService();

    @Test
    void everyMonday() {
        // 2026-06-08 은 월요일
        assertTrue(svc.isScheduledDay(ScheduleType.EVERY_MONDAY, LocalDate.of(2026, 6, 8)));
        assertFalse(svc.isScheduledDay(ScheduleType.EVERY_MONDAY, LocalDate.of(2026, 6, 9)));
    }

    @Test
    void firstAndLastMonday() {
        // 2026-06: 첫째 월요일 1일, 마지막 월요일 29일
        assertTrue(svc.isScheduledDay(ScheduleType.FIRST_MONDAY, LocalDate.of(2026, 6, 1)));
        assertTrue(svc.isScheduledDay(ScheduleType.LAST_WEEK_MONDAY, LocalDate.of(2026, 6, 29)));
    }

    @Test
    void dueIncludesNextDayWhenNotRun() {
        // 어제(6/8 월)가 배치일이고 아직 실행 안 함 -> 오늘(6/9) 실행 대상
        assertTrue(svc.isDue(ScheduleType.EVERY_MONDAY, LocalDate.of(2026, 6, 9), null));
        // 어제 이미 실행했다면 대상 아님
        assertFalse(svc.isDue(ScheduleType.EVERY_MONDAY, LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 8)));
    }
}
