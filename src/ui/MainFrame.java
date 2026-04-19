package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import util.DataStore;

public class MainFrame extends JFrame {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int SIDEBAR_W  = 224;
    private static final int LOGO_H     = 72;
    private static final int SECTION_H  = 32;
    private static final int ITEM_H     = 46;
    private static final int ITEM_GAP   = 4;
    private static final int ITEM_X     = 10;
    private static final int ICON_X     = 26;
    private static final int LABEL_X    = 56;
    private static final int ACCENT_W   = 4;

    private static final String[] NAV_LABELS = {
        "Subjects", "Teachers", "Students", "Rooms", "Groups", "Generate", "Timetable View"
    };
    private static final String[] NAV_CARDS = {
        "Subjects", "Teachers", "Students", "Rooms", "Groups", "Generate", "Timetable"
    };
    private static final String[] NAV_ICONS = {
        "\uD83D\uDCDA",
        "\uD83D\uDC68\u200D\uD83C\uDFEB",
        "\uD83D\uDC64",
        "\uD83C\uDFE2",
        "\uD83D\uDC65",
        "\u26A1",
        "\uD83D\uDCC5",
    };

    private static final String[] BTM_LABELS = { "Save", "Open", "Option", "Quit" };
    private static final String[] BTM_ICONS  = { "\uD83D\uDCBE", "\uD83D\uDCC2", "\u2699", "\uD83D\uDEAA" };
    private static final int BTM_ITEM_H = 38;
    private static final int BTM_ITEM_GAP = 6;

