package ui;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import model.Student;
import model.StudentGroup;
import model.Subject;
import util.CsvImporter;
import util.DataStore;

public class StudentPanel extends JPanel {

    private final JTextField nameField;
    private final JComboBox<String> groupBox;
    private final DefaultListModel<Subject> electiveListModel;
    private final JList<Subject> electiveList;
    private JButton addBtn;
    private int editingRow = -1;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public StudentPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4), "Add / Edit Student",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        AppTheme.FONT_TITLE, AppTheme.TEXT2)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        nameField = new JTextField(15);
        groupBox = new JComboBox<>();
        electiveListModel = new DefaultListModel<>();
        electiveList = new JList<>(electiveListModel);
        electiveList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        electiveList.setVisibleRowCount(5);
        electiveList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject s) setText(s.getName());
                return this;
            }
        });
        addBtn = new JButton("Add Student");
        AppTheme.stylePrimary(addBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        AppTheme.styleDanger(deleteBtn);
        JButton clearBtn = new JButton("New / Clear");
        AppTheme.styleSecondary(clearBtn);
        JButton refreshDataBtn = new JButton("Refresh Students Lists");
        AppTheme.styleSecondary(refreshDataBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; form.add(new JLabel("Student Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; form.add(new JLabel("Group:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(groupBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; form.add(new JLabel("Assign Subjects:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; JScrollPane subScroll = new JScrollPane(electiveList); form.add(subScroll, gbc);
        gbc.weightx = 0.0;
        gbc.gridx = 0; gbc.gridy = 3; form.add(refreshDataBtn, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0.0;

        // ── Import strip ─────────────────────────────────────────────────────
        JPanel importStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        importStrip.setBackground(AppTheme.BG);
        importStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 2, 0)));
        JButton importFileBtn  = new JButton("Browse File\u2026");
        AppTheme.styleSecondary(importFileBtn);
        JButton importPasteBtn = new JButton("Paste CSV\u2026");
        AppTheme.styleSecondary(importPasteBtn);
        JLabel  importStatus   = new JLabel(" ");
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

        final String STUDENT_FORMAT = """
                Name [, Group [, ElectiveSubjects (semicolon-separated)]]
                  Group and elective subjects are optional
                  Group must match a name in the Groups tab (if provided)
                  Elective subjects must match names in the Subjects tab
                  Example : John Doe, Group-A, Art;Music
                  Example : Jane Doe""";

        importFileBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            CsvImporter.ImportResult r = CsvImporter.importStudents(fc.getSelectedFile());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, STUDENT_FORMAT);
            refreshTable();
        });
        importPasteBtn.addActionListener(ev -> {
            String hint = """
                    # Format: Name [, Group [, ElectiveSubjects (semicolon-separated)]]
                    #   Group and elective subjects are optional
                    #   Group must match a name in the Groups tab (if provided)
                    #   Elective subjects must match names in the Subjects tab
                    #   Example : John Doe, Group-A, Art;Music
                    #   Example : Jane Doe
                    # Lines starting with '#' are ignored. First data row = header (skipped).
                    Name, Group, ElectiveSubjects
                    """;
            JTextArea ta = new JTextArea(hint, 12, 55);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
                    "Paste Students CSV",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION || ta.getText().isBlank()) return;
            CsvImporter.ImportResult r = CsvImporter.importStudentsFromText(ta.getText());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, STUDENT_FORMAT);
            refreshTable();
        });

        exportBtn.addActionListener(ev -> {
            if (DataStore.getStudents().isEmpty()) { importStatus.setText("Nothing to export."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            fc.setDialogTitle("Export Students CSV");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".csv")) f = new java.io.File(f.getAbsolutePath() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("Name,Group,ElectiveSubjects");
                for (Student s : DataStore.getStudents()) {
                    String grp = s.getGroup() != null ? s.getGroup().getName() : "";
                    String electives = s.getElectiveSubjects().stream()
                        .map(sub -> sub.getName().replace(";", " "))
                        .distinct()
                        .collect(Collectors.joining(";"));
                    pw.printf("\"%s\",\"%s\",%s%n",
                        s.getName().replace("\"", "\"\""),
                        grp.replace("\"", "\"\""),
                        electives);
                }
                importStatus.setText("Exported " + DataStore.getStudents().size() + " students.");
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton autoAssignBtn = new JButton("Auto-Assign Groups");
        AppTheme.styleSecondary(autoAssignBtn);

        JPanel actionsCol = new JPanel(new GridLayout(4, 1, 0, 8));
        actionsCol.setBackground(AppTheme.BG);
        actionsCol.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        actionsCol.add(AppTheme.makeActionCard(
                "\u2795", "Add / Save",
                "Save the current form as a new student.",
                new Color(0x2563EB), addBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDDD1", "Delete Selected",
                "Remove the selected student(s) from the list.",
                new Color(0xDC2626), deleteBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83E\uDDF9", "New / Clear",
                "Reset the form to add a new entry.",
                new Color(0x64748B), clearBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDD04", "Auto-Assign Groups",
                "Assign students to groups based on elective subjects.",
                new Color(0x0891B2), autoAssignBtn));

        JPanel actionsWrap = new JPanel(new BorderLayout());
        actionsWrap.setBackground(AppTheme.BG);
        actionsWrap.add(actionsCol, BorderLayout.NORTH);

        JPanel northSection = new JPanel(new BorderLayout(8, 4));
        northSection.setBackground(AppTheme.BG);
        northSection.add(form,        BorderLayout.CENTER);
        northSection.add(actionsWrap, BorderLayout.EAST);
        northSection.add(importStrip, BorderLayout.SOUTH);

        // ── Table ─────────────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(
                new String[]{"Name", "Group", "Elective Subjects"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        JScrollPane scroll = AppTheme.modernScroll(table);

        // Right-click on "Group" column header to clear all group assignments
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int col = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (col != 1) return; // only "Group" column
                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem("Clear Group column");
                item.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(StudentPanel.this,
                            "Remove group assignments from all students?",
                            "Clear Groups", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                    for (Student stu : DataStore.getStudents()) stu.setGroup(null);
                    refreshTable();
                });
                popup.add(item);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { mousePressed(e); }
        });

        add(northSection, BorderLayout.NORTH);
        add(scroll,       BorderLayout.CENTER);

        // ── Row selection → populate form ────────────────────────────────
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            editingRow = row;
            Student stu = DataStore.getStudents().get(row);
            nameField.setText(stu.getName());
            if (stu.getGroup() != null) groupBox.setSelectedItem(stu.getGroup().getName());
            refreshDropdowns();
            electiveList.clearSelection();
            java.util.Set<String> electiveNames = stu.getElectiveSubjects().stream()
                    .map(Subject::getName).collect(java.util.stream.Collectors.toSet());
            for (int i = 0; i < electiveListModel.getSize(); i++) {
                if (electiveNames.contains(electiveListModel.get(i).getName()))
                    electiveList.addSelectionInterval(i, i);
            }
            addBtn.setText("Update Student");
        });

        // ── Actions ───────────────────────────────────────────────────────────
        clearBtn.addActionListener(e -> clearForm());
        refreshDataBtn.addActionListener(e -> refreshDropdowns());

        autoAssignBtn.addActionListener(e -> {
            List<StudentGroup> groups = DataStore.getGroups();
            if (groups.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No groups defined yet. Generate groups first.",
                        "No Groups", JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<Student> students = DataStore.getStudents();
            if (students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No students to assign.",
                        "No Students", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Build a map: group -> remaining capacity
            java.util.Map<StudentGroup, Integer> remaining = new java.util.LinkedHashMap<>();
            for (StudentGroup g : groups) remaining.put(g, g.getSize());

            int assigned = 0;
            for (Student stu : students) {
                java.util.Set<String> wanted = stu.getElectiveSubjects().stream()
                        .map(Subject::getName)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(
                                () -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                StudentGroup best = null;
                int bestScore = Integer.MIN_VALUE;

                for (StudentGroup g : groups) {
                    int capLeft = remaining.getOrDefault(g, 0);
                    if (capLeft <= 0) continue;

                    java.util.Set<String> offered = g.getSubjects().stream()
                            .filter(sub -> !sub.isMandatory())
                            .map(Subject::getName)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.toCollection(
                                    () -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

                    int overlap = 0;
                    for (String name : wanted) {
                        if (offered.contains(name)) overlap++;
                    }

                    int score;
                    if (offered.equals(wanted)) {
                        score = 1000 + Math.min(capLeft, 50);
                    } else if (wanted.isEmpty() && offered.isEmpty()) {
                        score = 500 + Math.min(capLeft, 50);
                    } else if (overlap > 0) {
                        score = overlap * 10 - Math.abs(offered.size() - wanted.size());
                    } else {
                        continue;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        best = g;
                    }
                }

                if (best == null) {
                    for (StudentGroup g : groups) {
                        if (remaining.getOrDefault(g, 0) > 0) {
                            best = g;
                            break;
                        }
                    }
                }

                if (best != null) {
                    if (!best.equals(stu.getGroup())) assigned++;
                    stu.setGroup(best);
                    remaining.merge(best, -1, Integer::sum);
                }
            }
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    assigned + " student" + (assigned == 1 ? "" : "s") + " assigned to groups.",
                    "Auto-Assign Complete", JOptionPane.INFORMATION_MESSAGE);
        });

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a student name.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String selectedGroupName = (String) groupBox.getSelectedItem();
            StudentGroup group = selectedGroupName == null ? null :
                    DataStore.getGroups().stream()
                            .filter(g -> g.getName().equals(selectedGroupName)).findFirst().orElse(null);
            String groupName = group != null ? group.getName() : "—";

            Student duplicate = findStudentByNameIgnoreCase(name);
            if (editingRow < 0 && duplicate != null) {
            String existingGroup = duplicate.getGroup() == null ? "No Group" : duplicate.getGroup().getName();
            JOptionPane.showMessageDialog(this,
                "Student '" + name + "' already exists in group '" + existingGroup + "'.\n"
                    + "One student can have only one group.",
                "Duplicate Student", JOptionPane.WARNING_MESSAGE);
            return;
            }

            if (editingRow >= 0) {
                Student existing = DataStore.getStudents().get(editingRow);
            if (!existing.getName().equalsIgnoreCase(name) && duplicate != null) {
                String existingGroup = duplicate.getGroup() == null ? "No Group" : duplicate.getGroup().getName();
                JOptionPane.showMessageDialog(this,
                    "Student '" + name + "' already exists in group '" + existingGroup + "'.\n"
                        + "One student can have only one group.",
                    "Duplicate Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
                existing.setName(name);
                existing.setGroup(group);
                existing.getElectiveSubjects().clear();
                for (Subject s : electiveList.getSelectedValuesList()) {
                    DataStore.getSubjects().stream()
                            .filter(sub -> !sub.isMandatory() && sub.getName().equals(s.getName()))
                            .forEach(existing::addElective);
                }
                tableModel.setValueAt(name, editingRow, 0);
                tableModel.setValueAt(groupName, editingRow, 1);
                tableModel.setValueAt(existing.getElectiveSubjects().stream()
                        .map(Subject::getName).distinct().collect(Collectors.joining(", ")), editingRow, 2);
            } else {
                Student student = new Student(name, group);
                for (Subject s : electiveList.getSelectedValuesList()) {
                    DataStore.getSubjects().stream()
                            .filter(sub -> !sub.isMandatory() && sub.getName().equals(s.getName()))
                            .forEach(student::addElective);
                }
                DataStore.addStudent(student);
                tableModel.addRow(new Object[]{
                    name, groupName,
                    student.getElectiveSubjects().stream().map(Subject::getName).distinct().collect(Collectors.joining(", "))
                });
            }
            clearForm();
        });

        deleteBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more students to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (int i = rows.length - 1; i >= 0; i--) {
                DataStore.removeStudent(DataStore.getStudents().get(rows[i]));
                tableModel.removeRow(rows[i]);
            }
            clearForm();
        });

        refreshDropdowns();
    }

    private Student findStudentByNameIgnoreCase(String name) {
        if (name == null || name.isBlank()) return null;
        for (Student s : DataStore.getStudents()) {
            if (name.equalsIgnoreCase(s.getName())) return s;
        }
        return null;
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
        electiveList.clearSelection();
        addBtn.setText("Add Student");
    }

    /** Populate group dropdown and elective list from current DataStore data. */
    private void refreshDropdowns() {
        String prevGroup = (String) groupBox.getSelectedItem();
        groupBox.removeAllItems();
        DataStore.getGroups().forEach(g -> groupBox.addItem(g.getName()));
        if (prevGroup != null) {
            for (int i = 0; i < groupBox.getItemCount(); i++) {
                if (groupBox.getItemAt(i).equals(prevGroup)) { groupBox.setSelectedIndex(i); break; }
            }
        }

        // Rebuild list — preserve currently-selected names
        java.util.Set<String> prevSelected = electiveList.getSelectedValuesList().stream()
                .map(Subject::getName).collect(java.util.stream.Collectors.toSet());
        electiveListModel.clear();
        java.util.LinkedHashMap<String, Subject> byName = new java.util.LinkedHashMap<>();
        DataStore.getSubjects().stream().filter(s -> !s.isMandatory())
                .forEach(s -> byName.putIfAbsent(s.getName(), s));
        byName.values().forEach(electiveListModel::addElement);
        for (int i = 0; i < electiveListModel.getSize(); i++) {
            if (prevSelected.contains(electiveListModel.get(i).getName()))
                electiveList.addSelectionInterval(i, i);
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Student s : DataStore.getStudents()) {
            tableModel.addRow(new Object[]{
                s.getName(),
                s.getGroup() != null ? s.getGroup().getName() : "—",
                s.getElectiveSubjects().stream().map(Subject::getName).distinct().collect(Collectors.joining(", "))
            });
        }
        refreshDropdowns();
    }

}
