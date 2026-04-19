package ui;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.*;
import model.ScheduleEntry;
import model.StudentGroup;
import model.Subject;
import service.Scheduler;
import util.DataStore;

public class GeneratePanel extends JPanel {

    private final JCheckBox monBox, tueBox, wedBox, thuBox, friBox, satBox;
    private final JCheckBox limitDaysCheck;
    private final JSpinner limitDaysSpinner;
    private final JSpinner startSpinner, startMinSpinner, endSpinner, endMinSpinner;
    private final JCheckBox breakCheck;
    private final JSpinner breakStartSpinner, breakStartMinSpinner, breakDurSpinner;
    private final JSpinner weeksSpinner;
    private javax.swing.table.DefaultTableModel priorityModel;
    private javax.swing.table.DefaultTableModel availModel;

    public GeneratePanel(TimetableViewPanel viewPanel) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);

        // ── Settings ────────────────────────────────────────────────────────
        JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        daysPanel.setBackground(AppTheme.SURFACE);
        daysPanel.setBorder(BorderFactory.createTitledBorder("Working Days"));
        monBox = new JCheckBox("Mon", true);
        tueBox = new JCheckBox("Tue", true);
        wedBox = new JCheckBox("Wed", true);
        thuBox = new JCheckBox("Thu", true);
        friBox = new JCheckBox("Fri", true);
        satBox = new JCheckBox("Sat", false);
        limitDaysCheck = new JCheckBox("Limit to", false);
        limitDaysCheck.setOpaque(false);
        limitDaysSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        ((JSpinner.DefaultEditor) limitDaysSpinner.getEditor()).getTextField().setColumns(2);
        limitDaysSpinner.setEnabled(false);
        daysPanel.add(monBox); daysPanel.add(tueBox); daysPanel.add(wedBox);
        daysPanel.add(thuBox); daysPanel.add(friBox); daysPanel.add(satBox);
        daysPanel.add(Box.createHorizontalStrut(10));
        daysPanel.add(limitDaysCheck);
        daysPanel.add(limitDaysSpinner);
        daysPanel.add(new JLabel("day(s)"));
        limitDaysCheck.addActionListener(ev -> limitDaysSpinner.setEnabled(limitDaysCheck.isSelected()));
        java.awt.event.ActionListener dayCheckListener = ev -> updateDayLimitSpinnerMax();
        monBox.addActionListener(dayCheckListener);
        tueBox.addActionListener(dayCheckListener);
        wedBox.addActionListener(dayCheckListener);
        thuBox.addActionListener(dayCheckListener);
        friBox.addActionListener(dayCheckListener);
        satBox.addActionListener(dayCheckListener);
        updateDayLimitSpinnerMax();

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        timePanel.setBackground(AppTheme.SURFACE);
        timePanel.setBorder(BorderFactory.createTitledBorder("Daily Time Range"));
        startSpinner    = new JSpinner(new SpinnerNumberModel(8, 6, 22, 1));
        startMinSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        endSpinner      = new JSpinner(new SpinnerNumberModel(17, 7, 23, 1));
        endMinSpinner   = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        ((JSpinner.DefaultEditor) startMinSpinner.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) endMinSpinner.getEditor()).getTextField().setColumns(2);
        timePanel.add(new JLabel("Start:"));
        timePanel.add(startSpinner);
        timePanel.add(new JLabel("h"));
        timePanel.add(startMinSpinner);
        timePanel.add(new JLabel("min    End:"));
        timePanel.add(endSpinner);
        timePanel.add(new JLabel("h"));
        timePanel.add(endMinSpinner);
        timePanel.add(new JLabel("min"));

        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(AppTheme.BG);
        GridBagConstraints sc = new GridBagConstraints();
        sc.fill = GridBagConstraints.BOTH;
        sc.weighty = 0;
        sc.gridy = 0;
        sc.gridx = 0; sc.weightx = 0.6; sc.insets = new Insets(0, 0, 6, 4);
        settingsPanel.add(daysPanel, sc);
        sc.gridx = 1; sc.weightx = 0.4; sc.insets = new Insets(0, 4, 6, 0);
        settingsPanel.add(timePanel, sc);

        // ── Break / Interval ─────────────────────────────────────────────────
        JPanel breakPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        breakPanel.setBackground(AppTheme.SURFACE);
        breakPanel.setBorder(BorderFactory.createTitledBorder("Break / Interval"));
        breakCheck        = new JCheckBox("Add break period", true);
        breakStartSpinner    = new JSpinner(new SpinnerNumberModel(12, 6, 22, 1));
        breakStartMinSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        breakDurSpinner      = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        ((JSpinner.DefaultEditor) breakStartSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) breakStartMinSpinner.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) breakDurSpinner.getEditor()).getTextField().setColumns(2);
        breakPanel.add(breakCheck);
        breakPanel.add(new JLabel("Start:"));
        breakPanel.add(breakStartSpinner);
        breakPanel.add(new JLabel("h"));
        breakPanel.add(breakStartMinSpinner);
        breakPanel.add(new JLabel("min    Duration (h):"));
        breakPanel.add(breakDurSpinner);
        breakCheck.addActionListener(ev -> {
            breakStartSpinner.setEnabled(breakCheck.isSelected());
            breakStartMinSpinner.setEnabled(breakCheck.isSelected());
            breakDurSpinner.setEnabled(breakCheck.isSelected());
        });
        sc.gridy = 1; sc.gridx = 0; sc.weightx = 0.6; sc.insets = new Insets(0, 0, 0, 4);
        settingsPanel.add(breakPanel, sc);

        // ── Rotational weeks ──────────────────────────────────────────────────
        JPanel weeksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        weeksPanel.setBackground(AppTheme.SURFACE);
        weeksPanel.setBorder(BorderFactory.createTitledBorder("Rotational Weeks"));
        weeksSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 8, 1));
        ((JSpinner.DefaultEditor) weeksSpinner.getEditor()).getTextField().setColumns(3);
        weeksPanel.add(new JLabel("Number of weeks:"));
        weeksPanel.add(weeksSpinner);
        JLabel weeksHint = new JLabel("<html><i>each week gets a different shuffled schedule</i></html>");
        weeksHint.setFont(AppTheme.FONT_SMALL);
        weeksHint.setForeground(AppTheme.TEXT2);
        weeksPanel.add(weeksHint);
        sc.gridx = 1; sc.weightx = 0.4; sc.insets = new Insets(0, 4, 0, 0);
        settingsPanel.add(weeksPanel, sc);

        // ── Scheduling Adjustments (priorities + teacher availability) ──────────────────────────────
        // -- Subject Priorities --
        priorityModel = new javax.swing.table.DefaultTableModel(
            new String[]{"Subject", "Priority", "Preferred Day", "Lock"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c >= 1; }
            @Override public Class<?> getColumnClass(int c) {
                if (c == 3) return Boolean.class;
                return super.getColumnClass(c);
            }
        };
        JTable priorityTable = new JTable(priorityModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent ev) {
                int vCol = columnAtPoint(ev.getPoint());
                if (vCol < 0) return null;
                int mCol = convertColumnIndexToModel(vCol);
                return switch (mCol) {
                    case 0 -> "Subject name";
                    case 1 -> "Priority: 1 = low, 5 = high (higher priority is scheduled earlier)";
                    case 2 -> "Preferred weekday for this subject (None = no preferred day)";
                    case 3 -> "Lock this subject to the Preferred Day only";
                    default -> null;
                };
            }
        };
        priorityTable.setRowHeight(22);
        AppTheme.styleTable(priorityTable);
        JComboBox<Integer> priorityCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        JComboBox<String> prefDayCombo = new JComboBox<>(new String[]{
                "None", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        });
        priorityTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        priorityTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        priorityTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        priorityTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        priorityTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(priorityCombo));
        priorityTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(prefDayCombo));
        priorityTable.getTableHeader().setToolTipText("Hover columns for help");
        priorityTable.getTableHeader().addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent ev) {
                int vCol = priorityTable.getTableHeader().columnAtPoint(ev.getPoint());
                if (vCol < 0) {
                    priorityTable.getTableHeader().setToolTipText(null);
                    return;
                }
                int mCol = priorityTable.convertColumnIndexToModel(vCol);
                String tip = switch (mCol) {
                    case 0 -> "Subject name";
                    case 1 -> "Priority: 1 = low, 5 = high";
                    case 2 -> "Preferred weekday for this subject";
                    case 3 -> "Tick to force this subject only on its Preferred Day";
                    default -> null;
                };
                priorityTable.getTableHeader().setToolTipText(tip);
            }
        });
        priorityModel.addTableModelListener(ev -> {
              if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = ev.getFirstRow();
              if (row < 0 || row >= priorityModel.getRowCount() || row >= DataStore.getSubjects().size()) return;
            int col = ev.getColumn();
            model.Subject s = DataStore.getSubjects().get(row);
            if (col == 1 || col == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
                Object val = priorityModel.getValueAt(row, 1);
                if (val instanceof Integer p) s.setPriority(p);
            }
            if (col == 2 || col == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
                Object val = priorityModel.getValueAt(row, 2);
                String d = (val == null) ? "None" : val.toString();
                s.setPreferredDay("None".equals(d) ? null : d);
                if ("None".equals(d) && s.isPreferredDayOnly()) {
                    s.setPreferredDayOnly(false);
                    Object cur = priorityModel.getValueAt(row, 3);
                    if (!(cur instanceof Boolean b) || b) {
                        priorityModel.setValueAt(Boolean.FALSE, row, 3);
                    }
                }
            }
            if (col == 3 || col == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
                Object val = priorityModel.getValueAt(row, 3);
                boolean only = (val instanceof Boolean b) && b;
                s.setPreferredDayOnly(only);
                if (only && (s.getPreferredDay() == null || s.getPreferredDay().isBlank())) {
                    // Default to Monday when user ticks "only" without selecting a day.
                    s.setPreferredDay("Monday");
                    Object cur = priorityModel.getValueAt(row, 2);
                    if (cur == null || !"Monday".equals(cur.toString())) {
                        priorityModel.setValueAt("Monday", row, 2);
                    }
                }
            }
        });

        JPanel leftAdj = new JPanel(new BorderLayout(0, 4));
        leftAdj.setBorder(BorderFactory.createTitledBorder("Subject Priorities"));
        leftAdj.add(new JScrollPane(priorityTable), BorderLayout.CENTER);

        // -- Teacher Availability --
        availModel = new javax.swing.table.DefaultTableModel(new String[]{"Teacher", "Blocked Days"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable availTable = new JTable(availModel);
        availTable.setRowHeight(22);
        AppTheme.styleTable(availTable);
        availTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        availTable.getColumnModel().getColumn(1).setPreferredWidth(180);

        JButton editAvailBtn = new JButton("Edit Unavailable Days…");        AppTheme.styleSecondary(editAvailBtn);        editAvailBtn.addActionListener(ev -> {
            int row = availTable.getSelectedRow();
            if (row < 0 || row >= DataStore.getTeachers().size()) {
                JOptionPane.showMessageDialog(this,
                        "Select a teacher row first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            model.Teacher t = DataStore.getTeachers().get(row);
            String[] allDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            JCheckBox[] boxes = new JCheckBox[allDays.length];
            JPanel dayPanel = new JPanel(new GridLayout(0, 1, 2, 2));
            dayPanel.add(new JLabel("<html><b>Tick days the teacher cannot work:</b></html>"));
            for (int i = 0; i < allDays.length; i++) {
                boxes[i] = new JCheckBox(allDays[i], t.getBlockedDays().contains(allDays[i]));
                dayPanel.add(boxes[i]);
            }
            int res = JOptionPane.showConfirmDialog(this, dayPanel,
                    t.getName() + " — Unavailable Days",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            t.clearBlockedDays();
            for (int i = 0; i < allDays.length; i++) { if (boxes[i].isSelected()) t.blockDay(allDays[i]); }
            refreshAdjustments();
        });

        JPanel rightAdj = new JPanel(new BorderLayout(0, 4));
        rightAdj.setBorder(BorderFactory.createTitledBorder("Teacher Availability"));
        rightAdj.add(new JScrollPane(availTable), BorderLayout.CENTER);
        rightAdj.add(editAvailBtn, BorderLayout.SOUTH);

        JButton reloadAdjBtn = new JButton("<html><font face='Segoe UI Symbol'>\u21BB</font> Reload from Data</html>");
        AppTheme.styleSecondary(reloadAdjBtn);
        reloadAdjBtn.setToolTipText("Reload subject and teacher data from the other tabs");
        reloadAdjBtn.addActionListener(ev -> refreshAdjustments());

        JPanel adjBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        adjBottom.add(reloadAdjBtn);
        adjBottom.add(new JLabel("  Reload after adding/editing subjects or teachers."));

        JPanel adjCenter = new JPanel(new GridLayout(1, 2, 8, 0));
        adjCenter.add(leftAdj);
        adjCenter.add(rightAdj);

        JPanel adjPanel = new JPanel(new BorderLayout(0, 4));
        adjPanel.setBorder(BorderFactory.createTitledBorder("Scheduling Adjustments"));
        adjPanel.add(adjCenter, BorderLayout.CENTER);
        adjPanel.add(adjBottom, BorderLayout.SOUTH);

        // ── Generate button + emergency reschedule + progress + status ──────────────────────────────
        JButton generateBtn = new JButton("Generate Timetable");
        AppTheme.stylePrimary(generateBtn);
        generateBtn.setFont(generateBtn.getFont().deriveFont(Font.BOLD, 14f));
        generateBtn.setPreferredSize(new Dimension(220, 44));

        JButton emergencyBtn = new JButton("Mark Teacher Absent");
        AppTheme.styleDanger(emergencyBtn);
        emergencyBtn.setToolTipText("Remove a teacher's sessions for a chosen day and reschedule them with a substitute");

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("");

        JTextArea statusArea = new JTextArea(12, 50);
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusArea.setBackground(new Color(0xF8FAFC));
        statusArea.setForeground(AppTheme.TEXT);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Generation Log"));

        // ── Action cards ──────────────────────────────────────────────────────
        JPanel generateCard = makeActionCard(
                "\u26A1", "Generate Timetable",
                "Build a conflict-free schedule from the current data.",
                new Color(0x2563EB), generateBtn);

        JPanel emergencyCard = makeActionCard(
                "\uD83D\uDEA8", "Mark Teacher Absent",
                "Remove a teacher for a day and assign a substitute.",
                new Color(0xDC2626), emergencyBtn);

        JPanel actionsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        actionsRow.setBackground(AppTheme.BG);
        actionsRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        actionsRow.add(generateCard);
        actionsRow.add(emergencyCard);

        JPanel btnPanel = new JPanel(new BorderLayout(0, 6));
        btnPanel.setBackground(AppTheme.BG);
        btnPanel.add(actionsRow,  BorderLayout.CENTER);
        btnPanel.add(progressBar, BorderLayout.SOUTH);

        JPanel topSection = new JPanel(new BorderLayout(0, 6));
        topSection.setOpaque(false);
        topSection.add(settingsPanel, BorderLayout.NORTH);
        topSection.add(btnPanel,      BorderLayout.SOUTH);

        // Split the adjustments panel and the status log vertically so both always have space
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, adjPanel, statusScroll);
        centerSplit.setResizeWeight(0.45);      // 45% to adjustments, 55% to status
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setBorder(null);
        centerSplit.setPreferredSize(new Dimension(900, 420));

        contentPanel.add(topSection, BorderLayout.NORTH);
        contentPanel.add(centerSplit, BorderLayout.CENTER);

        JScrollPane pageScroll = new JScrollPane(contentPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pageScroll.setBorder(null);
        pageScroll.getViewport().setBackground(AppTheme.BG);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScroll, BorderLayout.CENTER);

        // ── Action ────────────────────────────────────────────────────────────
        generateBtn.addActionListener(e -> {
            statusArea.setText("");
            progressBar.setIndeterminate(true);
            progressBar.setString("Working…");
            generateBtn.setEnabled(false);
            emergencyBtn.setEnabled(false);
            viewPanel.clearWeekSchedules();

            // snapshot settings on the EDT before handing off to worker
            int start    = (int) startSpinner.getValue();
            int startMin = (int) startMinSpinner.getValue();
            int end      = (int) endSpinner.getValue();
            int endMin   = (int) endMinSpinner.getValue();
            int numWeeks = (int) weeksSpinner.getValue();
            java.util.Set<String> sharedTypes = new java.util.LinkedHashSet<>(DataStore.getMultiGroupSubjectTypes());
            java.util.List<String> checkedDays = collectSelectedWorkingDays();
            java.util.List<String> days = new java.util.ArrayList<>(checkedDays);
            int dayLimit = limitDaysCheck.isSelected() ? (int) limitDaysSpinner.getValue() : 0;
            java.util.Set<Integer> breakMinutes = buildBreakMinutes();

            new SwingWorker<java.util.List<java.util.List<ScheduleEntry>>, String>() {

                @Override
                protected java.util.List<java.util.List<ScheduleEntry>> doInBackground() throws Exception {
                    publish("[1/5] Clearing previous schedule...");
                    DataStore.clearWeekSchedules();
                    DataStore.clearTimeSlots();
                    DataStore.setLastBreakMinutes(java.util.Collections.emptySet());

                    publish("[2/5] Validating settings...");
                    if (start >= end)
                        throw new IllegalArgumentException("Start hour must be before end hour.");
                    if (days.isEmpty())
                        throw new IllegalArgumentException("Select at least one working day.");

                    String limitInfo = (dayLimit > 0 && dayLimit < days.size())
                            ? " (max " + dayLimit + " day(s) per group)"
                            : "";
                    publish("[3/5] Building time slots for " + days.size() + " day(s)" + limitInfo
                            + (breakMinutes.isEmpty() ? "" : " (break: "
                                + breakMinutes.size() + "h excluded)") + "...");
                    DataStore.generateTimeSlots(days.toArray(String[]::new), start, startMin, end, endMin,
                            breakMinutes);
                    DataStore.setLastBreakMinutes(breakMinutes);
                    publish("      Active days: " + String.join(", ", days));
                    publish("      → " + DataStore.getTimeSlots().size() + " slots generated.");

                    publish("[4/5] Validating data...");
                    if (DataStore.getSubjects().isEmpty())
                        throw new IllegalStateException("No subjects added.");

                    applySharedTypeRules(sharedTypes);
                    if (!sharedTypes.isEmpty()) {
                        publish("      Shared types: " + String.join(", ", sharedTypes));
                    } else {
                        publish("      Shared types: none");
                    }

                    publish("      Subjects  : " + DataStore.getSubjects().size());
                    if (DataStore.getTeachers().isEmpty())
                        throw new IllegalStateException("No teachers added.");
                    publish("      Teachers  : " + DataStore.getTeachers().size());
                    if (DataStore.getRooms().isEmpty())
                        throw new IllegalStateException("No rooms added.");
                    publish("      Rooms     : " + DataStore.getRooms().size());
                    if (DataStore.getGroups().isEmpty())
                        throw new IllegalStateException("__AUTO_GROUP__");
                    publish("      Groups    : " + DataStore.getGroups().size());
                    long emptyGroups = DataStore.getGroups().stream()
                            .filter(g -> g.getSubjects().isEmpty()).count();
                    if (emptyGroups > 0)
                        publish("      WARNING  : " + emptyGroups + " group(s) have NO subjects assigned"
                                + " — those groups will produce 0 sessions."
                                + " Assign subjects in the Groups tab.");

                    publish("[5/5] Running scheduler for " + numWeeks + " week(s)...");
                    Scheduler scheduler = new Scheduler();
                    if (dayLimit > 0) scheduler.setMaxDaysPerGroup(dayLimit);
                    java.util.List<java.util.List<ScheduleEntry>> allWeeks = new java.util.ArrayList<>();
                    for (int w = 1; w <= numWeeks; w++) {
                        publish("  \u25b6 Week " + w + "/" + numWeeks + ":");
                        java.util.List<ScheduleEntry> week = scheduler.generate(msg -> publish("    " + msg));
                        allWeeks.add(week);
                        publish("    \u2713 Week " + w + ": " + week.size() + " session(s) placed.");
                    }
                    return allWeeks;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String msg : chunks) {
                        statusArea.append(msg + "\n");
                    }
                    // auto-scroll to bottom
                    statusArea.setCaretPosition(statusArea.getDocument().getLength());
                }

                @Override
                protected void done() {
                    progressBar.setIndeterminate(false);
                    generateBtn.setEnabled(true);
                    emergencyBtn.setEnabled(true);

                    java.util.List<java.util.List<ScheduleEntry>> allWeeks;
                    try {
                        allWeeks = get();
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IllegalStateException
                                && "__AUTO_GROUP__".equals(cause.getMessage())) {
                            // ask on EDT
                            if (!tryAutoCreateGroup()) {
                                statusArea.append("\nERROR: No student groups — cancelled.\n");
                                progressBar.setString("Cancelled");
                                viewPanel.refresh();
                                return;
                            }
                            statusArea.append("      Auto-created group.\n");
                            // re-trigger generation (groups now exist)
                            generateBtn.doClick();
                            return;
                        }
                        String msg = cause != null ? cause.getMessage() : ex.getMessage();
                        statusArea.append("\nERROR: " + msg + "\n");
                        progressBar.setString("Error");
                        viewPanel.refresh();
                        return;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        statusArea.append("\nERROR: Generation interrupted.\n");
                        progressBar.setString("Interrupted");
                        viewPanel.refresh();
                        return;
                    }

                    DataStore.setWeekSchedules(allWeeks);
                    viewPanel.setWeekSchedules(allWeeks);

                    List<ScheduleEntry> result = allWeeks.isEmpty() ? java.util.Collections.emptyList() : allWeeks.get(0);

                    statusArea.append("\n");
                    if (result.isEmpty()) {
                        statusArea.append("""
                                WARNING: No schedule could be generated.
                                Check that teachers are assigned to subjects,
                                rooms have sufficient capacity, and time slots are defined.
                                """);
                        progressBar.setString("No schedule generated");
                        return;
                    }

                    // Per-group session counts
                    Map<String, Long> perGroup = result.stream()
                            .collect(Collectors.groupingBy(
                                    e -> e.getGroup().getName(), Collectors.counting()));
                    DataStore.getGroups().forEach(g -> {
                        long cnt = perGroup.getOrDefault(g.getName(), 0L);
                        statusArea.append("  " + g.getName() + ": " + cnt + " session(s)\n");
                    });
                    int unscheduled = countUnscheduled();
                    statusArea.append("Total entries     : " + result.size() + "\n");
                    if (unscheduled > 0) {
                        statusArea.append("Unscheduled slots : " + unscheduled
                                + " (no free teacher/room/time)\n");
                        progressBar.setString("Done with " + unscheduled + " unscheduled");
                    } else {
                        statusArea.append("All sessions placed successfully.\n");
                        progressBar.setString("Done — " + result.size() + " entries");
                    }
                    statusArea.append("\nSwitch to 'Timetable View' tab to see the results.");
                    statusArea.setCaretPosition(statusArea.getDocument().getLength());
                }
            }.execute();
        });

        // ── Emergency: Mark Teacher Absent ───────────────────────────────────
        emergencyBtn.addActionListener(e -> {
            List<model.Teacher> teachers = DataStore.getTeachers();
            if (teachers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No teachers in the system.",
                        "Emergency Reschedule", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (DataStore.getSchedule().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Generate a timetable first.",
                        "Emergency Reschedule", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // Pick teacher
            model.Teacher[] teacherArr = teachers.toArray(model.Teacher[]::new);
            model.Teacher absent = (model.Teacher) JOptionPane.showInputDialog(
                    this, "Select the absent teacher:", "Mark Teacher Absent",
                    JOptionPane.QUESTION_MESSAGE, null, teacherArr, teacherArr[0]);
            if (absent == null) return;

            // Pick day
            java.util.Set<String> daysInSchedule = DataStore.getSchedule().stream()
                    .filter(s -> s.getTeacher().equals(absent))
                    .map(s -> s.getSlot().getDay())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            if (daysInSchedule.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        absent.getName() + " has no sessions scheduled.",
                        "Emergency Reschedule", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] dayArr = daysInSchedule.toArray(String[]::new);
            String absentDay = (String) JOptionPane.showInputDialog(
                    this, "Select the day they are absent:", "Mark Teacher Absent",
                    JOptionPane.QUESTION_MESSAGE, null, dayArr, dayArr[0]);
            if (absentDay == null) return;

            // Temporarily block the absent teacher for that day
            absent.blockDay(absentDay);

            // Remove their entries for that day from ALL week schedules
            List<List<ScheduleEntry>> weeks = DataStore.getWeekSchedules();
            int replaced = 0, failed = 0;
            for (List<ScheduleEntry> weekSched : weeks) {
                List<ScheduleEntry> toRemove = weekSched.stream()
                        .filter(s -> s.getTeacher().equals(absent)
                                  && s.getSlot().getDay().equals(absentDay))
                        .collect(Collectors.toList());
                weekSched.removeAll(toRemove);

                // Try to fill each removed session with a substitute
                Scheduler sched = new Scheduler();
                for (ScheduleEntry removed : toRemove) {
                    int sh  = removed.getStartHour();
                    int dur = removed.getDurationHours();
                    model.Teacher sub = sched.findSubstitute(
                            removed.getSubject(), DataStore.getTeachers(),
                            weekSched, absentDay, sh, dur);
                    if (sub != null) {
                        model.TimeSlot slot = removed.getSlot();
                        weekSched.add(new ScheduleEntry(slot, removed.getSubject(),
                                sub, removed.getGroup(), removed.getRoom(), dur));
                        replaced++;
                    } else {
                        failed++;
                    }
                }
            }

            // Persist and refresh
            DataStore.setWeekSchedules(weeks);
            viewPanel.setWeekSchedules(weeks);

            // Unblock the teacher (day just handled as one-off; persistent block via TeacherPanel)
            absent.unblockDay(absentDay);

            String msg = absent.getName() + " marked absent on " + absentDay + ".\n"
                    + "Sessions substituted : " + replaced + "\n"
                    + "Sessions unresolved  : " + failed
                    + (failed > 0 ? "\n(No available substitute for " + failed + " session(s))" : "");
            statusArea.append("\n[Emergency Reschedule]\n" + msg + "\n");
            JOptionPane.showMessageDialog(this, msg, "Emergency Reschedule Complete",
                    failed > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /** Reload subject priorities and teacher availability from DataStore into the adjustment tables. */
    public void refreshAdjustments() {
        priorityModel.setRowCount(0);
        for (model.Subject s : DataStore.getSubjects()) {
            priorityModel.addRow(new Object[]{
                    s.getName(),
                    s.getPriority(),
                    s.getPreferredDay() == null ? "None" : s.getPreferredDay(),
                    s.isPreferredDayOnly()
            });
        }
        availModel.setRowCount(0);
        for (model.Teacher t : DataStore.getTeachers()) {
            String blocked = t.getBlockedDays().isEmpty() ? "— none —" : String.join(", ", t.getBlockedDays());
            availModel.addRow(new Object[]{t.getName(), blocked});
        }
    }

    /** Reads the checked working days in canonical order. */
    private java.util.List<String> collectSelectedWorkingDays() {
        java.util.List<String> days = new java.util.ArrayList<>();
        if (monBox.isSelected()) days.add("Monday");
        if (tueBox.isSelected()) days.add("Tuesday");
        if (wedBox.isSelected()) days.add("Wednesday");
        if (thuBox.isSelected()) days.add("Thursday");
        if (friBox.isSelected()) days.add("Friday");
        if (satBox.isSelected()) days.add("Saturday");
        return days;
    }

    /** Updates the limit-days spinner max to match the number of checked day boxes. */
    private void updateDayLimitSpinnerMax() {
        int count = collectSelectedWorkingDays().size();
        if (count < 1) count = 1;
        SpinnerNumberModel m = (SpinnerNumberModel) limitDaysSpinner.getModel();
        m.setMaximum(count);
        if ((int) m.getValue() > count) m.setValue(count);
    }

    /** Builds the set of total-minutes slot starts to exclude based on the break/interval settings. */
    private java.util.Set<Integer> buildBreakMinutes() {
        if (!breakCheck.isSelected()) return java.util.Collections.emptySet();
        int bsH = (int) breakStartSpinner.getValue();
        int bsM = (int) breakStartMinSpinner.getValue();
        int bd  = (int) breakDurSpinner.getValue();
        int bsTotal = bsH * 60 + bsM;
        java.util.Set<Integer> mins = new java.util.HashSet<>();
        for (int m = bsTotal; m < bsTotal + bd * 60; m += 60) mins.add(m);
        return mins;
    }

    /** Apply selected shared-type rules to all subjects before generation. */
    private void applySharedTypeRules(java.util.Set<String> sharedTypes) {
        for (Subject s : DataStore.getSubjects()) {
            boolean share = false;
            String type = s.getType();
            if (type != null) {
                for (String part : type.split(";")) {
                    if (sharedTypes.contains(part.trim())) {
                        share = true;
                        break;
                    }
                }
            }
            s.setAllGroups(share);
        }
    }

    /**
     * Shows a quick dialog to create a group when none exist.
     * Returns true if a group was created, false if the user cancelled.
     */
    private boolean tryAutoCreateGroup() {
        JTextField nameField = new JTextField("Group1", 12);
        JTextField sizeField = new JTextField("30", 6);

        java.util.List<Subject> allSubjects = DataStore.getSubjects();
        java.util.List<JCheckBox> checkBoxes = new java.util.ArrayList<>();
        JPanel subPanel = new JPanel(new GridLayout(0, 2));
        for (Subject s : allSubjects) {
            JCheckBox cb = new JCheckBox(s.toString(), true);
            checkBoxes.add(cb);
            subPanel.add(cb);
        }

        JPanel form = new JPanel(new BorderLayout(5, 8));
        JPanel top = new JPanel(new GridLayout(2, 2, 4, 4));
        top.add(new JLabel("Group Name:"));       top.add(nameField);
        top.add(new JLabel("No. of Students:")); top.add(sizeField);
        form.add(top, BorderLayout.NORTH);
        if (!allSubjects.isEmpty()) {
            form.add(new JLabel("Assign Subjects:"), BorderLayout.CENTER);
            form.add(new JScrollPane(subPanel), BorderLayout.SOUTH);
        }

        int result = JOptionPane.showConfirmDialog(
                this, form,
                "No groups defined — Create a group now?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return false;

        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Group1";
        int size;
        try { size = Integer.parseInt(sizeField.getText().trim()); }
        catch (NumberFormatException ex) { size = 30; }

        StudentGroup group = new StudentGroup(name, size);
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) group.addSubject(allSubjects.get(i));
        }
        DataStore.addGroup(group);
        return true;
    }

    private int countUnscheduled() {
        int total = 0;
        for (StudentGroup g : DataStore.getGroups()) {
            for (Subject s : g.getSubjects()) {
                int dur = s.getSessionDuration();
                int needed = (int) Math.ceil((double) s.getWeeklyHours() / dur);
                long placed = DataStore.getSchedule().stream()
                        .filter(e -> e.getGroup().equals(g) && e.getSubject().equals(s))
                        .count();
                if (placed < needed) total += (needed - placed);
            }
        }
        return total;
    }

    /**
     * Creates a styled action card: coloured left stripe, icon + title + description,
     * and the provided button docked to the right side.
     */
    private static JPanel makeActionCard(String icon, String title, String desc,
                                          Color accentColor, JButton btn) {
        // Left accent stripe
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(5, 0));

        // Icon label
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        iconLbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));

        // Title + description stacked
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.TEXT);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(AppTheme.FONT_SMALL);
        descLbl.setForeground(AppTheme.TEXT2);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(descLbl);

        // Button sizing
        btn.setPreferredSize(new Dimension(180, 36));
        btn.setMaximumSize(new Dimension(200, 36));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btn);

        // Centre row: icon + text + button
        JPanel inner = new JPanel(new BorderLayout(0, 0));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(iconLbl, BorderLayout.WEST);
        left.add(textPanel, BorderLayout.CENTER);
        inner.add(left,    BorderLayout.CENTER);
        inner.add(btnWrap, BorderLayout.EAST);

        // Card shell
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        card.add(stripe, BorderLayout.WEST);
        card.add(inner,  BorderLayout.CENTER);
        return card;
    }
}