    public MainFrame() {
        setTitle("Timetable Maker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);

        // ── Content panels ────────────────────────────────────────────────────
        SubjectPanel       subjectPanel  = new SubjectPanel();
        TeacherPanel       teacherPanel  = new TeacherPanel();
        RoomPanel          roomPanel     = new RoomPanel();
        GroupPanel         groupPanel    = new GroupPanel();
        StudentPanel       studentPanel  = new StudentPanel();
        TimetableViewPanel viewPanel     = new TimetableViewPanel();
        GeneratePanel      generatePanel = new GeneratePanel(viewPanel);
        OptionPanel        optionPanel   = new OptionPanel(subjectPanel, roomPanel);

        JPanel content = new JPanel(new CardLayout());
        content.setBackground(AppTheme.BG);
        content.add(subjectPanel,  "Subjects");
        content.add(teacherPanel,  "Teachers");
        content.add(studentPanel,  "Students");
        content.add(roomPanel,     "Rooms");
        content.add(groupPanel,    "Groups");
        content.add(generatePanel, "Generate");
        content.add(viewPanel,     "Timetable");
        content.add(optionPanel,   "Options");
        CardLayout cl = (CardLayout) content.getLayout();

        // ── Sidebar state ─────────────────────────────────────────────────────
        int[] sel = {0};
        int[] hov = {-1};
        int[] navScroll = {0};

        // ── Sidebar panel (fully custom-painted) ──────────────────────────────
        JPanel sidebar = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();

                // Full background
                g.setColor(new Color(0x1E293B));
                g.fillRect(0, 0, w, h);

                // Logo area
                g.setColor(new Color(0x0F172A));
                g.fillRect(0, 0, w, LOGO_H);

                // Blue dot
                g.setColor(new Color(0x3B82F6));
                g.fillOval(ICON_X, LOGO_H / 2 - 7, 14, 14);
                g.setColor(Color.WHITE);
                g.fillOval(ICON_X + 4, LOGO_H / 2 - 3, 6, 6);

                // App name
                g.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g.setColor(Color.WHITE);
                FontMetrics fm0 = g.getFontMetrics();
                g.drawString("Timetable Maker", LABEL_X, LOGO_H / 2 + fm0.getAscent() / 2 - 1);

                // Divider under logo
                g.setColor(new Color(0x334155));
                g.fillRect(0, LOGO_H, w, 1);

                // Section label
                int secY = LOGO_H + SECTION_H;
                g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g.setColor(new Color(0x64748B));
                g.drawString("NAVIGATION", ICON_X, secY - 8);

                // Nav items
                Font iconFont  = new Font("Segoe UI Emoji", Font.PLAIN, 14);
                Font plainFont = new Font("Segoe UI", Font.PLAIN, 13);
                Font boldFont  = new Font("Segoe UI", Font.BOLD,  13);

                // Calculate bottom area to determine nav clip bounds
                int btmRows = 3;
                int btmTotalH = btmRows * (BTM_ITEM_H + BTM_ITEM_GAP) - BTM_ITEM_GAP;
                int btmStartY = h - 42 - btmTotalH;
                Shape oldClip = g.getClip();
                g.clipRect(0, secY, w, Math.max(0, btmStartY - secY));

                for (int i = 0; i < NAV_LABELS.length; i++) {
                    int iy = secY + i * (ITEM_H + ITEM_GAP) - navScroll[0];
                    boolean isSel = i == sel[0];
                    boolean isHov = i == hov[0];

                    // Pill
                    if (isSel) {
                        g.setColor(new Color(0x2563EB));
                        g.fillRoundRect(ITEM_X, iy, w - ITEM_X * 2, ITEM_H, 10, 10);
                    } else if (isHov) {
                        g.setColor(new Color(0x334155));
                        g.fillRoundRect(ITEM_X, iy, w - ITEM_X * 2, ITEM_H, 10, 10);
                    }

                    // Accent bar
                    if (isSel) {
                        g.setColor(new Color(0x93C5FD));
                        g.fillRoundRect(ITEM_X, iy + 10, ACCENT_W, ITEM_H - 20, ACCENT_W, ACCENT_W);
                    }

                    // Icon
                    g.setFont(iconFont);
                    g.setColor(isSel ? Color.WHITE : (isHov ? new Color(0xCBD5E1) : new Color(0x64748B)));
                    FontMetrics ifm = g.getFontMetrics();
                    int ibase = iy + (ITEM_H + ifm.getAscent() - ifm.getDescent()) / 2;
                    g.drawString(NAV_ICONS[i], ICON_X + ACCENT_W + 4, ibase);

                    // Label
                    g.setFont(isSel ? boldFont : plainFont);
                    g.setColor(isSel ? Color.WHITE : (isHov ? new Color(0xCBD5E1) : new Color(0x94A3B8)));
                    FontMetrics lfm = g.getFontMetrics();
                    int lbase = iy + (ITEM_H + lfm.getAscent() - lfm.getDescent()) / 2;
                    int navTextW = lfm.stringWidth(NAV_LABELS[i]);
                    int navItemW = w - ITEM_X * 2;
                    int navTextX = ITEM_X + (navItemW - navTextW) / 2;
                    g.drawString(NAV_LABELS[i], navTextX, lbase);
                }

                g.setClip(oldClip);

                // Solid background for bottom area to ensure clean separation
                g.setColor(new Color(0x1E293B));
                g.fillRect(0, btmStartY - 10, w, h - btmStartY + 10);
                g.setColor(new Color(0x334155));
                g.fillRect(0, btmStartY - 10, w, 1);

                // bottom action items: Save/Open on row 1, Option on row 2, Quit on row 3
                int colGap = 8;
                int cellW = (w - ITEM_X * 2 - colGap) / 2;
                for (int i = 0; i < BTM_LABELS.length; i++) {
                    int ix;
                    int iy;
                    int width;
                    if (i < 2) {
                        int row = 0;
                        int col = i;
                        ix = ITEM_X + col * (cellW + colGap);
                        iy = btmStartY + row * (BTM_ITEM_H + BTM_ITEM_GAP);
                        width = cellW;
                    } else {
                        int row = i - 1;
                        ix = ITEM_X;
                        iy = btmStartY + row * (BTM_ITEM_H + BTM_ITEM_GAP);
                        width = w - ITEM_X * 2;
                    }
                    boolean isHovB = (hov[0] == NAV_LABELS.length + i);
                    if (isHovB) {
                        g.setColor(new Color(0x334155));
                        g.fillRoundRect(ix, iy, width, BTM_ITEM_H, 10, 10);
                    }
                    g.setFont(iconFont);
                    g.setColor(isHovB ? new Color(0xCBD5E1) : new Color(0x64748B));
                    FontMetrics bifm = g.getFontMetrics();
                    int bibase = iy + (BTM_ITEM_H + bifm.getAscent() - bifm.getDescent()) / 2;
                    int iconX = ix + 12;
                    g.drawString(BTM_ICONS[i], iconX, bibase);
                    g.setFont(plainFont);
                    g.setColor(isHovB ? new Color(0xCBD5E1) : new Color(0x94A3B8));
                    FontMetrics blfm = g.getFontMetrics();
                    int blbase = iy + (BTM_ITEM_H + blfm.getAscent() - blfm.getDescent()) / 2;
                    int btmTextW = blfm.stringWidth(BTM_LABELS[i]);
                    int btmTextX = ix + (width - btmTextW) / 2;
                    g.drawString(BTM_LABELS[i], btmTextX, blbase);
                }

                // Bottom strip
                g.setColor(new Color(0x334155));
                g.fillRect(0, h - 40, w, 1);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.setColor(new Color(0x475569));
                g.drawString("Academic Scheduler  v1.0", ICON_X, h - 14);

                g.dispose();
            }

            @Override
            public Dimension getPreferredSize() { return new Dimension(SIDEBAR_W, 0); }
            @Override
            public Dimension getMinimumSize()   { return new Dimension(SIDEBAR_W, 0); }
        };

        sidebar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sidebar.setOpaque(true);

        // final so lambdas can capture it
        final int navStartY = LOGO_H + SECTION_H;

        sidebar.addMouseWheelListener(we -> {
            int sH = sidebar.getHeight();
            int bTotalH = 3 * (BTM_ITEM_H + BTM_ITEM_GAP) - BTM_ITEM_GAP;
            int navAvailH = (sH - 42 - bTotalH) - navStartY;
            int navTotalH = NAV_LABELS.length * (ITEM_H + ITEM_GAP) - ITEM_GAP;
            int maxScroll = Math.max(0, navTotalH - navAvailH);
            navScroll[0] = Math.max(0, Math.min(navScroll[0] + we.getWheelRotation() * 30, maxScroll));
            sidebar.repaint();
        });

