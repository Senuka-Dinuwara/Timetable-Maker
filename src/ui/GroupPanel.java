package ui;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import model.StudentGroup;
import model.Subject;
import util.CsvImporter;
import util.DataStore;

public class GroupPanel extends JPanel {

    private JTextField nameField;
    private JTextField sizeField;
    private JButton addBtn;
    private int editingRow = -1;
    private DefaultTableModel tableModel;
    private JTable table;

    public GroupPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4), "Add / Edit Student Group",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        AppTheme.FONT_TITLE, AppTheme.TEXT2)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        nameField = new JTextField(15);
        sizeField = new JTextField(15);

        addBtn = new JButton("Add Group");
        AppTheme.stylePrimary(addBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        AppTheme.styleDanger(deleteBtn);
        JButton clearBtn = new JButton("New / Clear");
        AppTheme.styleSecondary(clearBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; form.add(new JLabel("Group Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; form.add(new JLabel("Capacity (students):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(sizeField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.gridwidth = 2;
        JButton generateGroupsBtn = new JButton("Generate Groups\u2026");
        AppTheme.styleSecondary(generateGroupsBtn);
        form.add(generateGroupsBtn, gbc);
        gbc.gridwidth = 1;


        // --- Table ---
        tableModel = new DefaultTableModel(new String[]{"Group Name", "Capacity"}, 0) {
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

        final String GROUP_FORMAT = """
                Name, Size, Subjects (semicolon-separated subject names)
                  Subjects must match names already added in the Subjects tab
                  Example : Group-A, 25, Math;Physics""";

        importFileBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            CsvImporter.ImportResult r = CsvImporter.importGroups(fc.getSelectedFile());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, GROUP_FORMAT);
            refreshTable();
        });
        importPasteBtn.addActionListener(ev -> {
            String hint = """
                    # Format: Name, Size, Subjects (semicolon-separated subject names)
                    #   Subjects must match names already added in the Subjects tab
                    #   Example : Group-A, 25, Math;Physics
                    # Lines starting with '#' are ignored. First data row = header (skipped).
                    Name, Size, Subjects
                    """;
            JTextArea ta = new JTextArea(hint, 12, 55);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
                    "Paste Groups CSV",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION || ta.getText().isBlank()) return;
            CsvImporter.ImportResult r = CsvImporter.importGroupsFromText(ta.getText());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, GROUP_FORMAT);
            refreshTable();
        });

        exportBtn.addActionListener(ev -> {
            if (DataStore.getGroups().isEmpty()) { importStatus.setText("Nothing to export."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            fc.setDialogTitle("Export Groups CSV");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".csv")) f = new java.io.File(f.getAbsolutePath() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("Name,Size,Subjects");
                for (StudentGroup g : DataStore.getGroups()) {
                    String subjs = g.getSubjects().stream()
                        .map(s -> s.getName().replace(";", " "))
                        .collect(java.util.stream.Collectors.joining(";"));
                    pw.printf("\"%s\",%d,%s%n", g.getName().replace("\"", "\"\""), g.getSize(), subjs);
                }
                importStatus.setText("Exported " + DataStore.getGroups().size() + " groups.");
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
            StudentGroup g = DataStore.getGroups().get(row);
            nameField.setText(g.getName());
            sizeField.setText(String.valueOf(g.getSize()));
            addBtn.setText("Update Group");
        });

        JPanel actionsCol = new JPanel(new GridLayout(3, 1, 0, 8));
        actionsCol.setBackground(AppTheme.BG);
        actionsCol.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        actionsCol.add(AppTheme.makeActionCard(
                "\u2795", "Add / Save",
                "Save the current form as a new group.",
                new Color(0x2563EB), addBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDDD1", "Delete Selected",
                "Remove the selected group(s) from the list.",
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
        add(scroll,       BorderLayout.CENTER);

        // --- Actions ---
        clearBtn.addActionListener(e -> clearForm());
        generateGroupsBtn.addActionListener(e -> showGenerateDialog());

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String sizeText = sizeField.getText().trim();
            if (name.isEmpty() || sizeText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int size;
            try {
                size = Integer.parseInt(sizeText);
                if (size <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Size must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (editingRow >= 0) {
                StudentGroup existing = DataStore.getGroups().get(editingRow);
                existing.setName(name);
                existing.setSize(size);
                tableModel.setValueAt(name, editingRow, 0);
                tableModel.setValueAt(size, editingRow, 1);
            } else {
                StudentGroup group = new StudentGroup(name, size);
                addMandatorySubjects(group);
                DataStore.addGroup(group);
                tableModel.addRow(new Object[]{name, size});
            }
            clearForm();
        });

        deleteBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more groups to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (int i = rows.length - 1; i >= 0; i--) {
                DataStore.removeGroup(DataStore.getGroups().get(rows[i]));
                tableModel.removeRow(rows[i]);
            }
            clearForm();
        });
    }

    /** Adds all currently-defined mandatory subjects to a group (if not already present). */
    private static void addMandatorySubjects(StudentGroup group) {
        for (Subject s : DataStore.getSubjects()) {
            if (s.isMandatory() && !group.getSubjects().contains(s)) {
                group.addSubject(s);
            }
        }
    }

    /** Adds all Subject entries (all types) with the same name as {@code s} to the group. */
    private static void addAllTypesOf(Subject s, StudentGroup group) {
        String subjectName = s.getName();
        for (Subject sub : DataStore.getSubjects()) {
            if (sub.getName().equalsIgnoreCase(subjectName) && !group.getSubjects().contains(sub)) {
                group.addSubject(sub);
            }
        }
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
        sizeField.setText("");
        addBtn.setText("Add Group");
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (StudentGroup g : DataStore.getGroups()) {
            tableModel.addRow(new Object[]{g.getName(), g.getSize()});
        }
    }

    private void showGenerateDialog() {
        // ── Shared: prefix + suffix ───────────────────────────────────────
        JTextField prefixField = new JTextField("", 10);
        ButtonGroup suffixBG = new ButtonGroup();
        JRadioButton numbersRadio = new JRadioButton("Numbers (1, 2, 3\u2026)", true);
        JRadioButton lettersRadio = new JRadioButton("Letters (A, B, C\u2026)");
        suffixBG.add(numbersRadio); suffixBG.add(lettersRadio);
        numbersRadio.setOpaque(false); lettersRadio.setOpaque(false);
        JLabel suffixNote = new JLabel();
        suffixNote.setFont(AppTheme.FONT_SMALL);
        suffixNote.setForeground(AppTheme.TEXT2);

        // ── Method radio buttons ──────────────────────────────────────────
        ButtonGroup methodBG = new ButtonGroup();
        JRadioButton electiveRadio = new JRadioButton("By elective selection", true);
        JRadioButton capacityRadio = new JRadioButton("By capacity");
        JRadioButton countRadio    = new JRadioButton("By count");
        methodBG.add(electiveRadio); methodBG.add(capacityRadio); methodBG.add(countRadio);
        electiveRadio.setOpaque(false); capacityRadio.setOpaque(false); countRadio.setOpaque(false);

        // ── Pre-computed data ─────────────────────────────────────────────
        // Deduplicate electives by name — one representative Subject per unique name
        java.util.LinkedHashMap<String, Subject> electiveMap = new java.util.LinkedHashMap<>();
        for (Subject s : DataStore.getSubjects()) {
            if (!s.isMandatory()) electiveMap.putIfAbsent(s.getName(), s);
        }
        List<Subject> electives = new java.util.ArrayList<>(electiveMap.values());
        int totalStudents = DataStore.getStudents().size();
        long noElectiveCount = DataStore.getStudents().stream()
                .filter(st -> st.getElectiveSubjects().isEmpty()).count();
        // ── Panel A: By elective selection ───────────────────────────────
        int derivedElectiveCap = electives.isEmpty() ? 30
                : Math.max(1, (int) Math.ceil((double) totalStudents / electives.size()));
        JTextField electiveCapField = new JTextField(String.valueOf(derivedElectiveCap), 6);
        JLabel electiveCapHint = new JLabel(!electives.isEmpty() && totalStudents > 0
                ? "(auto: " + totalStudents + " \u00f7 " + electives.size() + " subjects)" : "");
        electiveCapHint.setFont(AppTheme.FONT_SMALL);
        electiveCapHint.setForeground(AppTheme.TEXT2);
        JCheckBox defaultGroupCheck = new JCheckBox(
                "Add group for students with no elective", noElectiveCount > 0);
        defaultGroupCheck.setOpaque(false);
        JCheckBox includeNameCheck = new JCheckBox("Include subject name in group name", true);
        includeNameCheck.setOpaque(false);
        JLabel electiveHint = new JLabel();
        electiveHint.setFont(AppTheme.FONT_SMALL);
        electiveHint.setForeground(AppTheme.TEXT2);
        Runnable updateElectiveHint = () -> {
            if (electives.isEmpty()) {
                electiveHint.setText("No elective subjects found \u2014 add them in the Subjects tab first.");
                return;
            }
            String p = prefixField.getText().trim();
            boolean ul = lettersRadio.isSelected();
            String pfx = p.isEmpty() ? "Group" : p;
            String sample = pfx + "-" + (ul ? "A" : "1");
            String unassigned = noElectiveCount > 0
                    ? " " + noElectiveCount + " student" + (noElectiveCount == 1 ? " has" : "s have")
                      + " no elective and can be placed in general group(s)." : "";
            electiveHint.setText("<html>Students are grouped by their full elective selection, "
                    + "so learners who pick multiple electives stay together in the same generated timetable group. "
                    + "Example group name: " + sample + "." + unassigned + "</html>");
        };
        updateElectiveHint.run();
        includeNameCheck.addActionListener(ev -> updateElectiveHint.run());
        JPanel electivePanel = new JPanel(new GridBagLayout());
        electivePanel.setOpaque(false);
        GridBagConstraints ga = new GridBagConstraints();
        ga.insets = new Insets(3, 4, 3, 4); ga.fill = GridBagConstraints.HORIZONTAL;
        ga.gridx = 0; ga.gridy = 0; ga.weightx = 0;          electivePanel.add(new JLabel("Capacity per group:"), ga);
        ga.gridx = 1; ga.weightx = 0.4;                       electivePanel.add(electiveCapField, ga);
        ga.gridx = 2; ga.weightx = 0.6;                       electivePanel.add(electiveCapHint, ga);
        ga.gridx = 0; ga.gridy = 1; ga.gridwidth = 3;         electivePanel.add(includeNameCheck, ga);
        ga.gridwidth = 1;
        ga.gridx = 0; ga.gridy = 2; ga.gridwidth = 3;         electivePanel.add(defaultGroupCheck, ga);
        ga.gridwidth = 1;
        ga.gridx = 0; ga.gridy = 3; ga.gridwidth = 3;         electivePanel.add(electiveHint, ga);
        ga.gridwidth = 1;

        // ── Panel B: By capacity ───────────────────────────────────────────
        JTextField totalField = new JTextField(
                String.valueOf(totalStudents > 0 ? totalStudents : 100), 6);
        JTextField capPerGroupField = new JTextField("30", 6);
        JLabel capHintLabel = new JLabel(" ");
        capHintLabel.setFont(AppTheme.FONT_SMALL);
        capHintLabel.setForeground(AppTheme.TEXT2);
        JLabel totalHint = new JLabel(
                totalStudents > 0 ? "(" + totalStudents + " in list)" : "(no students yet)");
        totalHint.setFont(AppTheme.FONT_SMALL);
        totalHint.setForeground(AppTheme.TEXT2);
        JPanel capacityPanel = new JPanel(new GridBagLayout());
        capacityPanel.setOpaque(false);
        GridBagConstraints gb = new GridBagConstraints();
        gb.insets = new Insets(3, 4, 3, 4); gb.fill = GridBagConstraints.HORIZONTAL;
        gb.gridx = 0; gb.gridy = 0; gb.weightx = 0;          capacityPanel.add(new JLabel("Total students:"), gb);
        gb.gridx = 1; gb.weightx = 0.4;                       capacityPanel.add(totalField, gb);
        gb.gridx = 2; gb.weightx = 0.6;                       capacityPanel.add(totalHint, gb);
        gb.gridx = 0; gb.gridy = 1; gb.weightx = 0;           capacityPanel.add(new JLabel("Max per group:"), gb);
        gb.gridx = 1; gb.weightx = 1; gb.gridwidth = 2;       capacityPanel.add(capPerGroupField, gb);
        gb.gridwidth = 1;
        gb.gridx = 0; gb.gridy = 2; gb.gridwidth = 3;         capacityPanel.add(capHintLabel, gb);
        gb.gridwidth = 1;

        // ── Panel C: By count ─────────────────────────────────────────────
        JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 200, 1));
        JTextField capEachField = new JTextField("30", 6);
        JLabel countHintLabel = new JLabel(" ");
        countHintLabel.setFont(AppTheme.FONT_SMALL);
        countHintLabel.setForeground(AppTheme.TEXT2);
        JPanel countPanel = new JPanel(new GridBagLayout());
        countPanel.setOpaque(false);
        GridBagConstraints gcount = new GridBagConstraints();
        gcount.insets = new Insets(3, 4, 3, 4); gcount.fill = GridBagConstraints.HORIZONTAL;
        gcount.gridx = 0; gcount.gridy = 0; gcount.weightx = 0;   countPanel.add(new JLabel("Number of groups:"), gcount);
        gcount.gridx = 1; gcount.weightx = 1;                      countPanel.add(countSpinner, gcount);
        gcount.gridx = 0; gcount.gridy = 1; gcount.weightx = 0;   countPanel.add(new JLabel("Capacity each:"), gcount);
        gcount.gridx = 1; gcount.weightx = 1;                      countPanel.add(capEachField, gcount);
        gcount.gridx = 0; gcount.gridy = 2; gcount.gridwidth = 2; countPanel.add(countHintLabel, gcount);
        gcount.gridwidth = 1;

        // ── Hint Runnables ────────────────────────────────────────────────
        Runnable updateCapHint = () -> {
            try {
                int tot = Integer.parseInt(totalField.getText().trim());
                int cap = Integer.parseInt(capPerGroupField.getText().trim());
                if (tot > 0 && cap > 0) {
                    boolean ul = lettersRadio.isSelected();
                    int n = (int) Math.ceil((double) tot / cap);
                    String p = prefixField.getText().isEmpty() ? "Group" : prefixField.getText();
                    capHintLabel.setText("\u2192 " + n + " group" + (n == 1 ? "" : "s") + "  ("
                            + p + "-" + (ul ? "A" : "1") + " \u2026 "
                            + p + "-" + (ul ? toLetterSuffix(n) : String.valueOf(n)) + ")");
                    return;
                }
            } catch (NumberFormatException ignored) {}
            capHintLabel.setText(" ");
        };
        Runnable updateCountHint = () -> {
            int n  = (int) countSpinner.getValue();
            boolean ul = lettersRadio.isSelected();
            String p = prefixField.getText().isEmpty() ? "Group" : prefixField.getText();
            countHintLabel.setText("\u2192 " + p + "-" + (ul ? "A" : "1") + ", "
                    + p + "-" + (ul ? "B" : "2")
                    + (n > 2 ? ", " + p + "-" + (ul ? "C" : "3") + "\u2026" : ""));
        };
        updateCapHint.run();
        updateCountHint.run();

        // ── Listeners ─────────────────────────────────────────────────────
        javax.swing.event.DocumentListener sharedDL = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); updateCountHint.run(); updateElectiveHint.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); updateCountHint.run(); updateElectiveHint.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); updateCountHint.run(); updateElectiveHint.run(); }
        };
        javax.swing.event.DocumentListener capDL = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCapHint.run(); }
        };
        prefixField.getDocument().addDocumentListener(sharedDL);
        totalField.getDocument().addDocumentListener(capDL);
        capPerGroupField.getDocument().addDocumentListener(capDL);
        numbersRadio.addActionListener(ev -> { updateCapHint.run(); updateCountHint.run(); updateElectiveHint.run(); });
        lettersRadio.addActionListener(ev -> { updateCapHint.run(); updateCountHint.run(); updateElectiveHint.run(); });
        countSpinner.addChangeListener(ev -> updateCountHint.run());

        // ── CardLayout for method-specific fields ─────────────────────────
        JPanel methodCards = new JPanel(new CardLayout());
        methodCards.setOpaque(false);
        methodCards.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        methodCards.add(electivePanel, "elective");
        methodCards.add(capacityPanel, "capacity");
        methodCards.add(countPanel,    "count");

        Runnable applySuffixState = () -> {
            numbersRadio.setEnabled(true);
            lettersRadio.setEnabled(true);
        };
        applySuffixState.run();
        electiveRadio.addActionListener(ev -> {
            ((CardLayout) methodCards.getLayout()).show(methodCards, "elective");
            applySuffixState.run();
        });
        capacityRadio.addActionListener(ev -> {
            ((CardLayout) methodCards.getLayout()).show(methodCards, "capacity");
            applySuffixState.run(); updateCapHint.run();
        });
        countRadio.addActionListener(ev -> {
            ((CardLayout) methodCards.getLayout()).show(methodCards, "count");
            applySuffixState.run(); updateCountHint.run();
        });

        // ── Dialog layout ─────────────────────────────────────────────────
        JPanel suffixRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        suffixRow.setOpaque(false);
        suffixRow.add(numbersRadio); suffixRow.add(lettersRadio); suffixRow.add(suffixNote);

        JPanel namingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        namingRow.setOpaque(false);
        namingRow.add(new JLabel("Prefix:")); namingRow.add(prefixField);
        namingRow.add(Box.createRigidArea(new Dimension(10, 0)));
        namingRow.add(new JLabel("Suffix:")); namingRow.add(suffixRow);

        JPanel methodRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2));
        methodRow.setOpaque(false);
        methodRow.add(new JLabel("Method:"));
        methodRow.add(electiveRadio); methodRow.add(capacityRadio); methodRow.add(countRadio);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel dlgPanel = new JPanel();
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        namingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        dlgPanel.add(namingRow);
        dlgPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        dlgPanel.add(sep);
        dlgPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        dlgPanel.add(methodRow);
        dlgPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        dlgPanel.add(methodCards);
        dlgPanel.setPreferredSize(new Dimension(580, 250));

        int ok = JOptionPane.showConfirmDialog(this, dlgPanel,
                "Generate Groups", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String prefix = prefixField.getText().trim();
        boolean useLetters = lettersRadio.isSelected();
        try {
            if (electiveRadio.isSelected()) {
                if (electives.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No elective/basket subjects defined. Add elective subjects first.",
                            "No Elective Subjects", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int cap = Integer.parseInt(electiveCapField.getText().trim());
                if (cap <= 0) throw new NumberFormatException();
                boolean incName = includeNameCheck.isSelected();
                boolean useElectiveLetters = useLetters;
                String electivePrefix = prefix.isEmpty() ? "Group" : prefix;
                int globalIdx = DataStore.getGroups().size();

                java.util.Map<String, java.util.List<model.Student>> studentsBySelection = new java.util.LinkedHashMap<>();
                java.util.Map<String, java.util.List<Subject>> subjectsBySelection = new java.util.LinkedHashMap<>();
                java.util.List<model.Student> noElectiveStudents = new java.util.ArrayList<>();

                for (model.Student st : DataStore.getStudents()) {
                    java.util.List<String> pickedNames = st.getElectiveSubjects().stream()
                            .map(Subject::getName)
                            .filter(name -> name != null && !name.isBlank())
                            .distinct()
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                    if (pickedNames.isEmpty()) {
                        noElectiveStudents.add(st);
                        continue;
                    }

                    String selectionKey = String.join(" | ", pickedNames);
                    studentsBySelection.computeIfAbsent(selectionKey, k -> new java.util.ArrayList<>()).add(st);
                    subjectsBySelection.putIfAbsent(selectionKey, pickedNames.stream()
                            .map(electiveMap::get)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList()));
                }

                for (java.util.Map.Entry<String, java.util.List<model.Student>> entry : studentsBySelection.entrySet()) {
                    java.util.List<model.Student> enrolledStudents = entry.getValue();
                    java.util.List<Subject> selectedSubjects = subjectsBySelection.getOrDefault(
                            entry.getKey(), java.util.Collections.emptyList());
                    int enrolled = enrolledStudents.size();
                    if (enrolled <= 0 || selectedSubjects.isEmpty()) continue;

                    int numGroups = (int) Math.ceil((double) enrolled / cap);
                    String comboName = selectedSubjects.stream()
                            .map(Subject::getName)
                            .distinct()
                            .collect(Collectors.joining("+"));
                    boolean useSubjectName = incName && !comboName.isBlank();
                    String baseName = useSubjectName
                            ? (prefix.isEmpty() ? comboName : prefix + comboName)
                            : electivePrefix;

                    int remaining = enrolled;
                    int comboIdx = 0;
                    java.util.List<StudentGroup> created = new java.util.ArrayList<>();
                    for (int i = 1; i <= numGroups; i++) {
                        if (useSubjectName) comboIdx++; else globalIdx++;
                        int sfxNum = useSubjectName ? comboIdx : globalIdx;
                        int thisCap = Math.min(cap, remaining);
                        remaining -= thisCap;

                        String sfx = useElectiveLetters ? toLetterSuffix(sfxNum) : String.valueOf(sfxNum);
                        String groupName = baseName + "-" + sfx;
                        StudentGroup sg = new StudentGroup(groupName, thisCap);
                        for (Subject selected : selectedSubjects) addAllTypesOf(selected, sg);
                        addMandatorySubjects(sg);
                        DataStore.addGroup(sg);
                        tableModel.addRow(new Object[]{sg.getName(), sg.getSize()});
                        created.add(sg);
                    }

                    int idx = 0;
                    for (model.Student st : enrolledStudents) {
                        if (created.isEmpty()) break;
                        StudentGroup target = created.get(Math.min(idx / cap, created.size() - 1));
                        st.setGroup(target);
                        idx++;
                    }
                }

                if (defaultGroupCheck.isSelected() && !noElectiveStudents.isEmpty()) {
                    String defBase = prefix.isEmpty() ? "Group" : prefix;
                    int defGroups = (int) Math.ceil((double) noElectiveStudents.size() / cap);
                    int defRemaining = noElectiveStudents.size();
                    java.util.List<StudentGroup> defaultCreated = new java.util.ArrayList<>();
                    for (int i = 1; i <= defGroups; i++) {
                        globalIdx++;
                        int thisCap = Math.min(cap, defRemaining);
                        defRemaining -= thisCap;
                        String groupName = defBase + "-" + (useElectiveLetters ? toLetterSuffix(globalIdx) : String.valueOf(globalIdx));
                        StudentGroup defSG = new StudentGroup(groupName, thisCap);
                        addMandatorySubjects(defSG);
                        DataStore.addGroup(defSG);
                        tableModel.addRow(new Object[]{defSG.getName(), defSG.getSize()});
                        defaultCreated.add(defSG);
                    }
                    int idx = 0;
                    for (model.Student st : noElectiveStudents) {
                        if (defaultCreated.isEmpty()) break;
                        StudentGroup target = defaultCreated.get(Math.min(idx / cap, defaultCreated.size() - 1));
                        st.setGroup(target);
                        idx++;
                    }
                }
            } else if (capacityRadio.isSelected()) {
                int total = Integer.parseInt(totalField.getText().trim());
                int cap   = Integer.parseInt(capPerGroupField.getText().trim());
                if (total <= 0 || cap <= 0) throw new NumberFormatException();
                String capPrefix = prefix.isEmpty() ? "Group" : prefix;
                int numGroups = (int) Math.ceil((double) total / cap);
                int remaining = total;
                for (int i = 1; i <= numGroups; i++) {
                    int thisCap = Math.min(cap, remaining);
                    remaining -= thisCap;
                    StudentGroup sg = new StudentGroup(
                            capPrefix + "-" + (useLetters ? toLetterSuffix(i) : String.valueOf(i)), thisCap);
                    addMandatorySubjects(sg);
                    DataStore.addGroup(sg);
                    tableModel.addRow(new Object[]{sg.getName(), sg.getSize()});
                }
            } else {
                int count = (int) countSpinner.getValue();
                int cap   = Integer.parseInt(capEachField.getText().trim());
                if (cap <= 0) throw new NumberFormatException();
                String countPrefix = prefix.isEmpty() ? "Group" : prefix;
                for (int i = 1; i <= count; i++) {
                    StudentGroup sg = new StudentGroup(
                            countPrefix + "-" + (useLetters ? toLetterSuffix(i) : String.valueOf(i)), cap);
                    addMandatorySubjects(sg);
                    DataStore.addGroup(sg);
                    tableModel.addRow(new Object[]{sg.getName(), sg.getSize()});
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid positive numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Converts 1→A, 2→B, … 26→Z, 27→AA, 28→AB, … */
    private String toLetterSuffix(int i) {
        StringBuilder sb = new StringBuilder();
        while (i > 0) {
            i--;
            sb.insert(0, (char) ('A' + (i % 26)));
            i /= 26;
        }
        return sb.toString();
    }
}
