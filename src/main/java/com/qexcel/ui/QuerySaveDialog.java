package com.qexcel.ui;

import com.qexcel.app.AppContext;
import com.qexcel.model.DateFormatType;
import com.qexcel.model.DbConnectionDef;
import com.qexcel.model.ParamDef;
import com.qexcel.model.ParamType;
import com.qexcel.model.QueryDef;
import com.qexcel.model.ScheduleType;
import com.qexcel.util.SqlValidator;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * 쿼리 저장 화면: 쿼리명 / SQL / 배치조건 입력 + '?' 별 파라미터 형식 지정.
 */
public class QuerySaveDialog extends JDialog {

    private final transient AppContext ctx;
    private final JTextField nameField = new JTextField(24);
    private final JTextArea sqlArea = new JTextArea(8, 36);
    private final JComboBox<ScheduleType> scheduleBox = new JComboBox<>(ScheduleType.values());
    private final JComboBox<DateFormatType> outputDateBox = new JComboBox<>(DateFormatType.values());
    private final JComboBox<String> dbBox = new JComboBox<>();
    private final JPanel paramPanel = new JPanel();
    private final List<ParamRow> paramRows = new ArrayList<>();

    public QuerySaveDialog(Frame owner, AppContext ctx) {
        super(owner, "쿼리 저장", Dialog.ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        setSize(560, 560);
        setLocationRelativeTo(owner);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(labeled("쿼리명", nameField));
        top.add(new JLabel("SQL ( ? 로 변수 위치 지정 / 여러 SELECT 는 ; 로 구분해 각 결과를 시트로 저장"
                + " / CREATE TEMPORARY TABLE 허용 )"));
        top.add(new JScrollPane(sqlArea));
        JButton parseBtn = new JButton("? 파라미터 인식");
        parseBtn.addActionListener(e -> rebuildParams());
        top.add(parseBtn);
        top.add(labeled("배치조건", scheduleBox));

        // 결과 DATE/DATETIME 컬럼 출력 포맷(쿼리 단위, 기본 YYYY-MM-DD) (#2)
        outputDateBox.setSelectedItem(DateFormatType.YYYY_MM_DD);
        top.add(labeled("출력 날짜포맷", outputDateBox));

        JButton addDbBtn = new JButton("DB 추가");
        addDbBtn.addActionListener(e -> openDbDialog());
        JPanel dbRow = labeled("실행 DB", dbBox);
        dbRow.add(addDbBtn);
        top.add(dbRow);
        reloadDbBox(null);

        top.add(new JLabel("파라미터 형식"));

        paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(paramPanel), BorderLayout.CENTER);

        JButton saveBtn = new JButton("저장");
        saveBtn.addActionListener(e -> save());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(saveBtn);
        add(south, BorderLayout.SOUTH);
    }

    /** 현재 선택된 출력 날짜포맷(테스트/조회용). */
    DateFormatType currentOutputDateFormat() {
        return (DateFormatType) outputDateBox.getSelectedItem();
    }

    private JPanel labeled(String label, java.awt.Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel(label));
        p.add(c);
        return p;
    }

    /** 등록된 DB 목록으로 콤보를 다시 채우고, 지정한 이름을 선택한다. */
    private void reloadDbBox(String select) {
        dbBox.removeAllItems();
        for (DbConnectionDef d : ctx.dbStore().findAll()) {
            dbBox.addItem(d.getName());
        }
        if (select != null) {
            dbBox.setSelectedItem(select);
        }
    }

    private void openDbDialog() {
        DbConnectionDialog dialog = new DbConnectionDialog(
                (Frame) getOwner(), ctx);
        dialog.setVisible(true);
        if (dialog.getSavedName() != null) {
            reloadDbBox(dialog.getSavedName());
        }
    }

    private void rebuildParams() {
        paramPanel.removeAll();
        paramRows.clear();
        int count;
        try {
            count = SqlValidator.countPlaceholders(sqlArea.getText());
        } catch (Exception ex) {
            count = 0;
        }
        for (int i = 0; i < count; i++) {
            ParamRow row = new ParamRow(i + 1);
            paramRows.add(row);
            paramPanel.add(row.panel);
        }
        paramPanel.revalidate();
        paramPanel.repaint();
    }

    private void save() {
        String name = nameField.getText().trim();
        String sql = sqlArea.getText().trim();
        if (name.isEmpty() || sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "쿼리명과 SQL을 입력하세요.");
            return;
        }
        String dbName = (String) dbBox.getSelectedItem();
        if (dbName == null) {
            JOptionPane.showMessageDialog(this, "실행 DB를 선택하거나 'DB 추가'로 등록하세요.");
            return;
        }
        try {
            SqlValidator.validateRunnable(sql);
            int placeholders = SqlValidator.countPlaceholders(sql);
            if (placeholders != paramRows.size()) {
                JOptionPane.showMessageDialog(this, "'? 파라미터 인식'을 다시 눌러 형식을 지정하세요.");
                return;
            }
            QueryDef def = new QueryDef();
            def.setQueryName(name);
            def.setSql(sql);
            def.setSchedule((ScheduleType) scheduleBox.getSelectedItem());
            def.setDbName(dbName);
            def.setOutputDateFormat((DateFormatType) outputDateBox.getSelectedItem());
            List<ParamDef> params = new ArrayList<>();
            for (ParamRow r : paramRows) {
                params.add(r.toParamDef());
            }
            def.setParams(params);

            ctx.queryStore().save(def);
            JOptionPane.showMessageDialog(this, "저장되었습니다.");
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "검증 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** '?' 하나당 형식 지정 행 */
    private static class ParamRow {
        final int seq;
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JComboBox<ParamType> typeBox = new JComboBox<>(ParamType.values());
        final JComboBox<DateFormatType> dateBox = new JComboBox<>(DateFormatType.values());

        ParamRow(int seq) {
            this.seq = seq;
            dateBox.setEnabled(false);
            typeBox.addActionListener(e ->
                    dateBox.setEnabled(typeBox.getSelectedItem() == ParamType.DATE));
            panel.add(new JLabel("?#" + seq + " 형식"));
            panel.add(typeBox);
            panel.add(new JLabel("날짜포맷"));
            panel.add(dateBox);
        }

        ParamDef toParamDef() {
            ParamType type = (ParamType) typeBox.getSelectedItem();
            DateFormatType df = type == ParamType.DATE ? (DateFormatType) dateBox.getSelectedItem() : null;
            return new ParamDef(seq, type, df, "변수 " + seq);
        }
    }
}
