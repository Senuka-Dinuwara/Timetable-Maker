package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import model.Subject;
import model.Teacher;
import util.CsvImporter;
import util.DataStore;

public class TeacherPanel extends JPanel {

    private JTextField nameField;
    private JList<Subject> subjectList;
    private JButton addBtn;
    private int editingRow = -1;
    private DefaultTableModel tableModel;
    private JTable table;

    public TeacherPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4), "Add / Edit Teacher",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        AppTheme.FONT_TITLE, AppTheme.TEXT2)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        nameField = new JTextField(15);
        subjectList = new JList<>();
        subjectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        subjectList.setVisibleRowCount(5);
        subjectList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject s) setText(s.getName());
                return this;
            }
        });
        JScrollPane subScroll = new JScrollPane(subjectList);

        JButton refreshSubjectsBtn = new JButton("Refresh Teachers List");
        AppTheme.styleSecondary(refreshSubjectsBtn);
        addBtn = new JButton("Add Teacher");
        AppTheme.stylePrimary(addBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        AppTheme.styleDanger(deleteBtn);
        JButton clearBtn = new JButton("New / Clear");
        AppTheme.styleSecondary(clearBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; form.add(new JLabel("Teacher Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; form.add(new JLabel("Assign Subjects:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(subScroll, gbc);
        gbc.weightx = 0.0;
        gbc.gridx = 0; gbc.gridy = 2; form.add(refreshSubjectsBtn, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0.0;

        // --- Table ---
        tableModel = new DefaultTableModel(new String[]{"Name", "Subjects", "Blocked Days"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);
        JScrollPane scroll = AppTheme.modernScroll(table);

        // Right-click on a teacher row → edit blocked days
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
            private void maybeShow(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                table.setRowSelectionInterval(row, row);
                JPopupMenu popup = new JPopupMenu();
                JMenuItem editItem = new JMenuItem("Edit Blocked Days\u2026");
                editItem.addActionListener(ev -> showBlockedDaysDialog(DataStore.getTeachers().get(row), row));
                popup.add(editItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // Right-click on "Blocked Days" column header → clear all
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
            private void maybeShow(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int col = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (col != 2) return;
                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem("Clear Blocked Days column");
                item.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(TeacherPanel.this,
                            "Remove all blocked day assignments from every teacher?",
                            "Clear Blocked Days", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                    DataStore.getTeachers().forEach(t -> t.clearBlockedDays());
                    refreshTable();
                });
                popup.add(item);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // ── Import strip ──────────────────────────────────────────────────
        JPanel importStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        importStrip.setBackground(AppTheme.BG);
        importStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 2, 0)));
        JButton importFileBtn = new JButton("Browse File\u2026");
        AppTheme.styleSecondary(importFileBtn);
        JButton importPasteBtn = new JButton("Paste CSV\u2026");
        AppTheme.styleSecondary(importPasteBtn);
        JLabel importStatus = new JLabel(" ");
        importStatus.setFont(AppTheme.FONT_SMALL);
        importStatus.setForeground(AppTheme.TEXT2);
        JButton exportBtn = new JButton("Export CSV\u2026");
        AppTheme.styleSecondary(exportBtn);
        importStrip.add(new JLabel("Import:"));
        importStrip.add(importFileBtn);
        importStrip.add(importPasteBtn);
        importStrip.add(Box.createRigidArea(new Dimension(16, 0)));
        importStrip.add(new JLabel("Export:"));
        importStrip.add(exportBtn);
        importStrip.add(importStatus);

        final String TEACHER_FORMAT = """
                Name, Subjects (semicolon-separated subject names)
                  Subjects must match names already added in the Subjects tab
                  Example : Dr. Smith, Math;Physics""";

        importFileBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            CsvImporter.ImportResult r = CsvImporter.importTeachers(fc.getSelectedFile());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, TEACHER_FORMAT);
            refreshTable();
        });
        importPasteBtn.addActionListener(ev -> {
            String hint = """
                    # Format: Name, Subjects (semicolon-separated subject names)
                    #   Subjects must match names already added in the Subjects tab
                    #   Example: Dr. Smith, Math;Physics
                    # Lines starting with '#' are ignored. First data row = header (skipped).
                    Name, Subjects
                    """;
            JTextArea ta = new JTextArea(hint, 12, 55);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
                    "Paste Teachers CSV",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION || ta.getText().isBlank()) return;
            CsvImporter.ImportResult r = CsvImporter.importTeachersFromText(ta.getText());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, TEACHER_FORMAT);
            refreshTable();
        });

        exportBtn.addActionListener(ev -> {
            if (DataStore.getTeachers().isEmpty()) { importStatus.setText("Nothing to export."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            fc.setDialogTitle("Export Teachers CSV");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".csv")) f = new java.io.File(f.getAbsolutePath() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("Name,Subjects");
                for (Teacher t : DataStore.getTeachers()) {
                    String subjs = t.getSubjects().stream()
                        .map(s -> s.getName().replace(";", " "))
                        .collect(java.util.stream.Collectors.joining(";"));
                    pw.printf("\"%s\",%s%n", t.getName().replace("\"", "\"\""), subjs);
                }
                importStatus.setText("Exported " + DataStore.getTeachers().size() + " teachers.");
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Row selection → populate form ────────────────────────────────
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            editingRow = row;
            Teacher t = DataStore.getTeachers().get(row);
            nameField.setText(t.getName());
            refreshSubjectList();
            subjectList.clearSelection();
            for (int i = 0; i < subjectList.getModel().getSize(); i++) {
                if (t.getSubjects().contains(subjectList.getModel().getElementAt(i)))
                    subjectList.addSelectionInterval(i, i);
            }
            addBtn.setText("Update Teacher");
        });

        JPanel actionsCol = new JPanel(new GridLayout(3, 1, 0, 8));
        actionsCol.setBackground(AppTheme.BG);
        actionsCol.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        actionsCol.add(AppTheme.makeActionCard(
                "\u2795", "Add / Save",
                "Save the current form as a new teacher.",
                new Color(0x2563EB), addBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDDD1", "Delete Selected",
                "Remove the selected teacher(s) from the list.",
                new Color(0xDC2626), deleteBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83E\uDDF9", "New / Clear",
                "Reset the form to add a new entry.",
                new Color(0x64748B), clearBtn));

        JPanel actionsWrap = new JPanel(new BorderLayout());
        actionsWrap.setBackground(AppTheme.BG);
        actionsWrap.add(actionsCol, BorderLayout.NORTH);

        JPanel northSection = new JPanel(new BorderLayout(8, 4));
        northSection.setBackground(AppTheme.BG);
        northSection.add(form,        BorderLayout.CENTER);
        northSection.add(actionsWrap, BorderLayout.EAST);
        northSection.add(importStrip, BorderLayout.SOUTH);
        add(northSection, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // --- Actions ---
        clearBtn.addActionListener(e -> clearForm());
        refreshSubjectsBtn.addActionListener(e -> refreshSubjectList());

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a teacher name.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (editingRow >= 0) {
                Teacher existing = DataStore.getTeachers().get(editingRow);
                existing.setName(name);
                existing.getSubjects().clear();
                for (Subject s : subjectList.getSelectedValuesList()) existing.addSubject(s);
                tableModel.setValueAt(name, editingRow, 0);
                StringBuilder sb = new StringBuilder();
                existing.getSubjects().forEach(s -> sb.append(s.getName()).append(" "));
                tableModel.setValueAt(sb.toString().trim(), editingRow, 1);
                tableModel.setValueAt(blockedDaysLabel(existing), editingRow, 2);
            } else {
                Teacher teacher = new Teacher(name);
                for (Subject s : subjectList.getSelectedValuesList()) teacher.addSubject(s);
                DataStore.addTeacher(teacher);
                StringBuilder sb = new StringBuilder();
                teacher.getSubjects().forEach(s -> sb.append(s.getName()).append(" "));
                tableModel.addRow(new Object[]{name, sb.toString().trim(), ""});
            }
            clearForm();
        });

        deleteBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more teachers to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (int i = rows.length - 1; i >= 0; i--) {
                DataStore.removeTeacher(DataStore.getTeachers().get(rows[i]));
                tableModel.removeRow(rows[i]);
            }
            clearForm();
        });
    }

    private void showImportErrors(List<String> errors, String formatHint) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected format  (first row = header, skipped):\n");
        sb.append("  ").append(formatHint.replace("\n", "\n  ")).append("\n\n");
        sb.append("Issues found (").append(errors.size()).append("):\n");
        for (String e : errors) sb.append("  \u2022 ").append(e).append("\n");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(560, 280));
        JOptionPane.showMessageDialog(this, sp, "Import Warnings / Errors", JOptionPane.WARNING_MESSAGE);
    }

    private void clearForm() {
        editingRow = -1;
        table.clearSelection();
        nameField.setText("");
        subjectList.clearSelection();
        addBtn.setText("Add Teacher");
    }

    private void refreshSubjectList() {
        DefaultListModel<Subject> model = new DefaultListModel<>();
        for (Subject s : DataStore.getSubjects()) model.addElement(s);
        subjectList.setModel(model);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Teacher t : DataStore.getTeachers()) {
            StringBuilder subjects = new StringBuilder();
            t.getSubjects().forEach(s -> subjects.append(s.getName()).append(" "));
            tableModel.addRow(new Object[]{t.getName(), subjects.toString().trim(), blockedDaysLabel(t)});
        }
    }

    private String blockedDaysLabel(Teacher t) {
        return t.getBlockedDays().isEmpty() ? "" : String.join(", ", t.getBlockedDays());
    }

    private void showBlockedDaysDialog(Teacher teacher, int row) {
        String[] allDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        JCheckBox[] boxes = new JCheckBox[allDays.length];
        JPanel panel = new JPanel(new GridLayout(allDays.length, 1, 0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        for (int i = 0; i < allDays.length; i++) {
            boxes[i] = new JCheckBox(allDays[i]);
            boxes[i].setSelected(teacher.getBlockedDays().contains(allDays[i]));
            panel.add(boxes[i]);
        }
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Blocked Days \u2014 " + teacher.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        teacher.clearBlockedDays();
        for (int i = 0; i < allDays.length; i++) {
            if (boxes[i].isSelected()) teacher.blockDay(allDays[i]);
        }
        tableModel.setValueAt(blockedDaysLabel(teacher), row, 2);
    }
}
