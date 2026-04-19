package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Centralized theme constants and component-styling helpers.
 * Call {@link #apply()} once (before creating the main frame) to set UIManager defaults.
 */
public final class AppTheme {

    private AppTheme() {}

    // ── Colour palette ────────────────────────────────────────────────────────
    public static final Color BG            = new Color(0xF1F5F9);
    public static final Color SURFACE       = Color.WHITE;
    public static final Color SIDEBAR_BG    = new Color(0x1E293B);
    public static final Color SIDEBAR_SEL   = new Color(0x3B82F6);
    public static final Color SIDEBAR_HOV   = new Color(0x334155);
    public static final Color PRIMARY       = new Color(0x3B82F6);
    public static final Color PRIMARY_DK    = new Color(0x1D4ED8);
    public static final Color DANGER        = new Color(0xEF4444);
    public static final Color DANGER_DK     = new Color(0xDC2626);
    public static final Color TEXT          = new Color(0x1E293B);
    public static final Color TEXT2         = new Color(0x64748B);
    public static final Color BORDER        = new Color(0xE2E8F0);
    public static final Color TH_BG         = new Color(0xF8FAFC);
    public static final Color ROW_ALT       = new Color(0xF8FAFC);
    public static final Color SEL_BG        = new Color(0xBFDBFE);
    public static final Color SEL_FG        = new Color(0x1E3A5F);
    public static final Color SIDEBAR_TEXT2 = new Color(0x94A3B8);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BTN     = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BTN_PRI = new Font("Segoe UI", Font.BOLD,  12);

    // ── Apply global UIManager defaults ───────────────────────────────────────
    public static void apply() {
        UIManager.put("Panel.background",       BG);
        UIManager.put("TextField.background",   SURFACE);
        UIManager.put("TextField.foreground",   TEXT);
        UIManager.put("TextField.font",         FONT_BODY);
        UIManager.put("TextArea.font",          FONT_BODY);
        UIManager.put("ComboBox.font",          FONT_BODY);
        UIManager.put("ComboBox.foreground",    TEXT);
        UIManager.put("Label.font",             FONT_BODY);
        UIManager.put("Label.foreground",       TEXT);
        UIManager.put("CheckBox.font",          FONT_BODY);
        UIManager.put("CheckBox.foreground",    TEXT);
        UIManager.put("CheckBox.background",    BG);
        UIManager.put("RadioButton.font",       FONT_BODY);
        UIManager.put("Spinner.font",           FONT_BODY);
        UIManager.put("TabbedPane.font",        FONT_BODY);
        UIManager.put("Table.font",             FONT_BODY);
        UIManager.put("TableHeader.font",       FONT_TITLE);
        UIManager.put("Button.font",            FONT_BTN);
        UIManager.put("ToggleButton.font",      FONT_BTN);
        UIManager.put("TitledBorder.font",      FONT_TITLE);
        UIManager.put("TitledBorder.titleColor",TEXT2);
        UIManager.put("OptionPane.background",  SURFACE);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("List.font",              FONT_BODY);
        UIManager.put("List.selectionBackground", SEL_BG);
        UIManager.put("List.selectionForeground", SEL_FG);
    }

    // ── Button styling ────────────────────────────────────────────────────────

    /** Blue filled primary action button (Add, Save, Generate). */
    public static void stylePrimary(JButton btn) {
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BTN_PRI);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(PRIMARY_DK); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(PRIMARY); }
        });
    }

    /** Red filled danger button (Delete). */
    public static void styleDanger(JButton btn) {
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(DANGER_DK); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(DANGER); }
        });
    }

    /** Outlined secondary button (Clear, Browse, Import, Refresh). */
    public static void styleSecondary(JButton btn) {
        btn.setBackground(SURFACE);
        btn.setForeground(TEXT);
        btn.setFont(FONT_BTN);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(TH_BG); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(SURFACE); }
        });
    }

    // ── Table styling (for data/CRUD tables, not the timetable grid) ─────────
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setSelectionBackground(SEL_BG);
        table.setSelectionForeground(SEL_FG);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TITLE);
        header.setBackground(TH_BG);
        header.setForeground(TEXT2);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 34));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? SURFACE : ROW_ALT);
                    setForeground(TEXT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setFont(FONT_BODY);
                return this;
            }
        });
    }

    // ── Scroll pane that matches the theme ───────────────────────────────────
    public static JScrollPane modernScroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(SURFACE);
        return sp;
    }

    // ── Card-style panel (white bg + thin border) ─────────────────────────────
    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        return p;
    }

    // ── Action card (coloured stripe + icon + title + description + button) ───
    public static JPanel makeActionCard(String icon, String title, String desc,
                                        Color accentColor, JButton btn) {
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(5, 0));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        iconLbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(TEXT);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(FONT_SMALL);
        descLbl.setForeground(TEXT2);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(descLbl);

        btn.setPreferredSize(new Dimension(200, 36));
        btn.setMinimumSize(new Dimension(200, 36));
        btn.setMaximumSize(new Dimension(200, 36));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btn);

        JPanel inner = new JPanel(new BorderLayout(0, 0));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(iconLbl, BorderLayout.WEST);
        left.add(textPanel, BorderLayout.CENTER);
        inner.add(left,    BorderLayout.CENTER);
        inner.add(btnWrap, BorderLayout.EAST);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        card.add(stripe, BorderLayout.WEST);
        card.add(inner,  BorderLayout.CENTER);
        return card;
    }
}
