package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class AlgorithmComparisonPanel extends JPanel {

    private final Main mainApp;

    public AlgorithmComparisonPanel(Main mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.COLOR_PRIMARY_LIGHT);

        buildUI();
    }

    private void buildUI() {
        removeAll();

        // 1. Top Header Bar
        JPanel headerBar = UIUtils.createHeaderBar("Dining Philosopher Simulator - Algorithm Comparison", () -> {
            mainApp.showScreen(Main.SCREEN_MENU);
        });
        add(headerBar, BorderLayout.NORTH);

        // 2. Center Content
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 24, 16, 24));

        // Subtitle with Bar Chart Icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel chartIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 137, 123)); // Teal
                g2.fillRoundRect(2, 14, 6, 12, 3, 3);
                g2.fillRoundRect(10, 6, 6, 20, 3, 3);
                g2.fillRoundRect(18, 10, 6, 16, 3, 3);
                g2.dispose();
            }
        };
        chartIcon.setPreferredSize(new Dimension(26, 26));

        JLabel titleLbl = new JLabel("Comparison of Algorithms");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(new Color(21, 101, 192));

        titlePanel.add(chartIcon);
        titlePanel.add(titleLbl);
        center.add(titlePanel);
        center.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. Comparison Table
        String[] columnNames = {"Feature", "Deadlock Demonstration", "Semaphore Solution", "DQN-Based AI Solution"};
        Object[][] rowData = {
                {"Main Purpose", "Demonstrate deadlock", "Prevent classic deadlock", "Learn scheduling decisions"},
                {"Main Technique", "Unsafe resource acquisition", "N \u2212 1 room semaphore", "Neural network and Q-values"},
                {"Learning Required", "No", "No", "Yes"},
                {"Reward System", "No", "No", "Yes"},
                {"Resource Control", "Unsafe acquisition", "Semaphore synchronization", "DQN chooses actions; environment checks safety"},
                {"Training Required", "No", "No", "Yes"},
                {"Complexity", "Easy", "Easy", "Advanced"}
        };

        DefaultTableModel tableModel = new DefaultTableModel(rowData, columnNames) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(true);
        table.setGridColor(new Color(225, 232, 238));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(UIUtils.COLOR_PRIMARY_DARK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(100, 34));
        header.setReorderingAllowed(false);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(320);

        // Custom Cell Renderer with column coloring
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 12));
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);

                if (col == 0) {
                    c.setBackground(new Color(245, 248, 252));
                    c.setForeground(new Color(33, 37, 41));
                } else if (col == 1) {
                    c.setBackground(new Color(255, 245, 245));
                    c.setForeground(new Color(183, 28, 28));
                } else if (col == 2) {
                    c.setBackground(new Color(240, 248, 255));
                    c.setForeground(new Color(13, 71, 161));
                } else {
                    c.setBackground(new Color(250, 243, 255));
                    c.setForeground(new Color(74, 20, 140));
                }
                return c;
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(950, 260));
        tableScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1));
        center.add(tableScroll);
        center.add(Box.createRigidArea(new Dimension(0, 16)));

        // 4. Algorithm Summary Cards Row
        JPanel cardsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(950, 200));

        // Card 1: Deadlock
        JPanel deadlockCard = createSummaryCard(
                "Deadlock Demonstration",
                new Color(198, 40, 40),
                new Color(255, 235, 238),
                new String[]{
                        "\u2022 Shows how circular-wait deadlock occurs.",
                        "\u2022 All philosophers acquire one chopstick and wait.",
                        "\u2022 Useful for demonstrating Coffman conditions.",
                        "\u2022 Deterministic state machine prevents UI freeze."
                },
                "Open Deadlock Simulation",
                () -> mainApp.showScreen(Main.SCREEN_DEADLOCK)
        );

        // Card 2: Semaphore
        JPanel semaphoreCard = createSummaryCard(
                "Semaphore Solution",
                new Color(21, 101, 192),
                new Color(227, 242, 253),
                new String[]{
                        "\u2022 Uses N \u2212 1 counting room semaphore.",
                        "\u2022 Limits diner count to at most N \u2212 1.",
                        "\u2022 Mathematically guarantees at least 1 diner eats.",
                        "\u2022 Classic operating systems synchronization solution."
                },
                "Open Semaphore Simulation",
                () -> mainApp.showScreen(Main.SCREEN_SEMAPHORE)
        );

        // Card 3: DQN
        JPanel dqnCard = createSummaryCard(
                "DQN-Based AI Solution",
                new Color(123, 31, 162),
                new Color(243, 229, 245),
                new String[]{
                        "\u2022 Uses Deep Q-Network Reinforcement Learning.",
                        "\u2022 Compact neural network estimates action values.",
                        "\u2022 Environment verifies resource safety rigorously.",
                        "\u2022 Learns optimal non-conflicting scheduling."
                },
                "Open DQN Simulation",
                () -> mainApp.showScreen(Main.SCREEN_DQN)
        );

        cardsRow.add(deadlockCard);
        cardsRow.add(semaphoreCard);
        cardsRow.add(dqnCard);
        center.add(cardsRow);

        // 5. Bottom Navigation Bar
        JPanel bottomNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        bottomNav.setOpaque(false);

        JButton backBtn = UIUtils.createStyledButton("Back to Main Menu", UIUtils.COLOR_PRIMARY_DARK, 180, 36);
        backBtn.addActionListener(e -> mainApp.showScreen(Main.SCREEN_MENU));
        bottomNav.add(backBtn);

        add(center, BorderLayout.CENTER);
        add(bottomNav, BorderLayout.SOUTH);
    }

    private JPanel createSummaryCard(String title, Color themeColor, Color headerBg,
                                     String[] points, String btnText, Runnable onClick) {
        JPanel card = UIUtils.createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        // Card Header Banner
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        head.setBackground(headerBg);
        head.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, themeColor));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(themeColor);
        head.add(titleLbl);

        // Points List
        JPanel pointsPanel = new JPanel();
        pointsPanel.setLayout(new BoxLayout(pointsPanel, BoxLayout.Y_AXIS));
        pointsPanel.setOpaque(false);
        pointsPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

        for (String pt : points) {
            JLabel lbl = new JLabel(pt);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(55, 65, 81));
            pointsPanel.add(lbl);
            pointsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        // Open Simulation Button
        JButton openBtn = UIUtils.createStyledButton(btnText, themeColor, 0, 30);
        openBtn.addActionListener(e -> onClick.run());

        card.add(head, BorderLayout.NORTH);
        card.add(pointsPanel, BorderLayout.CENTER);
        card.add(openBtn, BorderLayout.SOUTH);

        return card;
    }
}
