package ui;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.*;
import model.ScheduleEntry;
import util.DataStore;

public class TimetableViewPanel extends JPanel {

    private static final String VIEW_CLASS   = "Class / Group";
    private static final String VIEW_TEACHER = "Teacher";
    private static final String VIEW_ROOM    = "Room";

    /** Sentinel value placed in continuation rows (hours covered by a multi-hour block). */
    private static final String CONT  = "\0";
    /** Sentinel value placed in break/interval rows. */
    private static final String BREAK = "\1";

    private final JComboBox<String> viewSelector;
    private final JComboBox<String> weekSelector;
    private final JPanel allTablesPanel;
    private final JLabel titleLabel;
    private final JTextField searchField;
    private final java.util.List<java.util.List<model.ScheduleEntry>> weekSchedules = new java.util.ArrayList<>();

    public TimetableViewPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // ── Controls ─────────────────────────────────────────────────────────
        viewSelector = new JComboBox<>(new String[]{VIEW_CLASS, VIEW_TEACHER, VIEW_ROOM});
        weekSelector = new JComboBox<>(new String[]{"Week 1"});
        weekSelector.setToolTipText("Select the rotational week to view");
        JButton refreshBtn = new JButton("Refresh");
        AppTheme.styleSecondary(refreshBtn);
        JButton downloadBtn = new JButton("Download HTML");
        AppTheme.styleSecondary(downloadBtn);

        searchField = new JTextField(16);
        searchField.setToolTipText("Filter by subject, teacher, room, or group");

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        leftControls.setOpaque(false);
        leftControls.add(new JLabel("View:"));
        leftControls.add(viewSelector);
        leftControls.add(new JLabel("  Week:"));
        leftControls.add(weekSelector);
        leftControls.add(refreshBtn);
        leftControls.add(Box.createHorizontalStrut(12));
        leftControls.add(new JLabel("Search:"));
        leftControls.add(searchField);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        rightControls.setOpaque(false);
        rightControls.add(downloadBtn);

        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setBackground(AppTheme.SURFACE);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        controls.add(leftControls, BorderLayout.CENTER);
        controls.add(rightControls, BorderLayout.EAST);

