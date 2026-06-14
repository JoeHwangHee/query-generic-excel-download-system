package com.qexcel.ui;

import com.qexcel.app.AppContext;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Swing 다이얼로그라 GUI 환경에서만 생성 검증한다(헤드리스에서는 skip).
 */
class QuerySaveDialogTest {

    @Test
    void constructsWithTitle() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        QuerySaveDialog dialog = new QuerySaveDialog(null, new AppContext());
        assertEquals("쿼리 저장", dialog.getTitle());
    }
}
