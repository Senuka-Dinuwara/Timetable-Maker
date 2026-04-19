package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import model.Subject;
import util.CsvImporter;
import util.DataStore;

public class SubjectPanel extends JPanel {

    private JTextField nameField;
    private JTextField hoursField;
    private JList<String> typeList;
    private DefaultListModel<String> typeListModel;
    private JComboBox<String> dayBox;
    private JSpinner durationSpinner;
    private JCheckBox mandatoryCheck;
    private JButton addBtn;
    private String editingName = null;
    private DefaultTableModel tableModel;
    private JTable table;

    private static final String[] DAYS  = {"None", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public SubjectPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4), "Add / Edit Subject",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        AppTheme.FONT_TITLE, AppTheme.TEXT2)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        nameField = new JTextField(15);
        hoursField = new JTextField(15);
        typeListModel = new DefaultListModel<>();
        typeList = new JList<>(typeListModel);
        // Toggle selection on single click (no Ctrl required) using selection-model override.
        typeList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (index0 < 0) return;
                if (isSelectedIndex(index0)) {
                    removeSelectionInterval(index0, index1);
                } else {
                    addSelectionInterval(index0, index1);
                }
            }
        });
        typeList.setVisibleRowCount(5);
        typeList.setFixedCellHeight(22);
        typeList.setToolTipText("Click items to select multiple types");
        typeList.setBackground(AppTheme.SURFACE);
        typeList.setSelectionBackground(AppTheme.SEL_BG);
        typeList.setSelectionForeground(AppTheme.SEL_FG);

        JScrollPane typeScroll = new JScrollPane(typeList);
        typeScroll.setPreferredSize(new Dimension(250, 106));
        typeScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        JButton refreshTypeBtn = new JButton("Refresh Types");
        AppTheme.styleSecondary(refreshTypeBtn);
        JButton selectAllTypeBtn = new JButton("Select All");
        AppTheme.styleSecondary(selectAllTypeBtn);
        JButton clearTypeBtn = new JButton("Clear");
        AppTheme.styleSecondary(clearTypeBtn);
        JLabel typeCountLbl = new JLabel("Selected: 0");
        typeCountLbl.setFont(AppTheme.FONT_SMALL);
        typeCountLbl.setForeground(AppTheme.TEXT2);

        JPanel typeActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        typeActionRow.setOpaque(false);
        typeActionRow.add(typeCountLbl);
        typeActionRow.add(Box.createHorizontalStrut(8));
        typeActionRow.add(selectAllTypeBtn);
        typeActionRow.add(clearTypeBtn);
        typeActionRow.add(refreshTypeBtn);

        JPanel typePanel = new JPanel(new BorderLayout(0, 4));
        typePanel.setOpaque(false);
        typePanel.add(typeScroll, BorderLayout.CENTER);
        typePanel.add(typeActionRow, BorderLayout.SOUTH);
        dayBox = new JComboBox<>(DAYS);
        durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
        ((JSpinner.DefaultEditor) durationSpinner.getEditor()).getTextField().setColumns(4);
        mandatoryCheck = new JCheckBox("Mandatory (all students attend)");
        mandatoryCheck.setSelected(true);
        addBtn = new JButton("Add Subject");
        AppTheme.stylePrimary(addBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        AppTheme.styleDanger(deleteBtn);
        JButton clearBtn  = new JButton("New / Clear");
        AppTheme.styleSecondary(clearBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; form.add(new JLabel("Subject Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; form.add(new JLabel("Weekly Hours:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(hoursField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; form.add(new JLabel("Assign Types:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(typePanel, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0; form.add(new JLabel("Session Duration (h):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(durationSpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; form.add(new JLabel("Preferred Day:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(dayBox, gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1.0; gbc.gridwidth = 2; form.add(mandatoryCheck, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0.0;

        Runnable updateTypeCount = () -> typeCountLbl.setText("Selected: " + typeList.getSelectedIndices().length);
        typeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateTypeCount.run();
        });

        refreshTypeBtn.addActionListener(e -> {
            refreshTypeList();
            updateTypeCount.run();
        });
        selectAllTypeBtn.addActionListener(e -> {
            if (!typeListModel.isEmpty()) typeList.setSelectionInterval(0, typeListModel.size() - 1);
            updateTypeCount.run();
        });
        clearTypeBtn.addActionListener(e -> {
            typeList.clearSelection();
            updateTypeCount.run();
        });
        refreshTypeList();
        updateTypeCount.run();


        // --- Table ---
        tableModel = new DefaultTableModel(
                new String[]{"Name", "Weekly Hours", "Session (h)", "Type", "Preferred Day", "Mandatory"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);
        JScrollPane scroll = AppTheme.modernScroll(table);

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

        final String SUBJECT_FORMAT = """
            Name, WeeklyHours, Type [, PreferredDay [, SessionDuration [, Mandatory]]]
              Type         : standard or custom values (semicolon-separated for multiple, e.g. Theory;Seminar)
                  PreferredDay : Monday | Tuesday | Wednesday | Thursday | Friday | Saturday  (or blank)
                  Mandatory    : true | false  (default: true)
                  Example      : Math, 4, Theory, Monday, 2, true
              Example      : Chemistry, 3, Theory;Lab, , 1, true
              Example      : UX Design, 2, Seminar;Workshop, Friday, 1, false""";

        importFileBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            CsvImporter.ImportResult r = CsvImporter.importSubjects(fc.getSelectedFile());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, SUBJECT_FORMAT);
            refreshTable();
        });
        importPasteBtn.addActionListener(ev -> {
                String hint = """
                    # Format: Name, WeeklyHours, Type [, PreferredDay [, SessionDuration [, Mandatory]]]
                    #   Type         : standard or custom (or semicolon-separated, e.g. Theory;Seminar)
                    #   PreferredDay : Monday | Tuesday | Wednesday | Thursday | Friday | Saturday  (or blank)
                    #   Mandatory    : true | false  (default: true)
                    # Lines starting with '#' are ignored. First data row = header (skipped).
                    Name, WeeklyHours, Type
                    """;
            JTextArea ta = new JTextArea(hint, 12, 55);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
                    "Paste Subjects CSV",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION || ta.getText().isBlank()) return;
            CsvImporter.ImportResult r = CsvImporter.importSubjectsFromText(ta.getText());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, SUBJECT_FORMAT);
            refreshTable();
        });

        exportBtn.addActionListener(ev -> {
            if (DataStore.getSubjects().isEmpty()) { importStatus.setText("Nothing to export."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            fc.setDialogTitle("Export Subjects CSV");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".csv")) f = new java.io.File(f.getAbsolutePath() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("Name,WeeklyHours,Type,PreferredDay,SessionDuration,Mandatory");
                for (Subject s : DataStore.getSubjects()) {
                    pw.printf("\"%s\",%d,%s,%s,%d,%b%n",
                        s.getName().replace("\"", "\"\""),
                        s.getWeeklyHours(), s.getType(),
                        s.getPreferredDay() == null ? "" : s.getPreferredDay(),
                        s.getSessionDuration(), s.isMandatory());
                }
                importStatus.setText("Exported " + DataStore.getSubjects().size() + " subjects.");
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Row selection → populate form ────────────────────────────────
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            String selName = (String) tableModel.getValueAt(row, 0);
            editingName = selName;
            List<Subject> matching = DataStore.getSubjects().stream()
                    .filter(s -> s.getName().equals(selName))
                    .collect(java.util.stream.Collectors.toList());
            if (matching.isEmpty()) return;
            Subject first = matching.get(0);
            nameField.setText(first.getName());
            hoursField.setText(String.valueOf(first.getWeeklyHours()));
            String joinedTypes = matching.stream().map(Subject::getType)
                    .collect(java.util.stream.Collectors.joining(";"));
            setTypeChecks(joinedTypes);
            durationSpinner.setValue(first.getSessionDuration());
            String pd = first.getPreferredDay();
            dayBox.setSelectedItem(pd == null ? "None" : pd);
            mandatoryCheck.setSelected(first.isMandatory());
            addBtn.setText("Update Subject");
        });

        JPanel actionsCol = new JPanel(new GridLayout(3, 1, 0, 8));
        actionsCol.setBackground(AppTheme.BG);
        actionsCol.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        actionsCol.add(AppTheme.makeActionCard(
                "\u2795", "Add / Save",
                "Save the current form as a new subject.",
                new Color(0x2563EB), addBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDDD1", "Delete Selected",
                "Remove the selected subject(s) from the list.",
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

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String hoursText = hoursField.getText().trim();
            if (name.isEmpty() || hoursText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int hours;
            try {
                hours = Integer.parseInt(hoursText);
                if (hours <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Weekly hours must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String type = getSelectedTypes();
            if (type.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one type.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int sessionDur = (int) durationSpinner.getValue();
            String selectedDay = (String) dayBox.getSelectedItem();
            String preferredDay = "None".equals(selectedDay) ? null : selectedDay;
            boolean mandatory = mandatoryCheck.isSelected();
            String[] typeParts = type.split(";");
            if (editingName != null) {
                // Remove all existing entries for this name, then re-add with updated types
                List<Subject> toRemove = DataStore.getSubjects().stream()
                        .filter(s -> s.getName().equals(editingName))
                        .collect(java.util.stream.Collectors.toList());
                toRemove.forEach(DataStore::removeSubject);
            }
            for (String rawType : typeParts) {
                String st = rawType.trim();
                Subject subject = new Subject(name, hours, st, preferredDay, false, sessionDur, mandatory);
                DataStore.addSubject(subject);
                if (mandatory) addToAllGroups(subject);
            }
            refreshTable();
            clearForm();
        });

        deleteBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more subjects to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            java.util.Set<String> namesToRemove = new java.util.LinkedHashSet<>();
            for (int r : rows) namesToRemove.add((String) tableModel.getValueAt(r, 0));
            for (String n : namesToRemove) {
                List<Subject> toRemove = DataStore.getSubjects().stream()
                        .filter(s -> s.getName().equals(n))
                        .collect(java.util.stream.Collectors.toList());
                toRemove.forEach(DataStore::removeSubject);
            }
            refreshTable();
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
        sp.setPreferredSize(new Dimension(560, 300));
        JOptionPane.showMessageDialog(this, sp, "Import Warnings / Errors", JOptionPane.WARNING_MESSAGE);
    }

    /** Adds the given mandatory subject to every group that does not already have it. */
    private static void addToAllGroups(Subject subject) {
        for (model.StudentGroup g : DataStore.getGroups()) {
            if (!g.getSubjects().contains(subject)) {
                g.addSubject(subject);
            }
        }
    }

    private void clearForm() {
        editingName = null;
        table.clearSelection();
        nameField.setText("");
        hoursField.setText("");
        typeList.clearSelection();
        durationSpinner.setValue(1);
        dayBox.setSelectedIndex(0);
        mandatoryCheck.setSelected(true);
        addBtn.setText("Add Subject");
    }

    private String getSelectedTypes() {
        return String.join(";", typeList.getSelectedValuesList());
    }

    private void setTypeChecks(String types) {
        typeList.clearSelection();
        if (types != null && !types.isBlank()) {
            for (String t : types.split(";")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty() && !containsType(trimmed)) {
                    typeListModel.addElement(trimmed);
                }
            }
        }
        if (types == null || types.isBlank()) return;
        for (String t : types.split(";")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) selectType(trimmed);
        }
    }

    private void refreshTypeList() {
        java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>();
        types.addAll(DataStore.getSubjectTypeOptions());
        for (Subject s : DataStore.getSubjects()) {
            String st = s.getType();
            if (st == null || st.isBlank()) continue;
            for (String t : st.split(";")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) types.add(trimmed);
            }
        }
        java.util.List<String> currentlySelected = typeList.getSelectedValuesList();
        typeListModel.clear();
        types.forEach(typeListModel::addElement);
        currentlySelected.forEach(this::selectType);
    }

    public void refreshTypeOptions() {
        refreshTypeList();
    }

    private boolean containsType(String type) {
        for (int i = 0; i < typeListModel.size(); i++) {
            if (typeListModel.get(i).equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    private void selectType(String type) {
        for (int i = 0; i < typeListModel.size(); i++) {
            if (typeListModel.get(i).equalsIgnoreCase(type)) {
                typeList.addSelectionInterval(i, i);
            }
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        java.util.LinkedHashMap<String, java.util.List<Subject>> byName = new java.util.LinkedHashMap<>();
        for (Subject s : DataStore.getSubjects()) {
            byName.computeIfAbsent(s.getName(), k -> new java.util.ArrayList<>()).add(s);
        }
        for (java.util.Map.Entry<String, java.util.List<Subject>> entry : byName.entrySet()) {
            Subject first = entry.getValue().get(0);
            String types = entry.getValue().stream()
                    .map(Subject::getType)
                    .collect(java.util.stream.Collectors.joining(", "));
            tableModel.addRow(new Object[]{
                entry.getKey(), first.getWeeklyHours(), first.getSessionDuration(), types,
                first.getPreferredDay() == null ? "-" : first.getPreferredDay(),
                first.isMandatory() ? "Yes" : "No"
            });
        }
    }
}