        // ── Title ─────────────────────────────────────────────────────────────
        titleLabel = new JLabel(" ", SwingConstants.CENTER);
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT2);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBackground(AppTheme.BG);
        top.add(controls,   BorderLayout.NORTH);
        top.add(titleLabel, BorderLayout.SOUTH);

        // ── All-tables area ───────────────────────────────────────────────────
        allTablesPanel = new JPanel();
        allTablesPanel.setLayout(new BoxLayout(allTablesPanel, BoxLayout.Y_AXIS));
        allTablesPanel.setBackground(AppTheme.BG);
        JScrollPane allTablesScroll = new JScrollPane(allTablesPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        allTablesScroll.getVerticalScrollBar().setUnitIncrement(16);
        allTablesScroll.setBorder(null);
        allTablesScroll.getViewport().setBackground(AppTheme.BG);

        add(top,             BorderLayout.NORTH);
        add(allTablesScroll, BorderLayout.CENTER);

        // ── Listeners ────────────────────────────────────────────────────────
        viewSelector.addActionListener(e -> buildTabs());
        weekSelector.addActionListener(e -> buildTabs());
        refreshBtn.addActionListener(e -> refresh());
        downloadBtn.addActionListener(e -> exportCurrentView());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent ev)  { buildTabs(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent ev)  { buildTabs(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent ev) { buildTabs(); }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void refresh() {
        buildTabs();
    }

    /** Clears all locally held week data and refreshes to an empty view. */
    public void clearWeekSchedules() {
        weekSchedules.clear();
        weekSelector.removeAllItems();
        weekSelector.addItem("Week 1");
        buildTabs();
    }

    /** Called by GeneratePanel after generation; replaces stored week data and refreshes view. */
    public void setWeekSchedules(java.util.List<java.util.List<model.ScheduleEntry>> weeks) {
        weekSchedules.clear();
        weekSchedules.addAll(weeks);
        // Rebuild week selector options
        String prev = (String) weekSelector.getSelectedItem();
        weekSelector.removeAllItems();
        for (int i = 1; i <= weeks.size(); i++) weekSelector.addItem("Week " + i);
        if (prev != null) {
            for (int i = 0; i < weekSelector.getItemCount(); i++) {
                if (weekSelector.getItemAt(i).equals(prev)) { weekSelector.setSelectedIndex(i); break; }
            }
        }
        buildTabs();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void buildTabs() {
        String view = (String) viewSelector.getSelectedItem();
        allTablesPanel.removeAll();
        titleLabel.setText(view);

        // Determine which schedule to display (selected week or fallback to DataStore)
        int weekIdx = weekSelector.getSelectedIndex();
        List<ScheduleEntry> activeSchedule;
        if (!weekSchedules.isEmpty() && weekIdx >= 0 && weekIdx < weekSchedules.size()) {
            activeSchedule = weekSchedules.get(weekIdx);
        } else {
            activeSchedule = DataStore.getSchedule();
        }

        // Apply search filter
        String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        List<ScheduleEntry> filteredSchedule = query.isEmpty() ? activeSchedule
                : activeSchedule.stream().filter(e ->
                        e.getSubject().getName().toLowerCase(java.util.Locale.ROOT).contains(query) ||
                        e.getTeacher().getName().toLowerCase(java.util.Locale.ROOT).contains(query) ||
                        e.getRoom().getId().toLowerCase(java.util.Locale.ROOT).contains(query) ||
                        e.getGroup().getName().toLowerCase(java.util.Locale.ROOT).contains(query))
                  .collect(Collectors.toList());

        switch (view) {
            case VIEW_CLASS -> {
                if (DataStore.getGroups().isEmpty()) { showEmpty("No groups defined."); return; }
                DataStore.getGroups().forEach(g ->
                    addEntitySection(g.getName(), e -> e.getGroup().getName().equals(g.getName()), view, filteredSchedule));
            }
            case VIEW_TEACHER -> {
                if (DataStore.getTeachers().isEmpty()) { showEmpty("No teachers defined."); return; }
                DataStore.getTeachers().forEach(t ->
                    addEntitySection(t.getName(), e -> e.getTeacher().getName().equals(t.getName()), view, filteredSchedule));
            }
            case VIEW_ROOM -> {
                if (DataStore.getRooms().isEmpty()) { showEmpty("No rooms defined."); return; }
                DataStore.getRooms().forEach(r ->
                    addEntitySection(r.getId(), e -> e.getRoom().getId().equals(r.getId()), view, filteredSchedule));
            }

        }

        allTablesPanel.revalidate();
        allTablesPanel.repaint();
    }

    private void showEmpty(String msg) {
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(AppTheme.TEXT2);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        allTablesPanel.add(lbl);
        allTablesPanel.revalidate();
        allTablesPanel.repaint();
    }

    /** Adds one section for a single entity (teacher/group/room/student). */
    private void addEntitySection(String sectionName, Predicate<ScheduleEntry> filter, String view,
                                   List<ScheduleEntry> schedule) {
        List<ScheduleEntry> entries = schedule.stream()
                .filter(filter).collect(Collectors.toList());

        // When searching, skip sections with no matching entries
        if (entries.isEmpty() && !searchField.getText().trim().isEmpty()) return;

        DefaultTableModel model = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        buildGrid(model, entries, view);

        // Section header bar
        JLabel secHeader = new JLabel("  " + sectionName);
        secHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        secHeader.setForeground(Color.WHITE);
        secHeader.setBackground(AppTheme.PRIMARY);
        secHeader.setOpaque(true);
        secHeader.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JTable table = new JTable(model);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new GridCellRenderer());
        // Set correct per-row heights via setRowHeight(row, h) so Swing's rowModel
        // tracks positions correctly (avoids rendering gaps from y = row × defaultHeight)
        table.setRowHeight(68);
        for (int row = 0; row < model.getRowCount(); row++) {
            boolean hasBreak = false;
            boolean hasCont = false;
            boolean hasContent = false;

            for (int col = 1; col < model.getColumnCount(); col++) {
                Object cell = model.getValueAt(row, col);
                if (BREAK.equals(cell)) {
                    hasBreak = true;
                } else if (CONT.equals(cell)) {
                    hasCont = true;
                } else if (cell != null) {
                    hasContent = true;
                }
            }

            if (hasBreak) {
                table.setRowHeight(row, 26);
            } else if (hasContent || hasCont) {
                // Keep standard period height so merged blocks span full period rows.
                table.setRowHeight(row, 68);
            }
        }

        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new BoldHeaderRenderer(th.getDefaultRenderer()));
        th.setPreferredSize(new Dimension(0, 28));

        TableColumnModel cm = table.getColumnModel();
        if (cm.getColumnCount() > 0) {
            cm.getColumn(0).setPreferredWidth(110);
            cm.getColumn(0).setMinWidth(96);
            cm.getColumn(0).setMaxWidth(140);
            for (int i = 1; i < cm.getColumnCount(); i++) cm.getColumn(i).setPreferredWidth(180);
        }

        JScrollPane tablePane = new JScrollPane(table,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tablePane.setBorder(null);
        tablePane.getViewport().setBackground(Color.WHITE);

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(AppTheme.SURFACE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 0, 0, 0),
                BorderFactory.createLineBorder(AppTheme.BORDER, 1)));
        section.add(secHeader,  BorderLayout.NORTH);
        section.add(tablePane,  BorderLayout.CENTER);
        
        // Make section expand horizontally while maintaining proper height in BoxLayout
        Dimension prefSize = section.getPreferredSize();
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefSize.height));

        allTablesPanel.add(section);
    }

    private void buildGrid(DefaultTableModel gridModel, List<ScheduleEntry> entries, String view) {
        // Active days in canonical order
        List<String> days = Arrays.stream(
                new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"})
                .filter(d -> DataStore.getTimeSlots().stream().anyMatch(s -> s.getDay().equals(d))
                          || entries.stream().anyMatch(e -> e.getSlot().getDay().equals(d)))
                .collect(Collectors.toList());

        // All slot start-minutes in ascending order (base slots + breaks + multi-hour continuations)
        SortedSet<Integer> minuteSet = new TreeSet<>();
        DataStore.getTimeSlots().forEach(s -> minuteSet.add(parseStartTotalMinutes(s.getTime())));
        DataStore.getLastBreakMinutes().forEach(minuteSet::add);
        entries.forEach(e -> {
            int startMinute = parseStartTotalMinutes(e.getSlot().getTime());
            for (int i = 0; i < e.getDurationHours(); i++) minuteSet.add(startMinute + (i * 60));
        });
        List<Integer> slotMinutes = new ArrayList<>(minuteSet);

        if (days.isEmpty() || slotMinutes.isEmpty()) {
            gridModel.setRowCount(0); gridModel.setColumnCount(0);
            return;
        }

        // Column headers
        String[] cols = new String[days.size() + 1];
        cols[0] = "Period / Time";
        for (int i = 0; i < days.size(); i++) cols[i + 1] = days.get(i);
        gridModel.setColumnIdentifiers(cols);
        gridModel.setRowCount(0);

        // Group entries sharing the same (day, startMinute, subject, type, teacher, room)
        Map<String, List<ScheduleEntry>> grouped = new LinkedHashMap<>();
        for (ScheduleEntry e : entries) {
            int startMinute = parseStartTotalMinutes(e.getSlot().getTime());
            String key = e.getSlot().getDay() + "|" + startMinute + "|"
                    + e.getSubject().getName() + "|" + e.getSubject().getType() + "|"
                    + e.getTeacher().getName() + "|" + e.getRoom().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        Map<String, List<String>> slotBlocks = new LinkedHashMap<>();
        Set<String> contKeys = new HashSet<>();

        for (Map.Entry<String, List<ScheduleEntry>> eg : grouped.entrySet()) {
            List<ScheduleEntry> list = eg.getValue();
            ScheduleEntry rep = list.get(0);
            int di = days.indexOf(rep.getSlot().getDay());
            int startMinute = parseStartTotalMinutes(rep.getSlot().getTime());
            int mi = slotMinutes.indexOf(startMinute);
            if (di < 0 || mi < 0) continue;

            String groupNames = list.stream()
                    .map(e -> e.getGroup().getName()).distinct().sorted()
                    .collect(Collectors.joining(", "));

            slotBlocks.computeIfAbsent(di + "|" + mi, k -> new ArrayList<>())
                      .add(buildBlock(groupNames, rep, view));

            for (int i = 1; i < rep.getDurationHours(); i++) {
                int nextMinuteIndex = slotMinutes.indexOf(startMinute + (i * 60));
                if (nextMinuteIndex >= 0) contKeys.add(di + "|" + nextMinuteIndex);
            }
        }

        Map<String, String> cellHtml = new HashMap<>();
        for (Map.Entry<String, List<String>> e : slotBlocks.entrySet()) {
            String inner = String.join("<hr style='margin:2px 0'/>", e.getValue());
            cellHtml.put(e.getKey(), "<html><body style='padding:2px'>" + inner + "</body></html>");
        }

        // Build minute → display-label map from actual slot start times
        Map<Integer, String> minuteLabel = new HashMap<>();
        DataStore.getTimeSlots().forEach(s -> {
            int startMinute = parseStartTotalMinutes(s.getTime());
            minuteLabel.putIfAbsent(startMinute, s.getTime().substring(0, 5)); // "HH:MM"
        });

        // Build break-minute set from stored total-minute break starts
        java.util.Set<Integer> breakMinutes = new HashSet<>(DataStore.getLastBreakMinutes());
        // Also add break labels to minuteLabel map
        DataStore.getLastBreakMinutes().forEach(m -> {
            int h = m / 60, min = m % 60;
            minuteLabel.putIfAbsent(m, String.format("%02d:%02d", h, min));
        });

        for (int mi = 0; mi < slotMinutes.size(); mi++) {
            int minute = slotMinutes.get(mi);
            Object[] row = new Object[days.size() + 1];
            if (breakMinutes.contains(minute)) {
                String hhmm = minuteLabel.getOrDefault(minute, formatMinuteLabel(minute));
                row[0] = hhmm;
                // Fill all day columns with break sentinel
                for (int di = 0; di < days.size(); di++) row[di + 1] = BREAK;
            } else {
                String hhmm = minuteLabel.getOrDefault(minute, formatMinuteLabel(minute));
                row[0] = hhmm;
                for (int di = 0; di < days.size(); di++) {
                    String k = di + "|" + mi;
                    row[di + 1] = contKeys.contains(k) ? CONT : cellHtml.get(k);
                }
            }
            gridModel.addRow(row);
        }
    }

    /** Builds the inner HTML block; omits the info that identifies the tab entity itself. */
    private static String buildBlock(String groupNames, ScheduleEntry e, String view) {
        String subject = esc(e.getSubject().getName()) + " " + esc(e.getSubject().getType());
        
        // Extract start time only (HH:MM) from the slot time range (HH:MM - HH:MM)
        //String slotTime = e.getSlot().getTime();
        //String startTime = slotTime.length() >= 5 ? slotTime.substring(0, 5) : slotTime;
        //String time    = "[" + esc(startTime) + "]";
        
        String teacher = esc(e.getTeacher().getName());
        String room    = esc(e.getRoom().getId());
        String group   = esc(groupNames);
        return switch (view) {
            case VIEW_TEACHER ->
                // Teacher tab: show what subject, which groups, which room
                "<font size='3'>" + "<br>" + group + "<br>" + room + "</font>";
            case VIEW_ROOM ->
                // Room tab: show what subject, teacher, which groups
                "<font size='3'>" + "<br>" + teacher + "<br>" + group + "</font>";
            default ->
                // Class/Master tab: show group, subject, teacher, room
                "<font size='3'>" + subject +  "<br>" + teacher + "<br>" + room + "</font>";
        };
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static int parseStartTotalMinutes(String time) {
        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(3, 5));
            return hour * 60 + minute;
        }
        catch (NumberFormatException ex) { return 0; }
    }

    private static String formatMinuteLabel(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    private List<ScheduleEntry> getActiveSchedule() {
        int weekIdx = weekSelector.getSelectedIndex();
        if (!weekSchedules.isEmpty() && weekIdx >= 0 && weekIdx < weekSchedules.size()) {
            return weekSchedules.get(weekIdx);
        }
        return DataStore.getSchedule();
    }

    private List<ScheduleEntry> getFilteredSchedule() {
        List<ScheduleEntry> activeSchedule = getActiveSchedule();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return activeSchedule;
        return activeSchedule.stream().filter(e ->
                e.getSubject().getName().toLowerCase(Locale.ROOT).contains(query)
                || e.getTeacher().getName().toLowerCase(Locale.ROOT).contains(query)
                || e.getRoom().getId().toLowerCase(Locale.ROOT).contains(query)
                || e.getGroup().getName().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    private void exportCurrentView() {
        List<ScheduleEntry> filtered = getFilteredSchedule();
        String view = String.valueOf(viewSelector.getSelectedItem());
        String week = String.valueOf(weekSelector.getSelectedItem());
        String query = searchField.getText().trim();
        List<ExportSection> sections = buildExportSections(filtered, view, query);
        if (filtered.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No timetable entries to export for the current view/filter.",
                    "Export Timetable", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (sections.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No timetable table is available to export yet.",
                    "Export Timetable", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.io.File downloadsDir = new java.io.File(System.getProperty("user.home"), "Downloads");
        JFileChooser fc = downloadsDir.isDirectory() ? new JFileChooser(downloadsDir) : new JFileChooser();
        fc.setDialogTitle("Export Timetable HTML");
        String safeView = view.replaceAll("[^a-zA-Z0-9._-]+", "_");
        String safeWeek = week.replaceAll("[^a-zA-Z0-9._-]+", "_");
        fc.setSelectedFile(new java.io.File("timetable_" + safeView + "_" + safeWeek + ".html"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File out = fc.getSelectedFile();
        String lower = out.getName().toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".html") && !lower.endsWith(".htm")) {
            out = new java.io.File(out.getAbsolutePath() + ".html");
        }

        try {
            exportHtml(out, sections);
            JOptionPane.showMessageDialog(this,
                    "Timetable exported to:\n" + out.getAbsolutePath(),
                    "Export Timetable", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Export failed: " + ex.getMessage(),
                    "Export Timetable", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportHtml(java.io.File out, List<ExportSection> sections) throws java.io.IOException {
        String view = String.valueOf(viewSelector.getSelectedItem());
        String week = String.valueOf(weekSelector.getSelectedItem());
        String title = "Timetable - " + escHtml(view) + " - " + escHtml(week);

        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(title).append("</h1>\n");

        for (ExportSection sec : sections) {
            int rowCount = sec.model.getRowCount();
            int colCount = sec.model.getColumnCount();
            double periodColWidth = 12.0;
            double dayColWidth = colCount <= 1 ? 100.0 : ((100.0 - periodColWidth) / (colCount - 1));

            body.append("<section class='sec'>\n")
                .append("<h2>").append(escHtml(sec.name)).append("</h2>\n")
                .append("<table class='tt'>\n")
                .append("<colgroup>");
            for (int c = 0; c < colCount; c++) {
                double width = (c == 0) ? periodColWidth : dayColWidth;
                body.append("<col style='width:")
                    .append(String.format(Locale.ROOT, "%.4f%%", width))
                    .append("'>");
            }
            body.append("</colgroup>\n<thead><tr>");

            for (int c = 0; c < colCount; c++) {
                body.append("<th>").append(escHtml(sec.model.getColumnName(c))).append("</th>");
            }
            body.append("</tr></thead>\n<tbody>\n");
            boolean[][] skipCell = new boolean[rowCount][colCount];

            for (int r = 0; r < rowCount; r++) {
                body.append("<tr>");
                for (int c = 0; c < colCount; c++) {
                    if (skipCell[r][c]) continue;
                    Object val = sec.model.getValueAt(r, c);
                    if (c == 0) {
                        body.append("<td class='time'>").append(escHtml(String.valueOf(val))).append("</td>");
                    } else if (BREAK.equals(val)) {
                        body.append("<td class='break'>Break / Interval</td>");
                    } else if (CONT.equals(val)) {
                        // Orphan continuation (defensive): keep a light filler cell.
                        body.append("<td class='cont'>&nbsp;</td>");
                    } else if (val == null) {
                        body.append("<td class='empty'>---</td>");
                    } else {
                        int span = 1;
                        int rr = r + 1;
                        while (rr < rowCount && CONT.equals(sec.model.getValueAt(rr, c))) {
                            skipCell[rr][c] = true;
                            span++;
                            rr++;
                        }

                        body.append("<td class='content'");
                        if (span > 1) body.append(" rowspan='").append(span).append("'");
                        body.append(">")
                            .append(normalizeCellHtml(val.toString()))
                            .append("</td>");
                    }
                }
                body.append("</tr>\n");
            }
            body.append("</tbody></table>\n</section>\n");
        }

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n")
            .append("<html><head><meta charset='utf-8'>")
            .append("<title>").append(title).append("</title>")
            .append("<style>")
            .append("body{font-family:Segoe UI,Arial,sans-serif;margin:24px;color:#1E293B;}")
            .append("h1{margin:0 0 10px 0;}")
            .append("h2{margin:18px 0 0 0;background:#3B82F6;color:#fff;padding:8px 10px;border-radius:6px 6px 0 0;font-size:14px;}")
            .append(".sec{margin:0 0 18px 0;border:1px solid #CBD5E1;border-radius:6px;overflow:hidden;}")
            .append(".tt{border-collapse:collapse;width:100%;table-layout:fixed;}")
            .append(".tt thead tr th,.tt tbody tr td{border:1px solid #CBD5E1;padding:6px 8px;text-align:left;font-size:12px;vertical-align:top;}")
            .append(".tt thead tr th{background:#E2E8F0;text-align:center;}")
            .append(".tt tbody tr td.time{background:#F1F5F9;font-weight:700;text-align:right;white-space:nowrap;}")
            .append(".tt tbody tr td.break{background:#FEF3C7;color:#92400E;font-style:italic;text-align:center;}")
            .append(".tt tbody tr td.cont{background:#EFF6FF;}")
            .append(".tt tbody tr td.content{background:#DBEAFE;overflow-wrap:anywhere;word-break:break-word;}")
            .append(".tt tbody tr td.empty{color:#94A3B8;text-align:center;}")
            .append("</style></head><body>\n")
            .append(body)
            .append("\n</body></html>\n");
        Files.writeString(out.toPath(), html.toString(), StandardCharsets.UTF_8);
    }

    private List<ExportSection> buildExportSections(List<ScheduleEntry> schedule, String view, String query) {
        List<ExportSection> out = new ArrayList<>();
        switch (view) {
            case VIEW_CLASS -> {
                for (model.StudentGroup g : DataStore.getGroups()) {
                    List<ScheduleEntry> entries = schedule.stream()
                            .filter(e -> e.getGroup().getName().equals(g.getName()))
                            .collect(Collectors.toList());
                    if (entries.isEmpty() && !query.isEmpty()) continue;
                    DefaultTableModel model = new DefaultTableModel() {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    buildGrid(model, entries, view);
                    if (model.getColumnCount() > 0) out.add(new ExportSection(g.getName(), model));
                }
            }
            case VIEW_TEACHER -> {
                for (model.Teacher t : DataStore.getTeachers()) {
                    List<ScheduleEntry> entries = schedule.stream()
                            .filter(e -> e.getTeacher().getName().equals(t.getName()))
                            .collect(Collectors.toList());
                    if (entries.isEmpty() && !query.isEmpty()) continue;
                    DefaultTableModel model = new DefaultTableModel() {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    buildGrid(model, entries, view);
                    if (model.getColumnCount() > 0) out.add(new ExportSection(t.getName(), model));
                }
            }
            case VIEW_ROOM -> {
                for (model.Room r : DataStore.getRooms()) {
                    List<ScheduleEntry> entries = schedule.stream()
                            .filter(e -> e.getRoom().getId().equals(r.getId()))
                            .collect(Collectors.toList());
                    if (entries.isEmpty() && !query.isEmpty()) continue;
                    DefaultTableModel model = new DefaultTableModel() {
                        @Override public boolean isCellEditable(int row, int col) { return false; }
                    };
                    buildGrid(model, entries, view);
                    if (model.getColumnCount() > 0) out.add(new ExportSection(r.getId(), model));
                }
            }
            default -> {
            }
        }
        return out;
    }

    private static String escHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String normalizeCellHtml(String raw) {
        String s = raw == null ? "" : raw;
        s = s.replaceFirst("(?i)^<html><body[^>]*>", "")
             .replaceFirst("(?i)^<html>", "")
             .replaceFirst("(?i)</body></html>$", "")
             .replaceFirst("(?i)</html>$", "");
        return s;
    }

    private static class ExportSection {
        private final String name;
        private final DefaultTableModel model;

        private ExportSection(String name, DefaultTableModel model) {
            this.name = name;
            this.model = model;
        }
    }

    // ── Cell renderer ─────────────────────────────────────────────────────────
    private static class GridCellRenderer extends DefaultTableCellRenderer {
        private static final Color BG_CONTENT  = new Color(0xDBEAFE);
        private static final Color BG_CONT_SEL = new Color(0xBFDBFE);
        private static final Color BG_TIME     = new Color(0xF1F5F9);
        private static final Color BG_BREAK    = new Color(0xFEF9C3);  // yellow-50
        private static final Color FG_BREAK    = new Color(0x92400E);  // amber-800
        private static final Color FG_EMPTY    = new Color(180, 180, 180);
        private static final Color GRID_COLOR  = new Color(180, 180, 180);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, null, sel, foc, row, col);
            lbl.setVerticalAlignment(SwingConstants.TOP);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            boolean isBreak = BREAK.equals(value);
            boolean isCont = CONT.equals(value);
            boolean isDayCol = col > 0;

            boolean aboveIsCont = row > 0 && isDayCol && CONT.equals(table.getValueAt(row - 1, col));
            boolean belowIsCont = row + 1 < table.getRowCount() && isDayCol && CONT.equals(table.getValueAt(row + 1, col));

            if (isBreak) {
                // Break / interval row
                if (col == 0) {
                    lbl.setText(value == null ? "Break" : value.toString());
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
                } else {
                    lbl.setText("<html><center><i>Break / Interval</i></center></html>");
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 10f));
                }
                lbl.setBackground(BG_BREAK);
                lbl.setForeground(FG_BREAK);

            } else if (isCont) {
                // Continuation row - hide content, blend with row above
                lbl.setText("");
                lbl.setBackground(sel ? BG_CONT_SEL : BG_CONTENT);
                lbl.setForeground(Color.BLACK);
                // Reduce top padding to create seamless vertical merge.
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 6));

            } else if (col == 0) {
                lbl.setText(value == null ? "" : value.toString());
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
                lbl.setBackground(BG_TIME);
                lbl.setForeground(Color.DARK_GRAY);

            } else if (value == null) {
                lbl.setText("---");
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setVerticalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
                lbl.setBackground(sel ? new Color(220, 220, 220) : Color.WHITE);
                lbl.setForeground(FG_EMPTY);

            } else {
                lbl.setText(value.toString());
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
                lbl.setBackground(sel ? new Color(150, 195, 245) : BG_CONTENT);
                lbl.setForeground(Color.BLACK);
            }

            // Draw custom per-cell borders. This allows hiding the line between
            // a content cell and its continuation cell to mimic merged rows.
            int top = 1, left = 1, bottom = 1, right = 1;
            if (isDayCol && !isBreak) {
                if (isCont || aboveIsCont) top = 0;
                if (belowIsCont) bottom = 0;
            }
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(top, left, bottom, right, GRID_COLOR),
                    lbl.getBorder()));

            return lbl;
        }
    }

    // ── Bold header renderer ──────────────────────────────────────────────────
    private static class BoldHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer wrapped;
        BoldHeaderRenderer(TableCellRenderer wrapped) { this.wrapped = wrapped; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean foc, int row, int col) {
            Component c = wrapped.getTableCellRendererComponent(table, value, sel, foc, row, col);
            if (c instanceof JLabel lbl) {
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return c;
        }
    }
}
