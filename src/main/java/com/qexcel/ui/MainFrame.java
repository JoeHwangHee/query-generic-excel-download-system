package com.qexcel.ui;

import com.qexcel.app.AppContext;
import com.qexcel.model.QueryDef;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * 메인 화면: 저장된 쿼리 목록 + 신규/실행/삭제.
 */
public class MainFrame extends JFrame {

    private final transient AppContext ctx;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> queryList = new JList<>(listModel);

    public MainFrame(AppContext ctx) {
        super("쿼리 기반 엑셀 다운로드");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);

        queryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(queryList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newBtn = new JButton("새 쿼리");
        JButton runBtn = new JButton("실행");
        JButton delBtn = new JButton("삭제");
        buttons.add(newBtn);
        buttons.add(runBtn);
        buttons.add(delBtn);
        add(buttons, BorderLayout.SOUTH);

        newBtn.addActionListener(e -> openSaveDialog());
        runBtn.addActionListener(e -> openRunDialog());
        delBtn.addActionListener(e -> deleteSelected());

        refresh();
    }

    public void refresh() {
        listModel.clear();
        for (QueryDef d : ctx.queryStore().findAll()) {
            listModel.addElement(d.getQueryName());
        }
    }

    private void openSaveDialog() {
        new QuerySaveDialog(this, ctx).setVisible(true);
        refresh();
    }

    private void openRunDialog() {
        String name = queryList.getSelectedValue();
        if (name == null) {
            JOptionPane.showMessageDialog(this, "쿼리를 선택하세요.");
            return;
        }
        QueryDef def = ctx.queryStore().find(name);
        new QueryRunDialog(this, ctx, def).setVisible(true);
    }

    private void deleteSelected() {
        String name = queryList.getSelectedValue();
        if (name == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "'" + name + "' 쿼리를 삭제할까요?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            ctx.queryStore().delete(name);
            refresh();
        }
    }
}
