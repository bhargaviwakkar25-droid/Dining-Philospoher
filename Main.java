package diningphilosopher;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    public static final String SCREEN_MENU = "MENU";
    public static final String SCREEN_DEADLOCK = "DEADLOCK";
    public static final String SCREEN_SEMAPHORE = "SEMAPHORE";
    public static final String SCREEN_DQN = "DQN";
    public static final String SCREEN_COMPARE = "COMPARE";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);

    private MainMenu menuPanel;
    private DeadlockPanel deadlockPanel;
    private SemaphorePanel semaphorePanel;
    private DQNPanel dqnPanel;
    private AlgorithmComparisonPanel comparePanel;

    private int globalPhilosopherCount = 5;

    public Main() {
        setTitle("Dining Philosopher Simulator: Deadlock, Semaphore Solution, and DQN-Based Adaptive Resource Allocation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 800);
        setMinimumSize(new Dimension(1040, 700));
        setLocationRelativeTo(null); // Center on screen

        initUI();
    }

    private void initUI() {
        menuPanel = new MainMenu(this);
        deadlockPanel = new DeadlockPanel(this);
        semaphorePanel = new SemaphorePanel(this);
        dqnPanel = new DQNPanel(this);
        comparePanel = new AlgorithmComparisonPanel(this);

        containerPanel.add(menuPanel, SCREEN_MENU);
        containerPanel.add(deadlockPanel, SCREEN_DEADLOCK);
        containerPanel.add(semaphorePanel, SCREEN_SEMAPHORE);
        containerPanel.add(dqnPanel, SCREEN_DQN);
        containerPanel.add(comparePanel, SCREEN_COMPARE);

        add(containerPanel, BorderLayout.CENTER);

        // Start at Main Menu
        showScreen(SCREEN_MENU);
    }

    public void showScreen(String screenName) {
        // Synchronize philosopher count when switching into simulation screens
        if (SCREEN_DEADLOCK.equals(screenName)) {
            deadlockPanel.setPhilosopherCount(globalPhilosopherCount);
        } else if (SCREEN_SEMAPHORE.equals(screenName)) {
            semaphorePanel.setPhilosopherCount(globalPhilosopherCount);
        } else if (SCREEN_DQN.equals(screenName)) {
            dqnPanel.setPhilosopherCount(globalPhilosopherCount);
        } else if (SCREEN_MENU.equals(screenName)) {
            menuPanel.updateSelectedCount(globalPhilosopherCount);
        }

        cardLayout.show(containerPanel, screenName);
    }

    public int getGlobalPhilosopherCount() {
        return globalPhilosopherCount;
    }

    public void setGlobalPhilosopherCount(int count) {
        this.globalPhilosopherCount = count;
    }
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            Main app = new Main();
            app.setVisible(true);
        });
    }
}
