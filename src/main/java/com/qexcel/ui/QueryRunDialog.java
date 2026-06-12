package com.qexcel.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.qexcel.app.AppContext;
import com.qexcel.model.ParamDef;
import com.qexcel.model.ParamType;
import com.qexcel.model.QueryDef;
import com.qexcel.service.QueryExecuteService.ExecutionOutcome;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * 쿼리 실행 화면: 파라미터 형식에 맞는 입력칸 동적 생성 후 실행.
 * DATE 형식은 달력 선택기를 제공한다.
 */
public class QueryRunDialog extends JDialog {

    private final transient AppContext ctx;
    private final transient QueryDef def;
    private final List<JComponent> inputs = new ArrayList<>();

    public QueryRunDialog(Frame owner, AppContext ctx, QueryDef def) {
        super(owner, "쿼리 실행 - " + def.getQueryName(), Dialog.ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.def = def;
        setSize(420, 360);
        setLocationRelativeTo(owner);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        for (ParamDef p : def.getParams()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            String label = (p.getLabel() != null ? p.getLabel() : "변수 " + p.getSeq())
                    + " (" + p.getType() + ")";
            row.add(new JLabel(label));
            JComponent input = p.getType() == ParamType.DATE ? new DatePicker() : new JTextField(16);
            inputs.add(input);
            row.add(input);
            form.add(row);
        }

        add(form, BorderLayout.CENTER);

        JButton runBtn = new JButton("실행 및 다운로드");
        runBtn.addActionListener(e -> run(runBtn));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(runBtn);
        add(south, BorderLayout.SOUTH);
    }

    private void run(JButton runBtn) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < def.getParams().size(); i++) {
            JComponent c = inputs.get(i);
            if (c instanceof DatePicker dp) {
                if (dp.getDate() == null) {
                    JOptionPane.showMessageDialog(this, "날짜를 선택하세요.");
                    return;
                }
                values.add(dp.getDate());
            } else if (c instanceof JTextField tf) {
                values.add(tf.getText().trim());
            }
        }

        runBtn.setEnabled(false);
        new SwingWorker<ExecutionOutcome, Void>() {
            @Override
            protected ExecutionOutcome doInBackground() throws Exception {
                return ctx.queryExecute().execute(def, values);
            }

            @Override
            protected void done() {
                runBtn.setEnabled(true);
                try {
                    ExecutionOutcome out = get();
                    JOptionPane.showMessageDialog(QueryRunDialog.this,
                            "완료: %d행\n%s\n%s".formatted(
                                    out.rowCount(),
                                    out.excelFile().getAbsolutePath(),
                                    out.sqlFile().getAbsolutePath()));
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(QueryRunDialog.this,
                            "실행 실패: " + cause.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
