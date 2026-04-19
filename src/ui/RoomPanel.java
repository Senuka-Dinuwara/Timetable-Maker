package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import model.Room;
import util.CsvImporter;
import util.DataStore;

public class RoomPanel extends JPanel {

    private JTextField idField;
    private JTextField capacityField;
    private JComboBox<String> typeBox;
    private JComboBox<String> assignmentBox;
    private JLabel assignmentLabel;
    private JButton addBtn;
    private int editingRow = -1;
    private DefaultTableModel tableModel;
    private JTable table;

    public RoomPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4), "Add / Edit Room",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        AppTheme.FONT_TITLE, AppTheme.TEXT2)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        idField = new JTextField(15);
        capacityField = new JTextField(15);
        typeBox = new JComboBox<>();
        typeBox.setToolTipText("Select room type");
        loadRoomTypeOptions();
        assignmentBox = new JComboBox<>();
        assignmentLabel = new JLabel("Assignment:");
        addBtn = new JButton("Add Room");
        AppTheme.stylePrimary(addBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        AppTheme.styleDanger(deleteBtn);
        JButton clearBtn = new JButton("New / Clear");
        AppTheme.styleSecondary(clearBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; form.add(new JLabel("Room Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; form.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(capacityField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(typeBox, gbc);
        JButton blockedBtn = new JButton("Edit Blocked Times\u2026");
        AppTheme.styleSecondary(blockedBtn);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0; form.add(assignmentLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(assignmentBox, gbc);
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; form.add(new JLabel("Blocked Times:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(blockedBtn, gbc);

        typeBox.addActionListener(e -> refreshAssignmentOptions(null, null));
        refreshAssignmentOptions(null, null);

        // --- Table ---
        tableModel = new DefaultTableModel(new String[]{"Room Name", "Capacity", "Type", "Assignment", "Blocked Times"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        JScrollPane scroll = AppTheme.modernScroll(table);

        // Right-click on "Blocked Times" column header to clear all blocked slots
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int col = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (col != 4) return; // only "Blocked Times" column
                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem("Clear Blocked Times column");
                item.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(RoomPanel.this,
                            "Remove all blocked times from every room?",
                            "Clear Blocked Times", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                    for (model.Room r : util.DataStore.getRooms()) r.setBlockedSlots(new java.util.ArrayList<>());
                    refreshTable();
                });
                popup.add(item);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { mousePressed(e); }
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

                final String ROOM_FORMAT = """
                        Name, Capacity, Type
                            Type     : Classroom | Lecture Hall | Lab
                            Example  : R101, 30, Classroom""";

        importFileBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            CsvImporter.ImportResult r = CsvImporter.importRooms(fc.getSelectedFile());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, ROOM_FORMAT);
            refreshTable();
        });
        importPasteBtn.addActionListener(ev -> {
                String hint = """
                    # Format: Name, Capacity, Type
                    #   Type    : Classroom | Lecture Hall | Lab
                    #   Example : R101, 30, Classroom
                    # Lines starting with '#' are ignored. First data row = header (skipped).
                    Name, Capacity, Type
                    """;
            JTextArea ta = new JTextArea(hint, 12, 55);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
                    "Paste Rooms CSV",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION || ta.getText().isBlank()) return;
            CsvImporter.ImportResult r = CsvImporter.importRoomsFromText(ta.getText());
            importStatus.setText("Imported: " + r.imported + (r.errors.isEmpty() ? "" : "  (" + r.errors.size() + " warn)"));
            if (!r.errors.isEmpty()) showImportErrors(r.errors, ROOM_FORMAT);
            refreshTable();
        });

        exportBtn.addActionListener(ev -> {
            if (DataStore.getRooms().isEmpty()) { importStatus.setText("Nothing to export."); return; }
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            fc.setDialogTitle("Export Rooms CSV");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".csv")) f = new java.io.File(f.getAbsolutePath() + ".csv");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                pw.println("ID,Capacity,Type");
                for (Room r : DataStore.getRooms()) {
                    pw.printf("\"%s\",%d,%s%n", r.getId().replace("\"", "\"\""), r.getCapacity(), r.getType());
                }
                importStatus.setText("Exported " + DataStore.getRooms().size() + " rooms.");
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
            Room r = DataStore.getRooms().get(row);
            idField.setText(r.getId());
            capacityField.setText(String.valueOf(r.getCapacity()));
            typeBox.setSelectedItem(r.getType());
            refreshAssignmentOptions(r.getAssignedGroup(), r.getAssignedSubject());
            addBtn.setText("Update Room");
        });

        JButton updateBlockedBtn = new JButton("Update from Schedule");
        AppTheme.styleSecondary(updateBlockedBtn);

        JPanel actionsCol = new JPanel(new GridLayout(4, 1, 0, 8));
        actionsCol.setBackground(AppTheme.BG);
        actionsCol.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        actionsCol.add(AppTheme.makeActionCard(
                "\u2795", "Add / Save",
                "Save the current form as a new room.",
                new Color(0x2563EB), addBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDDD1", "Delete Selected",
                "Remove the selected room(s) from the list.",
                new Color(0xDC2626), deleteBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83E\uDDF9", "New / Clear",
                "Reset the form to add a new entry.",
                new Color(0x64748B), clearBtn));
        actionsCol.add(AppTheme.makeActionCard(
                "\uD83D\uDD04", "Update Blocked Times",
                "Mark all scheduled room slots as blocked.",
                new Color(0x0891B2), updateBlockedBtn));

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

        updateBlockedBtn.addActionListener(e -> {
            java.util.List<model.ScheduleEntry> entries = util.DataStore.getSchedule();
            if (entries.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No timetable generated yet.", "Nothing to Update", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // Collect slots per room
            java.util.Map<model.Room, java.util.Set<String>> roomSlots = new java.util.LinkedHashMap<>();
            for (model.ScheduleEntry se : entries) {
                model.Room r = se.getRoom();
                if (r == null) continue;
                roomSlots.computeIfAbsent(r, k -> new java.util.LinkedHashSet<>());
                String day = se.getSlot().getDay();
                String time = se.getSlot().getTime(); // "HH:mm - HH:mm"
                String entry = day + " " + time;
                roomSlots.get(r).add(entry);
            }
            int added = 0;
            for (java.util.Map.Entry<model.Room, java.util.Set<String>> me : roomSlots.entrySet()) {
                model.Room r = me.getKey();
                java.util.List<String> blocked = new java.util.ArrayList<>(r.getBlockedSlots());
                for (String slot : me.getValue()) {
                    if (!blocked.contains(slot)) { blocked.add(slot); added++; }
                }
                r.setBlockedSlots(blocked);
            }
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    added + " new blocked slot" + (added == 1 ? "" : "s") + " added across "
                            + roomSlots.size() + " room" + (roomSlots.size() == 1 ? "" : "s") + ".",
                    "Blocked Times Updated", JOptionPane.INFORMATION_MESSAGE);
        });

        addBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            String capText = capacityField.getText().trim();
            if (id.isEmpty() || capText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int capacity;
            try {
                capacity = Integer.parseInt(capText);
                if (capacity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capacity must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String type = (String) typeBox.getSelectedItem();
            String selectedAssignment = (String) assignmentBox.getSelectedItem();
            String assignedGroup = null;
            String assignedSubject = null;
            if (type != null && DataStore.isRoomTypeAssignableToGroups(type)) {
                assignedGroup = normalizeAssignment(selectedAssignment);
            } else if (type != null && DataStore.isRoomTypeAssignableToSubjects(type)) {
                assignedSubject = normalizeAssignment(selectedAssignment);
            }
            if (editingRow >= 0) {
                Room existing = DataStore.getRooms().get(editingRow);
                existing.setId(id);
                existing.setCapacity(capacity);
                existing.setType(type);
                existing.setAssignedGroup(assignedGroup);
                existing.setAssignedSubject(assignedSubject);
                tableModel.setValueAt(id, editingRow, 0);
                tableModel.setValueAt(capacity, editingRow, 1);
                tableModel.setValueAt(type, editingRow, 2);
                tableModel.setValueAt(assignmentDisplay(existing), editingRow, 3);
                tableModel.setValueAt(blockedSlotsDisplay(existing), editingRow, 4);
            } else {
                Room room = new Room(id, capacity, type);
                room.setAssignedGroup(assignedGroup);
                room.setAssignedSubject(assignedSubject);
                DataStore.addRoom(room);
                tableModel.addRow(new Object[]{id, capacity, type, assignmentDisplay(room), ""});
            }
            clearForm();
        });

        blockedBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a room first.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Room r = DataStore.getRooms().get(row);
            showBlockedTimesDialog(r);
            tableModel.setValueAt(blockedSlotsDisplay(r), row, 4);
        });

        deleteBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Select one or more rooms to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (int i = rows.length - 1; i >= 0; i--) {
                DataStore.removeRoom(DataStore.getRooms().get(rows[i]));
                tableModel.removeRow(rows[i]);
            }
            clearForm();
        });
    }

    private String blockedSlotsDisplay(model.Room r) {
        java.util.List<String> s = r.getBlockedSlots();
        if (s == null || s.isEmpty()) return "";
        return s.size() + " blocked: " + String.join(", ", s);
    }

    private String assignmentDisplay(Room r) {
        String g = r.getAssignedGroup();
        if (g != null && !g.isBlank()) return "Group: " + g;
        String s = r.getAssignedSubject();
        if (s != null && !s.isBlank()) return "Subject: " + s;
        return "Disabled";
    }

    private String normalizeAssignment(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty() || "<None>".equals(v) || "Disabled for this type".equals(v)) return null;
        return v;
    }

    private void refreshAssignmentOptions(String preferredGroup, String preferredSubject) {
        String type = (String) typeBox.getSelectedItem();
        assignmentBox.removeAllItems();

        if (type != null && DataStore.isRoomTypeAssignableToGroups(type)) {
            assignmentLabel.setText("Assigned Group:");
            assignmentBox.setEnabled(true);
            assignmentBox.addItem("<None>");
            for (model.StudentGroup g : DataStore.getGroups()) assignmentBox.addItem(g.getName());
            if (preferredGroup != null && !preferredGroup.isBlank()) {
                assignmentBox.setSelectedItem(preferredGroup);
            } else {
                assignmentBox.setSelectedIndex(0);
            }
            return;
        }

        if (type != null && DataStore.isRoomTypeAssignableToSubjects(type)) {
            assignmentLabel.setText("Assigned Subject:");
            assignmentBox.setEnabled(true);
            assignmentBox.addItem("<None>");
            java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
            for (model.Subject s : DataStore.getSubjects()) {
                if (s.getName() != null && !s.getName().isBlank()) names.add(s.getName().trim());
            }
            for (String n : names) assignmentBox.addItem(n);
            if (preferredSubject != null && !preferredSubject.isBlank()) {
                assignmentBox.setSelectedItem(preferredSubject);
            } else {
                assignmentBox.setSelectedIndex(0);
            }
            return;
        }

        assignmentLabel.setText("Assignment:");
        assignmentBox.setEnabled(false);
        assignmentBox.addItem("Disabled for this type");
        assignmentBox.setSelectedIndex(0);
    }

    private void showBlockedTimesDialog(model.Room room) {
        // ── Left: list of currently blocked ranges ─────────────────────
        javax.swing.DefaultListModel<String> listModel = new javax.swing.DefaultListModel<>();
        room.getBlockedSlots().forEach(listModel::addElement);
        javax.swing.JList<String> blockedList = new javax.swing.JList<>(listModel);
        blockedList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        blockedList.setFont(AppTheme.FONT_BODY);
        JScrollPane listScroll = new JScrollPane(blockedList);
        listScroll.setPreferredSize(new Dimension(240, 220));
        listScroll.setBorder(BorderFactory.createTitledBorder("Blocked ranges"));

        JButton removeBtn = new JButton("Remove Selected");
        AppTheme.styleDanger(removeBtn);
        removeBtn.addActionListener(ev -> {
            int[] sel = blockedList.getSelectedIndices();
            for (int i = sel.length - 1; i >= 0; i--) listModel.remove(sel[i]);
        });

        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.setOpaque(false);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(removeBtn, BorderLayout.SOUTH);

        // ── Right: day + 24h spinners for start/end ───────────────────
        java.util.List<model.TimeSlot> allSlots = util.DataStore.getTimeSlots();
        java.util.List<String> days = allSlots.stream()
                .map(model.TimeSlot::getDay).distinct().sorted()
                .collect(java.util.stream.Collectors.toList());
        if (days.isEmpty()) days = java.util.Arrays.asList(
                "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday");

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createTitledBorder("Add a range"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> dayCombo = new JComboBox<>(days.toArray(String[]::new));

        // Hour:Minute spinners
        JSpinner startHourSpin = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        JSpinner startMinSpin  = new JSpinner(new SpinnerNumberModel(0, 0, 59, 5));
        JSpinner endHourSpin   = new JSpinner(new SpinnerNumberModel(10, 0, 23, 1));
        JSpinner endMinSpin    = new JSpinner(new SpinnerNumberModel(0, 0, 59, 5));
        for (JSpinner sp : new JSpinner[]{startHourSpin, startMinSpin, endHourSpin, endMinSpin}) {
            ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(2);
        }

        JPanel startRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        startRow.setOpaque(false);
        startRow.add(startHourSpin); startRow.add(new JLabel(":")); startRow.add(startMinSpin);

        JPanel endRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        endRow.setOpaque(false);
        endRow.add(endHourSpin); endRow.add(new JLabel(":")); endRow.add(endMinSpin);

        JButton addBtn2 = new JButton("Add \u25b6");
        AppTheme.stylePrimary(addBtn2);

        JLabel errLbl = new JLabel(" ");
        errLbl.setFont(AppTheme.FONT_SMALL);
        errLbl.setForeground(new java.awt.Color(0xDC2626));

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 1; rightPanel.add(new JLabel("Day:"), gc);
        gc.gridx = 1; rightPanel.add(dayCombo, gc);
        gc.gridx = 0; gc.gridy = 1; rightPanel.add(new JLabel("Start:"), gc);
        gc.gridx = 1; rightPanel.add(startRow, gc);
        gc.gridx = 0; gc.gridy = 2; rightPanel.add(new JLabel("End:"), gc);
        gc.gridx = 1; rightPanel.add(endRow, gc);
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; rightPanel.add(addBtn2, gc);
        gc.gridy = 4; rightPanel.add(errLbl, gc);

        addBtn2.addActionListener(ev -> {
            String day = (String) dayCombo.getSelectedItem();
            int sh = (int) startHourSpin.getValue(), sm = (int) startMinSpin.getValue();
            int eh = (int) endHourSpin.getValue(),   em = (int) endMinSpin.getValue();
            int startMins = sh * 60 + sm, endMins = eh * 60 + em;
            if (endMins <= startMins) { errLbl.setText("End must be after start."); return; }
            errLbl.setText(" ");
            String entry = day + " " + String.format("%02d:%02d", sh, sm)
                    + " - " + String.format("%02d:%02d", eh, em);
            if (!listModel.contains(entry)) listModel.addElement(entry);
        });

        // ── Combine ───────────────────────────────────────────────────
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(leftPanel,  BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        int ok = JOptionPane.showConfirmDialog(this, panel,
                "Blocked Times \u2014 " + room.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) result.add(listModel.getElementAt(i));
        room.setBlockedSlots(result);
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
        idField.setText("");
        capacityField.setText("");
        typeBox.setSelectedItem("Classroom");
        refreshAssignmentOptions(null, null);
        addBtn.setText("Add Room");
    }

    public void refreshTypeOptions() {
        loadRoomTypeOptions();
        refreshAssignmentOptions(null, null);
    }

    private void loadRoomTypeOptions() {
        Object current = typeBox.getSelectedItem();
        typeBox.removeAllItems();
        for (String t : DataStore.getRoomTypeOptions()) typeBox.addItem(t);
        if (current != null) {
            String c = current.toString();
            boolean found = false;
            for (int i = 0; i < typeBox.getItemCount(); i++) {
                if (c.equalsIgnoreCase(typeBox.getItemAt(i))) {
                    typeBox.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found && typeBox.getItemCount() > 0) typeBox.setSelectedIndex(0);
        } else if (typeBox.getItemCount() > 0) {
            typeBox.setSelectedIndex(0);
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (model.Room r : DataStore.getRooms()) {
            tableModel.addRow(new Object[]{r.getId(), r.getCapacity(), r.getType(), assignmentDisplay(r), blockedSlotsDisplay(r)});
        }
    }
}
