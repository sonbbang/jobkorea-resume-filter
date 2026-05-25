package job;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

public class ResumeFilterGui extends JFrame {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ResumeFilterGui.class);
    private static final String PREF_LAST_FOLDER = "lastFolder";

    private JTextField folderField;
    private JTextField outputFolderField;
    private JTextField keywordsField;
    private JTextArea jdTextArea;
    private JComboBox<String> formatCombo;
    private JButton runButton;
    private JTextArea logArea;

    public ResumeFilterGui() {
        setTitle("이력서 필터링 도구");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(750, 700);
        setLocationRelativeTo(null);
        buildUI();
        redirectSystemOut();
    }

    private void buildUI() {
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(12, 12, 6, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // 이력서 폴더
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        top.add(new JLabel("이력서 폴더:"), c);

        String initFolder = PREFS.get(PREF_LAST_FOLDER, System.getProperty("user.home") + File.separator + "Downloads");
        folderField = new JTextField(initFolder);
        c.gridx = 1; c.weightx = 1.0;
        top.add(folderField, c);

        JButton browseBtn = new JButton("찾아보기");
        c.gridx = 2; c.weightx = 0;
        top.add(browseBtn, c);

        browseBtn.addActionListener(e -> {
            String current = folderField.getText().trim();
            JFileChooser chooser = new JFileChooser(current.isEmpty() ? null : new java.io.File(current));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("이력서 폴더 선택");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String selected = chooser.getSelectedFile().getAbsolutePath();
                folderField.setText(selected);
                PREFS.put(PREF_LAST_FOLDER, selected);
            }
        });

        // 출력 폴더
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        top.add(new JLabel("출력 폴더명:"), c);

        outputFolderField = new JTextField("out");
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 2;
        top.add(outputFolderField, c);
        c.gridwidth = 1;

        // 키워드
        c.gridx = 0; c.gridy = 2; c.weightx = 0;
        top.add(new JLabel("키워드:"), c);

        keywordsField = new JTextField("팀장");
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 2;
        top.add(keywordsField, c);
        c.gridwidth = 1;

        // 공고 내용 붙여넣기 (선택)
        c.gridx = 0; c.gridy = 3; c.weightx = 0; c.anchor = GridBagConstraints.NORTHWEST;
        top.add(new JLabel("<html>공고 내용<br>(선택):</html>"), c);
        c.anchor = GridBagConstraints.CENTER;

        jdTextArea = new JTextArea(5, 20);
        jdTextArea.setLineWrap(true);
        jdTextArea.setWrapStyleWord(true);
        jdTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        jdTextArea.setToolTipText("채용공고 내용을 복사해서 붙여넣으면 키워드 매칭 점수가 계산됩니다");
        JScrollPane jdScroll = new JScrollPane(jdTextArea);
        jdScroll.setPreferredSize(new Dimension(0, 100));
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 2;
        top.add(jdScroll, c);
        c.gridwidth = 1;

        // 이력서 형식 + 실행 버튼
        c.gridx = 0; c.gridy = 4; c.weightx = 0;
        top.add(new JLabel("이력서 형식:"), c);

        formatCombo = new JComboBox<>(new String[]{"잡코리아 (jobkorea)", "사람인 (saramin)"});
        formatCombo.setSelectedIndex(0);
        c.gridx = 1; c.weightx = 1.0;
        top.add(formatCombo, c);

        runButton = new JButton("실  행");
        runButton.setPreferredSize(new Dimension(90, 28));
        c.gridx = 2; c.weightx = 0;
        top.add(runButton, c);

        runButton.addActionListener(e -> runFilter());

        // 로그 영역
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(new TitledBorder("실행 로그"));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private void redirectSystemOut() {
        OutputStream out = new OutputStream() {
            private final StringBuilder buf = new StringBuilder();

            @Override
            public void write(int b) {
                buf.append((char) b);
                if (b == '\n') flush();
            }

            @Override
            public void write(byte[] b, int off, int len) {
                buf.append(new String(b, off, len, StandardCharsets.UTF_8));
                flush();
            }

            @Override
            public void flush() {
                if (buf.length() == 0) return;
                String text = buf.toString();
                buf.setLength(0);
                SwingUtilities.invokeLater(() -> {
                    logArea.append(text);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        };

        PrintStream ps = new PrintStream(out, true, StandardCharsets.UTF_8);
        System.setOut(ps);
        System.setErr(ps);
    }

    private void runFilter() {
        String folder = folderField.getText().trim();
        if (folder.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이력서 폴더를 선택해주세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String outputFolder = outputFolderField.getText().trim();
        if (outputFolder.isEmpty()) outputFolder = "out";

        String keywords = keywordsField.getText().trim();

        PDFConfig.ResumeFormatType type = formatCombo.getSelectedIndex() == 0
                ? PDFConfig.ResumeFormatType.JOBKOREA
                : PDFConfig.ResumeFormatType.SARAMIN;

        PDFConfig.setOutputFolder(outputFolder);
        PDFConfig.setKeywords(keywords);

        String jdText = jdTextArea.getText().trim();

        runButton.setEnabled(false);
        logArea.setText("");

        new Thread(() -> {
            try {
                ResumeFilter.run(folder, type, jdText.isEmpty() ? null : jdText);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(ResumeFilterGui.this, "처리가 완료되었습니다.", "완료", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                System.err.println("오류: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> runButton.setEnabled(true));
            }
        }).start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ResumeFilterGui().setVisible(true));
    }
}