        sidebar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int navBottom = sidebar.getHeight() - 42 - (3 * (BTM_ITEM_H + BTM_ITEM_GAP) - BTM_ITEM_GAP);
                int navRow = rowAt(e.getY(), navStartY, navScroll[0], navBottom);
                int btmRow = btmRowAt(e.getX(), e.getY(), sidebar);
                int combined = navRow >= 0 ? navRow : (btmRow >= 0 ? NAV_LABELS.length + btmRow : -1);
                if (combined != hov[0]) { hov[0] = combined; sidebar.repaint(); }
            }
        });
        sidebar.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hov[0] = -1; sidebar.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                int navBottom = sidebar.getHeight() - 42 - (3 * (BTM_ITEM_H + BTM_ITEM_GAP) - BTM_ITEM_GAP);
                int row = rowAt(e.getY(), navStartY, navScroll[0], navBottom);
                if (row >= 0 && row < NAV_LABELS.length) {
                    sel[0] = row;
                    sidebar.repaint();
                    cl.show(content, NAV_CARDS[row]);
                    if ("Generate".equals(NAV_CARDS[row])) generatePanel.refreshAdjustments();
                    return;
                }
                int btmRow = btmRowAt(e.getX(), e.getY(), sidebar);
                switch (btmRow) {
                    case 0 -> handleSave(sidebar);
                    case 1 -> handleOpen(sidebar, subjectPanel, teacherPanel, roomPanel, groupPanel, studentPanel, optionPanel);
                    case 2 -> {
                        cl.show(content, "Options");
                        optionPanel.reloadFromStore();
                        hov[0] = -1;
                        sidebar.repaint();
                    }
                    case 3 -> handleQuit(sidebar);
                    default -> {
                    }
                }
            }
        });

        // ── Root ──────────────────────────────────────────────────────────────
        getContentPane().setBackground(AppTheme.BG);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);

        cl.show(content, NAV_CARDS[0]);
    }

    private static int rowAt(int mouseY, int navStartY, int scrollOffset, int navBottomY) {
        if (mouseY < navStartY || mouseY >= navBottomY) return -1;
        int rowH = ITEM_H + ITEM_GAP;
        for (int i = 0; i < NAV_LABELS.length; i++) {
            int iy = navStartY + i * rowH - scrollOffset;
            if (mouseY >= iy && mouseY < iy + ITEM_H) return i;
        }
        return -1;
    }

    private static int btmRowAt(int mouseX, int mouseY, JPanel sidebar) {
        int h = sidebar.getHeight();
        int btmRows = 3;
        int btmTotalH = btmRows * (BTM_ITEM_H + BTM_ITEM_GAP) - BTM_ITEM_GAP;
        int btmStartY = h - 42 - btmTotalH;
        int colGap = 8;
        int cellW = (SIDEBAR_W - ITEM_X * 2 - colGap);
        cellW /= 2;
        for (int i = 0; i < BTM_LABELS.length; i++) {
            int ix;
            int iy;
            int width;
            if (i < 2) {
                int row = 0;
                int col = i;
                ix = ITEM_X + col * (cellW + colGap);
                iy = btmStartY + row * (BTM_ITEM_H + BTM_ITEM_GAP);
                width = cellW;
            } else {
                int row = i - 1;
                ix = ITEM_X;
                iy = btmStartY + row * (BTM_ITEM_H + BTM_ITEM_GAP);
                width = SIDEBAR_W - ITEM_X * 2;
            }
            if (mouseX >= ix && mouseX < ix + width && mouseY >= iy && mouseY < iy + BTM_ITEM_H) return i;
        }
        return -1;
    }

    private static final javax.swing.filechooser.FileNameExtensionFilter TMT_FILTER =
            new FileNameExtensionFilter("Timetable Project (*.tmt)", "tmt");

    private void handleSave(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(TMT_FILTER);
        fc.setDialogTitle("Save Project");
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().endsWith(".tmt")) f = new File(f.getAbsolutePath() + ".tmt");
        try {
            DataStore.saveProject(f);
            JOptionPane.showMessageDialog(parent, "Project saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(parent, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleOpen(Component parent, SubjectPanel sp, TeacherPanel tp,
                            RoomPanel rp, GroupPanel gp, StudentPanel stp, OptionPanel op) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(TMT_FILTER);
        fc.setDialogTitle("Open Project");
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            DataStore.loadProject(fc.getSelectedFile());
            sp.refreshTable();
            tp.refreshTable();
            rp.refreshTable();
            gp.refreshTable();
            stp.refreshTable();
            sp.refreshTypeOptions();
            rp.refreshTypeOptions();
            op.reloadFromStore();
            JOptionPane.showMessageDialog(parent, "Project loaded.", "Opened", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(parent, "Open failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleQuit(Component parent) {
        int ok = JOptionPane.showConfirmDialog(parent,
                "Quit Timetable Maker?", "Quit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) System.exit(0);
    }
}
