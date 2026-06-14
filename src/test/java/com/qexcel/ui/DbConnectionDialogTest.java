package com.qexcel.ui;

import com.qexcel.app.AppContext;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Swing 다이얼로그라 GUI 환경에서만 생성 검증한다(헤드리스에서는 skip).
 */
class DbConnectionDialogTest {

    @Test
    void savedNameIsNullBeforeSaving() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        DbConnectionDialog dialog = new DbConnectionDialog(null, new AppContext());
        assertNull(dialog.getSavedName());
    }
}
