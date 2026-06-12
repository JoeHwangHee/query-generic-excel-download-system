package com.qexcel.app;

import com.qexcel.ui.BatchAlertFlow;
import com.qexcel.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

/**
 * 애플리케이션 진입점.
 * 기동 시: 쿼리 캐시 적재 -> 배치 대상 점검(alert) -> 메인 화면 표시.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        AppContext ctx = new AppContext();
        ctx.start();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(ctx);
            frame.setVisible(true);

            // 요구사항 4: 기동 시 배치 대상이면 alert 후 하나씩 실행창 표시
            new BatchAlertFlow(ctx, frame).checkAndRun();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("종료 처리");
            com.qexcel.db.DataSourceProvider.shutdown();
        }));
    }
}
