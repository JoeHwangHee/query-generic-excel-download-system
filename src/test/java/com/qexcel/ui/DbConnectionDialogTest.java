package com.qexcel.ui;

import com.qexcel.app.AppContext;
import com.qexcel.model.DbConnectionDef;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void driverComboDefaultsToMySql() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        // 편집형 콤보의 기본 드라이버는 MySQL 이어야 한다(#1)
        DbConnectionDialog dialog = new DbConnectionDialog(null, new AppContext());
        assertEquals(DbConnectionDef.DEFAULT_DRIVER, dialog.currentDriver());
    }
}
