package com.qexcel.ui;

import com.qexcel.app.AppContext;
import com.qexcel.model.DbConnectionDef;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;

/**
 * DB 접속 정보 추가 화면: 설정명 / JDBC URL / 사용자 / 비밀번호 / 드라이버 입력 +
 * 연결 테스트 + 저장. 저장 시 {@code DbStoreService} 를 통해 파일에 즉시 반영된다.
 */
public class DbConnectionDialog extends JDialog {

    private final transient AppContext ctx;
    private final JTextField nameField = new JTextField(20);
    private final JTextField urlField = new JTextField(28);
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passField = new JPasswordField(16);
    /** 드라이버: 프리셋(MySQL/MariaDB) 드롭다운 + 직접 입력 허용 */
    private final JComboBox<String> driverBox = new JComboBox<>(
            new String[]{DbConnectionDef.DEFAULT_DRIVER, DbConnectionDef.MARIADB_DRIVER});

    /** 저장에 성공한 설정명(호출측에서 콤보 선택용). 저장 전에는 null. */
    private transient String savedName;

    public DbConnectionDialog(Frame owner, AppContext ctx) {
        super(owner, "DB 추가", Dialog.ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        driverBox.setEditable(true); // 프리셋 외 임의 드라이버 직접 입력 허용
        setSize(520, 320);
        setLocationRelativeTo(owner);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(labeled("설정명", nameField));
        form.add(labeled("JDBC URL", urlField));
        form.add(labeled("사용자", userField));
        form.add(labeled("비밀번호", passField));
        form.add(labeled("드라이버", driverBox));
        add(form, BorderLayout.CENTER);

        JButton testBtn = new JButton("연결 테스트");
        testBtn.addActionListener(e -> testConnection(testBtn));
        JButton saveBtn = new JButton("저장");
        saveBtn.addActionListener(e -> save());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(testBtn);
        south.add(saveBtn);
        add(south, BorderLayout.SOUTH);
    }

    public String getSavedName() {
        return savedName;
    }

    /** 콤보(편집형)에서 현재 드라이버 문자열을 추출한다(직접 입력 포함). */
    String currentDriver() {
        Object item = driverBox.isEditable()
                ? driverBox.getEditor().getItem()
                : driverBox.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private JPanel labeled(String label, java.awt.Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel(label));
        p.add(c);
        return p;
    }

    /** 입력값으로 DbConnectionDef 구성. 필수값 누락 시 안내 후 null 반환. */
    private DbConnectionDef collect() {
        String name = nameField.getText().trim();
        String url = urlField.getText().trim();
        String driver = currentDriver();
        if (name.isEmpty() || url.isEmpty() || driver.isEmpty()) {
            JOptionPane.showMessageDialog(this, "설정명 / JDBC URL / 드라이버는 필수입니다.");
            return null;
        }
        DbConnectionDef def = new DbConnectionDef();
        def.setName(name);
        def.setJdbcUrl(url);
        def.setUsername(userField.getText().trim());
        def.setPassword(new String(passField.getPassword()));
        def.setDriver(driver);
        return def;
    }

    private void testConnection(JButton testBtn) {
        DbConnectionDef def = collect();
        if (def == null) {
            return;
        }
        testBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ctx.dataSources().testConnection(def);
                return null;
            }

            @Override
            protected void done() {
                testBtn.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(DbConnectionDialog.this,
                            "접속 성공", "연결 테스트", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(DbConnectionDialog.this,
                            "접속 실패: " + cause.getMessage(), "연결 테스트", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void save() {
        DbConnectionDef def = collect();
        if (def == null) {
            return;
        }
        ctx.dbStore().save(def);
        ctx.dataSources().invalidate(def.getName()); // 동일 이름 재정의 시 기존 풀 폐기
        savedName = def.getName();
        JOptionPane.showMessageDialog(this, "저장되었습니다.");
        dispose();
    }
}
