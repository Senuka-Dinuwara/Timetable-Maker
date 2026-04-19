package ui;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import util.CsvImporter;
import util.CsvImporter.ImportResult;

/**
 * Import tab — lets the user either:
 *   (a) pick a CSV file from disk, or
 *   (b) paste/type CSV text directly into the text area.
 *
 * CSV formats (header row always required):
 *   Subjects  →  Name, WeeklyHours, Type [, PreferredDay [, AllGroups [, SessionDuration]]]
 *   Teachers  →  Name, Subjects                   (Subjects semicolon-separated)
 *   Rooms     →  ID, Capacity, Type
 *   Groups    →  Name, Size, Subjects             (Subjects semicolon-separated)
 */
public class ImportPanel extends JPanel {

    private final JTextArea logArea;
    private final JTextArea csvTextArea;
    private final JComboBox<String> csvTypeBox;

    private static final String[] CSV_TYPES = {"Subjects", "Teachers", "Rooms", "Groups"};

    public ImportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Instruction label ───────────────────────────────────────────────
        JLabel hint = new JLabel("<html>"
                + "<b>Import data from a CSV file</b> (top section) "
                + "<b>or paste CSV text directly</b> (bottom section).<br><br>"
                + "<b>Subjects:</b> &nbsp; Name, WeeklyHours, Type &nbsp;<i>[, PreferredDay [, AllGroups [, SessionDuration]]]</i><br>"
                + "<b>Teachers:</b> &nbsp; Name, Subjects &nbsp;<i>(semicolon-separated)</i><br>"
                + "<b>Rooms:</b> &nbsp;&nbsp;&nbsp;&nbsp; ID, Capacity, Type<br>"
                + "<b>Groups:</b> &nbsp;&nbsp;&nbsp;&nbsp; Name, Size, Subjects &nbsp;<i>(semicolon-separated)</i><br><br>"
                + "<i>Always import Subjects first — Teachers and Groups reference them by name.</i>"
                + "</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // ── File-picker buttons ─────────────────────────────────────────────
        JPanel filePanel = new JPanel(new GridLayout(2, 2, 10, 6));
        filePanel.setBorder(BorderFactory.createTitledBorder("Import from File"));

        JButton subjectsBtn = makeBtn("Subjects CSV…");
        JButton teachersBtn = makeBtn("Teachers CSV…");
        JButton roomsBtn    = makeBtn("Rooms CSV…");
        JButton groupsBtn   = makeBtn("Groups CSV…");

        filePanel.add(subjectsBtn);
        filePanel.add(teachersBtn);
        filePanel.add(roomsBtn);
        filePanel.add(groupsBtn);

        // ── Paste / type CSV ─────────────────────────────────────────────────
        csvTextArea = new JTextArea(8, 55);
        csvTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        csvTextArea.setToolTipText("Paste or type CSV data here (include the header row)");
        JScrollPane csvScroll = new JScrollPane(csvTextArea);

        csvTypeBox = new JComboBox<>(CSV_TYPES);
        JButton importTextBtn = new JButton("Import from Text");
        JButton clearTextBtn  = new JButton("Clear");

        JPanel textControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        textControls.add(new JLabel("Type:"));
        textControls.add(csvTypeBox);
        textControls.add(importTextBtn);
        textControls.add(clearTextBtn);

        JPanel pastePanel = new JPanel(new BorderLayout(4, 4));
        pastePanel.setBorder(BorderFactory.createTitledBorder("Import by Pasting CSV Text"));
        pastePanel.add(csvScroll, BorderLayout.CENTER);
        pastePanel.add(textControls, BorderLayout.SOUTH);

        // ── Log area ─────────────────────────────────────────────────────────
        logArea = new JTextArea(7, 55);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Import Log"));

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.addActionListener(e -> logArea.setText(""));

        JPanel logPanel = new JPanel(new BorderLayout(4, 4));
        logPanel.add(logScroll, BorderLayout.CENTER);
        logPanel.add(clearLogBtn, BorderLayout.SOUTH);

        // ── Layout ────────────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(hint,      BorderLayout.NORTH);
        top.add(filePanel, BorderLayout.CENTER);
        top.add(pastePanel, BorderLayout.SOUTH);

        // Use a split pane so both halves stay visible when the window is resized
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, logPanel);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // ── File-import actions ────────────────────────────────────────────────
        subjectsBtn.addActionListener(e -> {
            File f = chooseCsv("subjects");
            if (f != null) log("Subjects: " + f.getName(), CsvImporter.importSubjects(f));
        });
        teachersBtn.addActionListener(e -> {
            File f = chooseCsv("teachers");
            if (f != null) log("Teachers: " + f.getName(), CsvImporter.importTeachers(f));
        });
        roomsBtn.addActionListener(e -> {
            File f = chooseCsv("rooms");
            if (f != null) log("Rooms: " + f.getName(), CsvImporter.importRooms(f));
        });
        groupsBtn.addActionListener(e -> {
            File f = chooseCsv("groups");
            if (f != null) log("Groups: " + f.getName(), CsvImporter.importGroups(f));
        });

        // ── Text-import actions ────────────────────────────────────────────────
        importTextBtn.addActionListener(e -> {
            String text = csvTextArea.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please paste CSV data first.",
                        "No Input", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String type = (String) csvTypeBox.getSelectedItem();
            ImportResult r = switch (type) {
                case "Subjects" -> CsvImporter.importSubjectsFromText(text);
                case "Teachers" -> CsvImporter.importTeachersFromText(text);
                case "Rooms"    -> CsvImporter.importRoomsFromText(text);
                default         -> CsvImporter.importGroupsFromText(text);
            };
            log(type + " (from text): imported " + r.imported, r);
        });

        clearTextBtn.addActionListener(e -> csvTextArea.setText(""));
    }

    private JButton makeBtn(String label) {
        JButton btn = new JButton(label);
        btn.setPreferredSize(new Dimension(200, 38));
        return btn;
    }

    private File chooseCsv(String description) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select " + description + " CSV file");
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        fc.setAcceptAllFileFilterUsed(false);
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile() : null;
    }

    private void log(String summary, ImportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("── ").append(summary).append('\n');
        for (String err : r.errors) sb.append("   WARN: ").append(err).append('\n');
        if (r.errors.isEmpty()) sb.append("   OK — no warnings.\n");
        logArea.append(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
