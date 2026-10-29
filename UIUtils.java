package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Styling and component utilities for the Dining Philosopher Simulator GUI.
 * Provides a clean blue-and-white theme, rounded buttons, styled tables,
 * and standard header banners.
 */
public class UIUtils {

    // Color Palette
    public static final Color COLOR_PRIMARY_DARK = new Color(21, 101, 192);   // #1565C0 Deep Blue
    public static final Color COLOR_PRIMARY_LIGHT = new Color(235, 243, 250); // #EBF3FA Light Blue Background
    public static final Color COLOR_CARD_BG = Color.WHITE;
    public static final Color COLOR_CARD_BORDER = new Color(207, 216, 220);  // Soft Gray-Blue Border

    public static final Color COLOR_BTN_START = new Color(46, 125, 50);      // Green
    public static final Color COLOR_BTN_PAUSE = new Color(198, 40, 40);      // Red
    public static final Color COLOR_BTN_RESET = new Color(84, 110, 122);     // Slate Gray
    public static final Color COLOR_BTN_STEP = new Color(25, 118, 210);      // Blue
    public static final Color COLOR_BTN_EVAL = new Color(0, 131, 143);       // Teal
    public static final Color COLOR_BTN_PURPLE = new Color(123, 31, 162);    // Purple
    public static final Color COLOR_BTN_ORANGE = new Color(239, 108, 0);     // Orange

    // Status Colors
    public static final Color COLOR_THINKING = new Color(46, 125, 50);       // Green
    public static final Color COLOR_HUNGRY = new Color(245, 127, 23);        // Yellow/Amber
    public static final Color COLOR_EATING = new Color(21, 101, 192);        // Blue
    public static final Color COLOR_WAITING = new Color(198, 40, 40);        // Red
    public static final Color COLOR_AVAILABLE = new Color(117, 117, 117);    // Gray
    public static final Color COLOR_OCCUPIED = new Color(230, 81, 0);        // Orange

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    public static String getTimestamp() {
        return TIME_FMT.format(new Date());
    }

    /**
     * Creates a styled button with rounded corners, white bold text, and specified background.
     */
    public static JButton createStyledButton(String text, Color bgColor, int width, int height) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (width > 0 && height > 0) {
            button.setPreferredSize(new Dimension(width, height));
        }
        return button;
    }

    /**
     * Creates a rounded card panel.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(COLOR_CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        return panel;
    }

    /**
     * Creates the top navigation header bar with title, fork-knife icon, and back button.
     */
    public static JPanel createHeaderBar(String title, Runnable onBackClicked) {
        JPanel header = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Subtle gradient in header
                GradientPaint gp = new GradientPaint(0, 0, COLOR_PRIMARY_DARK, getWidth(), 0, new Color(13, 71, 161));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(1000, 52));
        header.setBorder(new EmptyBorder(8, 16, 8, 16));

        // Left: Icon + Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                // Draw clean fork & knife icon
                // Fork
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(6, 22, 6, 12);
                g2.drawLine(3, 8, 3, 12);
                g2.drawLine(6, 8, 6, 12);
                g2.drawLine(9, 8, 9, 12);
                g2.drawLine(3, 12, 9, 12);

                // Knife
                g2.drawLine(18, 22, 18, 12);
                g2.fillArc(15, 6, 6, 12, 0, 180);
                g2.dispose();
            }
        };
        iconLabel.setPreferredSize(new Dimension(26, 26));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);

        leftPanel.add(iconLabel);
        leftPanel.add(titleLabel);
        header.add(leftPanel, BorderLayout.WEST);

        // Right: Back to Main Menu button
        if (onBackClicked != null) {
            JButton backBtn = createStyledButton("Back to Main Menu", new Color(13, 71, 161).darker(), 160, 32);
            backBtn.addActionListener(e -> onBackClicked.run());
            header.add(backBtn, BorderLayout.EAST);
        }

        return header;
    }

    /**
     * Styles a JTable with standard dark-blue headers and centered cells.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 235, 240));
        table.setSelectionBackground(new Color(227, 242, 253));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(100, 28));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                                                           boolean isSelected, boolean hasFocus, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, r, c);
                lbl.setBackground(COLOR_PRIMARY_DARK);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(13, 71, 161)));
                return lbl;
            }
        });
    }

    /**
     * Custom renderer for Philosopher Status table.
     */
    public static class PhilosopherCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 12));

            String text = (value != null) ? value.toString() : "";

            if (col == 1) {
                if (text.contains("Thinking")) {
                    c.setBackground(new Color(232, 245, 233));
                    c.setForeground(COLOR_THINKING);
                } else if (text.contains("Hungry")) {
                    c.setBackground(new Color(255, 249, 196));
                    c.setForeground(COLOR_HUNGRY);
                } else if (text.contains("Eating")) {
                    c.setBackground(new Color(227, 242, 253));
                    c.setForeground(COLOR_EATING);
                } else if (text.contains("Waiting") || text.contains("Holding")) {
                    c.setBackground(new Color(255, 235, 238));
                    c.setForeground(COLOR_WAITING);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.DARK_GRAY);
                }
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                c.setForeground(Color.BLACK);
            }

            return c;
        }
    }

    /**
     * Custom renderer for Chopstick Status table.
     */
    public static class ChopstickCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 12));

            String text = (value != null) ? value.toString() : "";

            if (col == 1) {
                if (text.contains("In Use") || text.contains("Occupied")) {
                    c.setBackground(new Color(255, 235, 238));
                    c.setForeground(COLOR_OCCUPIED);
                } else {
                    c.setBackground(new Color(245, 245, 245));
                    c.setForeground(COLOR_AVAILABLE);
                }
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                c.setForeground(Color.BLACK);
            }

            return c;
        }
    }
}
