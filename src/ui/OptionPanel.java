package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import util.DataStore;

public class OptionPanel extends JPanel {

    private final SubjectPanel subjectPanel;
    private final RoomPanel roomPanel;

    private final DefaultListModel<String> subjectTypeModel = new DefaultListModel<>();
    private final DefaultListModel<String> roomTypeModel = new DefaultListModel<>();
    private java.util.Set<String> multiGroupTypes = new java.util.LinkedHashSet<>();
    private JList<String> subjectTypeList;
    private JList<String> roomTypeList;

    public OptionPanel(SubjectPanel subjectPanel, RoomPanel roomPanel) {
        this.subjectPanel = subjectPanel;
        this.roomPanel = roomPanel;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setBackground(AppTheme.BG);

        JLabel title = new JLabel("Options", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppTheme.TEXT);

        JPanel body = new JPanel(new GridLayout(1, 2, 10, 0));
        body.setOpaque(false);
        body.add(buildSubjectTypesCard());
        body.add(buildRoomTypesCard());

        JLabel hint = new JLabel("Manage reusable type lists used in Subjects and Rooms forms.");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.TEXT2);

        add(title, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);

        loadModelsFromStore();
    }

    private JPanel buildSubjectTypesCard() {
        subjectTypeList = new JList<>(subjectTypeModel);
        subjectTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectTypeList.setCellRenderer(new SubjectTypeRenderer());
        subjectTypeList.setFixedCellHeight(44);
        subjectTypeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleMultiGroupStatus(subjectTypeList, e.getPoint());
            }
        });
        subjectTypeList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int idx = subjectTypeList.getSelectedIndex();
                    if (idx >= 0) {
                        String type = subjectTypeModel.get(idx);
                        if (multiGroupTypes.contains(type)) {
                            multiGroupTypes.remove(type);
                        } else {
                            multiGroupTypes.add(type);
                        }
                        DataStore.setMultiGroupSubjectTypes(multiGroupTypes);
                        subjectTypeList.repaint();
                    }
                    e.consume();
                }
            }
        });

        JScrollPane sp = AppTheme.modernScroll(subjectTypeList);
        sp.setPreferredSize(new Dimension(340, 280));

        JTextField addField = new JTextField(14);
        JButton addBtn = new JButton("Add");
        AppTheme.stylePrimary(addBtn);
        JButton clearBtn = new JButton("Clear");
        AppTheme.styleSecondary(clearBtn);
        JButton removeBtn = new JButton("Remove");
        AppTheme.styleDanger(removeBtn);
        JButton resetBtn = new JButton("Reset Defaults");
        AppTheme.styleSecondary(resetBtn);

        subjectTypeList.addListSelectionListener(e -> {
            String sel = subjectTypeList.getSelectedValue();
            if (sel != null) {
                addField.setText(sel);
                addBtn.setText("Update");
            } else {
                addField.setText("");
                addBtn.setText("Add");
            }
        });

        addBtn.addActionListener(e -> {
            String t = addField.getText().trim();
            if (t.isEmpty()) return;
            
            String sel = subjectTypeList.getSelectedValue();
            if (sel != null && !sel.equals(t)) {
                // Update mode
                DataStore.renameSubjectTypeOption(sel, t);
            } else if (sel == null) {
                // Add mode
                DataStore.addSubjectTypeOption(t);
            }
            addField.setText("");
            reloadFromStore();
            subjectPanel.refreshTypeOptions();
        });

        clearBtn.addActionListener(e -> {
            addField.setText("");
            subjectTypeList.clearSelection();
        });

        removeBtn.addActionListener(e -> {
            String sel = subjectTypeList.getSelectedValue();
            if (sel == null) return;
            DataStore.removeSubjectTypeOption(sel);
            multiGroupTypes.remove(sel);
            DataStore.setMultiGroupSubjectTypes(multiGroupTypes);
            reloadFromStore();
            subjectPanel.refreshTypeOptions();
        });

        resetBtn.addActionListener(e -> {
            DataStore.resetSubjectTypeOptions();
            DataStore.resetMultiGroupSubjectTypes();
            reloadFromStore();
            subjectPanel.refreshTypeOptions();
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        top.add(new JLabel("Add Type:"));
        top.add(addField);
        top.add(addBtn);
        top.add(clearBtn);

        JLabel desc = new JLabel("Define subject categories and whether each runs in one shared slot for all groups.");
        desc.setFont(AppTheme.FONT_SMALL);
        desc.setForeground(AppTheme.TEXT2);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setOpaque(false);

        JLabel sharedBadge = new JLabel("Shared");
        sharedBadge.setOpaque(true);
        sharedBadge.setFont(AppTheme.FONT_SMALL);
        sharedBadge.setForeground(new Color(0x166534));
        sharedBadge.setBackground(new Color(0xDCFCE7));
        sharedBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel separateBadge = new JLabel("Separate");
        separateBadge.setOpaque(true);
        separateBadge.setFont(AppTheme.FONT_SMALL);
        separateBadge.setForeground(new Color(0x1E40AF));
        separateBadge.setBackground(new Color(0xDBEAFE));
        separateBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel legendHint = new JLabel("Switch rule: Shared = all groups same time, Separate = each group different slot");
        legendHint.setFont(AppTheme.FONT_SMALL);
        legendHint.setForeground(AppTheme.TEXT2);

        legend.add(sharedBadge);
        legend.add(separateBadge);
        legend.add(legendHint);

        JPanel card = AppTheme.cardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Subject Types"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        
        JPanel topSection = new JPanel(new BorderLayout(0, 6));
        topSection.setOpaque(false);
        topSection.add(top, BorderLayout.NORTH);
        topSection.add(desc, BorderLayout.CENTER);
        topSection.add(legend, BorderLayout.SOUTH);
        
        card.add(topSection, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        south.setOpaque(false);
        south.add(removeBtn);
        south.add(resetBtn);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private void toggleMultiGroupStatus(JList<String> list, Point p) {
        int idx = list.locationToIndex(p);
        if (idx < 0) return;
        Rectangle bounds = list.getCellBounds(idx, idx);
        if (!bounds.contains(p)) return;
        list.setSelectedIndex(idx);
        String type = subjectTypeModel.get(idx);
        if (multiGroupTypes.contains(type)) {
            multiGroupTypes.remove(type);
        } else {
            multiGroupTypes.add(type);
        }
        DataStore.setMultiGroupSubjectTypes(multiGroupTypes);
        list.repaint();
    }

    private class SubjectTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);
            
            if (isSelected) {
                panel.setBackground(AppTheme.SEL_BG);
                panel.setForeground(AppTheme.SEL_FG);
            } else {
                panel.setBackground(AppTheme.SURFACE);
                panel.setForeground(AppTheme.TEXT);
            }

            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));

            String type = (String) value;
            boolean isMultiGroup = multiGroupTypes.contains(type);

            JLabel typeLabel = new JLabel(type);
            typeLabel.setForeground(panel.getForeground());
            typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

            JLabel statusBadge = new JLabel(isMultiGroup ? "Shared" : "Separate");
            statusBadge.setOpaque(true);
            statusBadge.setFont(AppTheme.FONT_SMALL);
            statusBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            if (isMultiGroup) {
                statusBadge.setForeground(new Color(0x166534));
                statusBadge.setBackground(new Color(0xDCFCE7));
            } else {
                statusBadge.setForeground(new Color(0x1E40AF));
                statusBadge.setBackground(new Color(0xDBEAFE));
            }

            panel.add(typeLabel, BorderLayout.CENTER);
            panel.add(statusBadge, BorderLayout.EAST);
            
            return panel;
        }
    }

    private JPanel buildRoomTypesCard() {
        roomTypeList = new JList<>(roomTypeModel);
        roomTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTypeList.setCellRenderer(new RoomTypeRenderer());
        roomTypeList.setFixedCellHeight(44);
        roomTypeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cycleRoomTypeMode(roomTypeList, e.getPoint());
            }
        });
        roomTypeList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
                int idx = roomTypeList.getSelectedIndex();
                if (idx < 0) return;
                String type = roomTypeModel.get(idx);
                cycleRoomTypeMode(type);
                roomTypeList.repaint();
                e.consume();
            }
        });
        JScrollPane sp = AppTheme.modernScroll(roomTypeList);
        sp.setPreferredSize(new Dimension(340, 280));

        JTextField addField = new JTextField(14);
        JButton addBtn = new JButton("Add");
        AppTheme.stylePrimary(addBtn);
        JButton clearBtn = new JButton("Clear");
        AppTheme.styleSecondary(clearBtn);
        JButton removeBtn = new JButton("Remove");
        AppTheme.styleDanger(removeBtn);
        JButton resetBtn = new JButton("Reset Defaults");
        AppTheme.styleSecondary(resetBtn);

        roomTypeList.addListSelectionListener(e -> {
            String sel = roomTypeList.getSelectedValue();
            if (sel != null) {
                addField.setText(sel);
                addBtn.setText("Update");
            } else {
                addField.setText("");
                addBtn.setText("Add");
            }
        });

        addBtn.addActionListener(e -> {
            String t = addField.getText().trim();
            if (t.isEmpty()) return;
            
            String sel = roomTypeList.getSelectedValue();
            if (sel != null && !sel.equals(t)) {
                // Update mode
                DataStore.renameRoomTypeOption(sel, t);
            } else if (sel == null) {
                // Add mode
                DataStore.addRoomTypeOption(t);
            }
            addField.setText("");
            reloadFromStore();
            roomPanel.refreshTypeOptions();
        });

        clearBtn.addActionListener(e -> {
            addField.setText("");
            roomTypeList.clearSelection();
        });

        removeBtn.addActionListener(e -> {
            String sel = roomTypeList.getSelectedValue();
            if (sel == null) return;
            DataStore.removeRoomTypeOption(sel);
            reloadFromStore();
            roomPanel.refreshTypeOptions();
        });

        resetBtn.addActionListener(e -> {
            DataStore.resetRoomTypeOptions();
            reloadFromStore();
            roomPanel.refreshTypeOptions();
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        top.add(new JLabel("Add Type:"));
        top.add(addField);
        top.add(addBtn);
        top.add(clearBtn);

        JLabel desc = new JLabel("Define room categories and click a row to switch mode: Group Only, Subject Only, or Disabled.");
        desc.setFont(AppTheme.FONT_SMALL);
        desc.setForeground(AppTheme.TEXT2);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setOpaque(false);

        JLabel groupBadge = new JLabel("Group Only");
        groupBadge.setOpaque(true);
        groupBadge.setFont(AppTheme.FONT_SMALL);
        groupBadge.setForeground(new Color(0x166534));
        groupBadge.setBackground(new Color(0xDCFCE7));
        groupBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel subjectBadge = new JLabel("Subject Only");
        subjectBadge.setOpaque(true);
        subjectBadge.setFont(AppTheme.FONT_SMALL);
        subjectBadge.setForeground(new Color(0x1E40AF));
        subjectBadge.setBackground(new Color(0xDBEAFE));
        subjectBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel disabledBadge = new JLabel("Disabled");
        disabledBadge.setOpaque(true);
        disabledBadge.setFont(AppTheme.FONT_SMALL);
        disabledBadge.setForeground(new Color(0x9A3412));
        disabledBadge.setBackground(new Color(0xFFEDD5));
        disabledBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel legendHint = new JLabel("Click a row (or press Space) to cycle mode");
        legendHint.setFont(AppTheme.FONT_SMALL);
        legendHint.setForeground(AppTheme.TEXT2);

        legend.add(groupBadge);
        legend.add(subjectBadge);
        legend.add(disabledBadge);
        legend.add(legendHint);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.add(removeBtn);
        actions.add(resetBtn);

        JPanel card = AppTheme.cardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Room Types"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel topSection = new JPanel(new BorderLayout(0, 6));
        topSection.setOpaque(false);
        topSection.add(top, BorderLayout.NORTH);
        topSection.add(desc, BorderLayout.CENTER);
        topSection.add(legend, BorderLayout.SOUTH);

        card.add(topSection, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void cycleRoomTypeMode(JList<String> list, Point p) {
        int idx = list.locationToIndex(p);
        if (idx < 0) return;
        Rectangle bounds = list.getCellBounds(idx, idx);
        if (!bounds.contains(p)) return;
        list.setSelectedIndex(idx);
        String type = roomTypeModel.get(idx);
        cycleRoomTypeMode(type);
        list.repaint();
    }

    private void cycleRoomTypeMode(String type) {
        if (type == null) return;
        boolean group = DataStore.isRoomTypeAssignableToGroups(type);
        boolean subject = DataStore.isRoomTypeAssignableToSubjects(type);
        if (group) {
            DataStore.setRoomTypeAssignableToGroups(type, false);
            DataStore.setRoomTypeAssignableToSubjects(type, true);
        } else if (subject) {
            DataStore.setRoomTypeAssignableToSubjects(type, false);
        } else {
            DataStore.setRoomTypeAssignableToGroups(type, true);
        }
    }

    private class RoomTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);

            if (isSelected) {
                panel.setBackground(AppTheme.SEL_BG);
                panel.setForeground(AppTheme.SEL_FG);
            } else {
                panel.setBackground(AppTheme.SURFACE);
                panel.setForeground(AppTheme.TEXT);
            }

            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));

            String type = (String) value;

            JLabel typeLabel = new JLabel(type);
            typeLabel.setForeground(panel.getForeground());
            typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

            boolean allowGroups = DataStore.isRoomTypeAssignableToGroups(type);
            boolean allowSubjects = DataStore.isRoomTypeAssignableToSubjects(type);

            JLabel modeBadge;
            if (allowGroups) {
                modeBadge = new JLabel("Group Only");
                modeBadge.setForeground(new Color(0x166534));
                modeBadge.setBackground(new Color(0xDCFCE7));
            } else if (allowSubjects) {
                modeBadge = new JLabel("Subject Only");
                modeBadge.setForeground(new Color(0x1E40AF));
                modeBadge.setBackground(new Color(0xDBEAFE));
            } else {
                modeBadge = new JLabel("Disabled");
                modeBadge.setForeground(new Color(0x9A3412));
                modeBadge.setBackground(new Color(0xFFEDD5));
            }
            modeBadge.setOpaque(true);
            modeBadge.setFont(AppTheme.FONT_SMALL);
            modeBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

            panel.add(typeLabel, BorderLayout.CENTER);
            panel.add(modeBadge, BorderLayout.EAST);
            return panel;
        }
    }

    public void reloadFromStore() {
        loadModelsFromStore();
    }

    private void loadModelsFromStore() {
        subjectTypeModel.clear();
        for (String t : DataStore.getSubjectTypeOptions()) subjectTypeModel.addElement(t);

        roomTypeModel.clear();
        for (String t : DataStore.getRoomTypeOptions()) roomTypeModel.addElement(t);

        multiGroupTypes = new java.util.LinkedHashSet<>(DataStore.getMultiGroupSubjectTypes());
        if (subjectTypeList != null) {
            subjectTypeList.repaint();
        }
        if (roomTypeList != null) {
            roomTypeList.repaint();
        }
    }
}
