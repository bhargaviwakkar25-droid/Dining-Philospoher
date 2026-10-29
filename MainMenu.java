package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainMenu extends JPanel {

    private final Main mainApp;
    private JComboBox<Integer> countCombo;

    public MainMenu(Main mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.COLOR_PRIMARY_LIGHT);

        buildUI();
    }

    private void buildUI() {
        removeAll();

        // 1. Top Header Banner
        JPanel headerBar = UIUtils.createHeaderBar("Dining Philosopher Simulator", null);
        add(headerBar, BorderLayout.NORTH);

        // 2. Center Content with 4 Cards
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(25, 30, 20, 30));

        JPanel cardsContainer = new JPanel(new GridLayout(1, 4, 20, 0));
        cardsContainer.setOpaque(false);
        cardsContainer.setPreferredSize(new Dimension(1060, 420));

        // Card 1: Deadlock Demonstration
        cardsContainer.add(createMenuCard(
                "Deadlock\nDemonstration",
                "Visualize how deadlock occurs in the Dining Philosopher Problem.",
                "Open Simulation",
                new Color(198, 40, 40),
                new Color(255, 235, 238),
                CardIconType.DEADLOCK,
                () -> mainApp.showScreen(Main.SCREEN_DEADLOCK)
        ));

        // Card 2: Semaphore Solution
        cardsContainer.add(createMenuCard(
                "Semaphore\nSolution",
                "Observe how the N-1 semaphore solution prevents deadlock.",
                "Open Simulation",
                new Color(21, 101, 192),
                new Color(227, 242, 253),
                CardIconType.SEMAPHORE,
                () -> mainApp.showScreen(Main.SCREEN_SEMAPHORE)
        ));

        // Card 3: DQN-Based AI Solution
        cardsContainer.add(createMenuCard(
                "DQN-Based\nAI Solution",
                "See how a Deep Q-Network learns scheduling decisions for philosophers.",
                "Open Simulation",
                new Color(123, 31, 162),
                new Color(243, 229, 245),
                CardIconType.DQN,
                () -> mainApp.showScreen(Main.SCREEN_DQN)
        ));

        // Card 4: Compare Algorithms
        cardsContainer.add(createMenuCard(
                "Compare\nAlgorithms",
                "Compare Deadlock, Semaphore, and DQN based on key features.",
                "Open Comparison",
                new Color(0, 137, 123),
                new Color(224, 242, 241),
                CardIconType.COMPARE,
                () -> mainApp.showScreen(Main.SCREEN_COMPARE)
        ));

        center.add(cardsContainer);
        add(center, BorderLayout.CENTER);

        // 3. Bottom Control Row
        JPanel bottomBar = new JPanel(new BorderLayout(15, 0));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.COLOR_CARD_BORDER),
                new EmptyBorder(12, 35, 12, 35)
        ));

        // Left: Philosopher Count Selector
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBottom.setOpaque(false);
        JLabel countLbl = new JLabel("Number of Philosophers (for simulations):");
        countLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        countCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        countCombo.setSelectedItem(mainApp.getGlobalPhilosopherCount());
        countCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        countCombo.setPreferredSize(new Dimension(65, 30));
        countCombo.addActionListener(e -> {
            int val = (Integer) countCombo.getSelectedItem();
            mainApp.setGlobalPhilosopherCount(val);
        });

        leftBottom.add(countLbl);
        leftBottom.add(countCombo);
        bottomBar.add(leftBottom, BorderLayout.WEST);

        // Right: Exit Button
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBottom.setOpaque(false);

        JButton exitBtn = UIUtils.createStyledButton("Exit", new Color(13, 71, 161), 110, 34);
        exitBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to exit the Dining Philosopher Simulator?",
                    "Exit Simulator",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
        rightBottom.add(exitBtn);
        bottomBar.add(rightBottom, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private enum CardIconType { DEADLOCK, SEMAPHORE, DQN, COMPARE }

    private JPanel createMenuCard(String title, String desc, String btnText,
                                  Color themeColor, Color iconBg, CardIconType iconType, Runnable onClick) {
        JPanel card = UIUtils.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 16, 20, 16));

        // Top: Custom Icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Circular background badge
                g2.setColor(iconBg);
                g2.fillOval(cx - 36, cy - 36, 72, 72);
                g2.setColor(themeColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - 36, cy - 36, 72, 72);

                if (iconType == CardIconType.DEADLOCK) {
                    // Circular nodes in deadlock
                    int r = 18;
                    for (int i = 0; i < 5; i++) {
                        double ang = 2 * Math.PI * i / 5 - Math.PI / 2;
                        int nx = (int) (cx + r * Math.cos(ang));
                        int ny = (int) (cy + r * Math.sin(ang));
                        g2.setColor(themeColor);
                        g2.fillOval(nx - 4, ny - 4, 8, 8);
                    }
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                } else if (iconType == CardIconType.SEMAPHORE) {
                    // Semaphores & table
                    g2.setColor(new Color(192, 133, 76));
                    g2.fillOval(cx - 15, cy - 15, 30, 30);
                    int r = 22;
                    for (int i = 0; i < 5; i++) {
                        double ang = 2 * Math.PI * i / 5 - Math.PI / 2;
                        int nx = (int) (cx + r * Math.cos(ang));
                        int ny = (int) (cy + r * Math.sin(ang));
                        g2.setColor(themeColor);
                        g2.fillOval(nx - 5, ny - 5, 10, 10);
                    }
                } else if (iconType == CardIconType.DQN) {
                    // AI Chip & Neural Network nodes
                    g2.setColor(themeColor);
                    g2.fillRoundRect(cx - 14, cy - 14, 28, 28, 6, 6);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("AI", cx - fm.stringWidth("AI") / 2, cy + 5);

                    // Pins
                    g2.setColor(themeColor);
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawLine(cx - 20, cy - 6, cx - 14, cy - 6);
                    g2.drawLine(cx - 20, cy + 6, cx - 14, cy + 6);
                    g2.drawLine(cx + 14, cy - 6, cx + 20, cy - 6);
                    g2.drawLine(cx + 14, cy + 6, cx + 20, cy + 6);
                    g2.drawLine(cx - 6, cy - 20, cx - 6, cy - 14);
                    g2.drawLine(cx + 6, cy - 20, cx + 6, cy - 14);
                    g2.drawLine(cx - 6, cy + 14, cx - 6, cy + 20);
                    g2.drawLine(cx + 6, cy + 14, cx + 6, cy + 20);
                } else {
                    // Bar Chart
                    g2.setColor(themeColor);
                    g2.fillRoundRect(cx - 16, cy + 2, 7, 16, 2, 2);
                    g2.fillRoundRect(cx - 4, cy - 12, 7, 30, 2, 2);
                    g2.fillRoundRect(cx + 8, cy - 4, 7, 22, 2, 2);
                }

                g2.dispose();
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(80, 85));

        // Center: Title & Description
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        String[] lines = title.split("\n");
        for (String line : lines) {
            JLabel titleLabel = new JLabel(line);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(themeColor);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            textPanel.add(titleLabel);
        }

        textPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel descLabel = new JLabel("<html><div style='text-align: center;'>" + desc + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 116, 139));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(descLabel);

        // Bottom: Action Button
        JButton actionBtn = UIUtils.createStyledButton(btnText, themeColor, 0, 36);
        actionBtn.addActionListener(e -> onClick.run());

        card.add(iconPanel, BorderLayout.NORTH);
        card.add(textPanel, BorderLayout.CENTER);
        card.add(actionBtn, BorderLayout.SOUTH);

        return card;
    }

    public void updateSelectedCount(int count) {
        if (countCombo != null) {
            countCombo.setSelectedItem(count);
        }
    }
}
