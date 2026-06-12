package com.qexcel.ui;

import com.qexcel.app.AppContext;
import com.qexcel.model.QueryDef;

import javax.swing.JOptionPane;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 요구사항 4: 기동 시 오늘(또는 미실행으로 인한 익일)이 배치일인 쿼리를 찾아
 * alert 후 하나씩 실행창을 띄운다. 여러 개면 순차 처리한다.
 */
public class BatchAlertFlow {

    private final AppContext ctx;
    private final MainFrame frame;

    public BatchAlertFlow(AppContext ctx, MainFrame frame) {
        this.ctx = ctx;
        this.frame = frame;
    }

    public void checkAndRun() {
        LocalDate today = LocalDate.now();
        List<QueryDef> due = new ArrayList<>();
        for (QueryDef d : ctx.queryStore().findAll()) {
            LocalDate lastRun = ctx.runHistory().getLastRun(d.getQueryName());
            if (ctx.schedule().isDue(d.getSchedule(), today, lastRun)) {
                due.add(d);
            }
        }
        if (due.isEmpty()) {
            return;
        }

        StringBuilder names = new StringBuilder();
        for (QueryDef d : due) {
            names.append("\n - ").append(d.getQueryName());
        }
        JOptionPane.showMessageDialog(frame,
                "오늘 실행 대상 배치 쿼리가 " + due.size() + "건 있습니다:" + names,
                "배치 알림", JOptionPane.INFORMATION_MESSAGE);

        // 하나씩 순차 실행 (모달 다이얼로그라 순서대로 진행됨)
        for (QueryDef d : due) {
            new QueryRunDialog(frame, ctx, d).setVisible(true);
        }
        frame.refresh();
    }
}
