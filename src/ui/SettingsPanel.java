package ui;

import java.awt.*;
import javax.swing.*;
import util.DataStore;

public class SettingsPanel extends JPanel {

    private JCheckBox monBox, tueBox, wedBox, thuBox, friBox, satBox;
    private JSpinner startSpinner, endSpinner;

    public SettingsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Days ---
        JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        daysPanel.setBorder(BorderFactory.createTitledBorder("Working Days"));
        monBox = new JCheckBox("Mon", true);
        tueBox = new JCheckBox("Tue", true);
        wedBox = new JCheckBox("Wed", true);
        thuBox = new JCheckBox("Thu", true);
        friBox = new JCheckBox("Fri", true);
        satBox = new JCheckBox("Sat", false);
        daysPanel.add(monBox); daysPanel.add(tueBox); daysPanel.add(wedBox);
        daysPanel.add(thuBox); daysPanel.add(friBox); daysPanel.add(satBox);

        // --- Time Range ---
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.setBorder(BorderFactory.createTitledBorder("Daily Time Range"));
        startSpinner = new JSpinner(new SpinnerNumberModel(8, 6, 22, 1));
        endSpinner = new JSpinner(new SpinnerNumberModel(17, 7, 23, 1));
        timePanel.add(new JLabel("Start Hour:"));
        timePanel.add(startSpinner);
        timePanel.add(new JLabel("  End Hour:"));
        timePanel.add(endSpinner);

        JButton applyBtn = new JButton("Apply Settings");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; add(daysPanel, gbc);
        gbc.gridy = 1; add(timePanel, gbc);
        gbc.gridy = 2; gbc.gridwidth = 1; add(applyBtn, gbc);

        applyBtn.addActionListener(e -> {
            int start = (int) startSpinner.getValue();
            int end = (int) endSpinner.getValue();
            if (start >= end) {
                JOptionPane.showMessageDialog(this,
                        "Start hour must be before end hour.", "Settings Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.util.List<String> days = new java.util.ArrayList<>();
            if (monBox.isSelected()) days.add("Monday");
            if (tueBox.isSelected()) days.add("Tuesday");
            if (wedBox.isSelected()) days.add("Wednesday");
            if (thuBox.isSelected()) days.add("Thursday");
            if (friBox.isSelected()) days.add("Friday");
            if (satBox.isSelected()) days.add("Saturday");
            if (days.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Select at least one working day.", "Settings Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataStore.generateTimeSlots(days.toArray(String[]::new), start, end);
            JOptionPane.showMessageDialog(this,
                    DataStore.getTimeSlots().size() + " time slots generated.", "Settings Applied", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
