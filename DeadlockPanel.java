package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DeadlockPanel extends JPanel {

    private final Main mainApp;
    private int numPhilosophers = 5;

    private List<Philosopher> philosophers = new ArrayList<>();
    private List<Chopstick> chopsticks = new ArrayList<>();

    private TableViewPanel tableView;
    private JComboBox<Integer> philCountCombo;
    private JTable philTable;
    private DefaultTableModel philTableModel;
    private JTable chopTable;
    private DefaultTableModel chopTableModel;
    private JTextArea logArea;

    private JButton startBtn;
    private JButton pauseBtn;
    private JButton resumeBtn;
    private JButton resetBtn;
    private JButton stepBtn;

    // Simulation State Machine
    private Timer simTimer;
    private int simStep = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;

    public DeadlockPanel(Main mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIUtils.COLOR_PRIMARY_LIGHT);

        initData(5);
        buildUI();
    }

    private void initData(int n) {
        this.numPhilosophers = n;
        philosophers.clear();
        chopsticks.clear();

        for (int i = 1; i <= n; i++) {
            philosophers.add(new Philosopher(i));
            chopsticks.add(new Chopstick(i));
        }

        // Left & Right chopstick IDs for each philosopher
        for (int i = 0; i < n; i++) {
            int left = i + 1;
            int right = (n == 1) ? 1 : ((i + 1) % n + 1);
            philosophers.get(i).setHeldLeftChopstick(-1);
            philosophers.get(i).setHeldRightChopstick(-1);
        }
    }

    private void buildUI() {
        removeAll();

        // 1. Top Header Bar
        JPanel headerBar = UIUtils.createHeaderBar("Dining Philosopher Simulator - Deadlock Demonstration", () -> {
            stopSimulation();
            mainApp.showScreen(Main.SCREEN_MENU);
        });
        add(headerBar, BorderLayout.NORTH);

        // Subheader with Philosopher count selector
        JPanel subHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        subHeader.setBackground(Color.WHITE);
        subHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_CARD_BORDER));

        JLabel countLabel = new JLabel("Number of Philosophers:");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        philCountCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        philCountCombo.setSelectedItem(numPhilosophers);
        philCountCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        philCountCombo.addActionListener(e -> {
            int selected = (Integer) philCountCombo.getSelectedItem();
            if (selected != numPhilosophers) {
                stopSimulation();
                initData(selected);
                tableView.setDeadlockDetected(false);
                tableView.updateData(philosophers, chopsticks);
                updateTables();
                log("Reset simulation for " + selected + " philosophers.");
                if (selected == 1) {
                    log("Notice: For N = 1, classic circular wait cannot occur because circular dependency requires at least 2 philosophers.");
                }
            }
        });

        subHeader.add(countLabel);
        subHeader.add(philCountCombo);

        // 2. Main Content Split: Center-Left Table, Right Panel
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Center Table View
        tableView = new TableViewPanel(philosophers, chopsticks);
        tableView.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1));
        centerPanel.add(tableView, BorderLayout.CENTER);

        // Right Side: Controls and Tables
        JPanel rightPanel = createRightPanel();
        centerPanel.add(rightPanel, BorderLayout.EAST);

        // Bottom: Event Log
        JPanel bottomPanel = createBottomLogPanel();

        // Assemble
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(subHeader, BorderLayout.NORTH);
        centerContainer.add(centerPanel, BorderLayout.CENTER);
        centerContainer.add(bottomPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        updateTables();
        log("Deadlock Demonstration ready. Select philosopher count and click Start.");
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(320, 520));

        // Controls Card
        JPanel controlsCard = UIUtils.createCardPanel();
        controlsCard.setLayout(new BorderLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_PRIMARY_DARK, 1), "Controls");
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb.setTitleColor(UIUtils.COLOR_PRIMARY_DARK);
        controlsCard.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(6, 6, 6, 6)));

        JPanel btnRow1 = new JPanel(new GridLayout(1, 3, 6, 6));
        btnRow1.setOpaque(false);
        startBtn = UIUtils.createStyledButton("Start", UIUtils.COLOR_BTN_START, 0, 32);
        pauseBtn = UIUtils.createStyledButton("Pause", UIUtils.COLOR_BTN_PAUSE, 0, 32);
        resumeBtn = UIUtils.createStyledButton("Resume", UIUtils.COLOR_BTN_STEP, 0, 32);
        resumeBtn.setEnabled(false);
        pauseBtn.setEnabled(false);

        btnRow1.add(startBtn);
        btnRow1.add(pauseBtn);
        btnRow1.add(resumeBtn);

        JPanel btnRow2 = new JPanel(new GridLayout(1, 2, 6, 6));
        btnRow2.setOpaque(false);
        resetBtn = UIUtils.createStyledButton("Reset", UIUtils.COLOR_BTN_RESET, 0, 32);
        stepBtn = UIUtils.createStyledButton("Step Forward", UIUtils.COLOR_BTN_STEP, 0, 32);

        btnRow2.add(resetBtn);
        btnRow2.add(stepBtn);

        JPanel btnBox = new JPanel(new GridLayout(2, 1, 6, 6));
        btnBox.setOpaque(false);
        btnBox.add(btnRow1);
        btnBox.add(btnRow2);
        controlsCard.add(btnBox, BorderLayout.CENTER);

        startBtn.addActionListener(e -> startSimulation());
        pauseBtn.addActionListener(e -> {
            if (isPaused) {
                resumeSimulation();
            } else {
                pauseSimulation();
            }
        });
        resumeBtn.addActionListener(e -> resumeSimulation());
        resetBtn.addActionListener(e -> resetSimulation());
        stepBtn.addActionListener(e -> stepForward());

        right.add(controlsCard);
        right.add(Box.createRigidArea(new Dimension(0, 8)));

        // Philosopher Status Table Card
        JPanel philCard = UIUtils.createCardPanel();
        philCard.setLayout(new BorderLayout());
        TitledBorder tb2 = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_PRIMARY_DARK, 1), "Philosopher Status");
        tb2.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb2.setTitleColor(UIUtils.COLOR_PRIMARY_DARK);
        philCard.setBorder(BorderFactory.createCompoundBorder(tb2, new EmptyBorder(4, 4, 4, 4)));

        philTableModel = new DefaultTableModel(new String[]{"Philosopher", "State"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        philTable = new JTable(philTableModel);
        UIUtils.styleTable(philTable);
        philTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        philTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        philTable.setDefaultRenderer(Object.class, new UIUtils.PhilosopherCellRenderer());

        JScrollPane philScroll = new JScrollPane(philTable);
        philScroll.setPreferredSize(new Dimension(300, 130));
        philCard.add(philScroll, BorderLayout.CENTER);

        right.add(philCard);
        right.add(Box.createRigidArea(new Dimension(0, 8)));

        // Chopstick Status Table Card
        JPanel chopCard = UIUtils.createCardPanel();
        chopCard.setLayout(new BorderLayout());
        TitledBorder tb3 = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_PRIMARY_DARK, 1), "Chopstick Status");
        tb3.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb3.setTitleColor(UIUtils.COLOR_PRIMARY_DARK);
        chopCard.setBorder(BorderFactory.createCompoundBorder(tb3, new EmptyBorder(4, 4, 4, 4)));

        chopTableModel = new DefaultTableModel(new String[]{"Chopstick", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        chopTable = new JTable(chopTableModel);
        UIUtils.styleTable(chopTable);
        chopTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        chopTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        chopTable.setDefaultRenderer(Object.class, new UIUtils.ChopstickCellRenderer());

        JScrollPane chopScroll = new JScrollPane(chopTable);
        chopScroll.setPreferredSize(new Dimension(300, 130));
        chopCard.add(chopScroll, BorderLayout.CENTER);

        right.add(chopCard);

        return right;
    }

    private JPanel createBottomLogPanel() {
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 12, 10, 12));
        bottom.setPreferredSize(new Dimension(1000, 135));

        JPanel logHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        logHeader.setBackground(UIUtils.COLOR_PRIMARY_DARK);
        JLabel logTitle = new JLabel("Event Log");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logTitle.setForeground(Color.WHITE);
        logHeader.add(logTitle);

        logArea = new JTextArea();
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(new Color(33, 37, 41));
        logArea.setMargin(new Insets(4, 6, 4, 6));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1));

        bottom.add(logHeader, BorderLayout.NORTH);
        bottom.add(scrollPane, BorderLayout.CENTER);
        return bottom;
    }

    private void updateTables() {
        // Update Philosopher Table
        philTableModel.setRowCount(0);
        for (Philosopher p : philosophers) {
            philTableModel.addRow(new Object[]{p.getName(), p.getDisplayStatus()});
        }

        // Update Chopstick Table
        chopTableModel.setRowCount(0);
        for (Chopstick c : chopsticks) {
            chopTableModel.addRow(new Object[]{c.getName(), c.getStatusText()});
        }
    }

    private void log(String message) {
        String entry = "[" + UIUtils.getTimestamp() + "] " + message + "\n";
        logArea.append(entry);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // --- Simulation Logic ---

    private void startSimulation() {
        if (numPhilosophers == 1) {
            log("Simulation started...");
            log("P1 is THINKING.");
            log("P1 becomes HUNGRY.");
            log("With only 1 philosopher and 1 chopstick, circular wait cannot form.");
            log("A dining philosopher requires 2 chopsticks to eat, but only C1 exists.");
            return;
        }

        resetSimulation();
        isRunning = true;
        isPaused = false;
        startBtn.setEnabled(false);
        pauseBtn.setText("Pause");
        pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
        pauseBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        philCountCombo.setEnabled(false);

        log("Simulation started...");
        log("Initial state: All philosophers are THINKING.");

        simTimer = new Timer(1100, e -> executeStep());
        simTimer.start();
    }

    private void pauseSimulation() {
        if (isRunning && !isPaused) {
            if (simTimer != null) {
                simTimer.stop();
            }
            isPaused = true;
            pauseBtn.setText("Resume");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_STEP);
            resumeBtn.setEnabled(true);
            log("Simulation paused.");
        }
    }

    private void resumeSimulation() {
        if (isRunning && isPaused) {
            if (simTimer != null) {
                simTimer.start();
            }
            isPaused = false;
            pauseBtn.setText("Pause");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
            resumeBtn.setEnabled(false);
            log("Simulation resumed.");
        }
    }

    private void resetSimulation() {
        stopSimulation();
        simStep = 0;
        tableView.setDeadlockDetected(false);

        for (Philosopher p : philosophers) {
            p.reset();
        }
        for (Chopstick c : chopsticks) {
            c.reset();
        }

        updateTables();
        tableView.repaint();
        philCountCombo.setEnabled(true);
        startBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);
        log("Simulation reset. All resources cleared.");
    }

    private void stopSimulation() {
        if (simTimer != null) {
            simTimer.stop();
            simTimer = null;
        }
        isRunning = false;
        isPaused = false;
    }

    private void stepForward() {
        if (numPhilosophers == 1) {
            log("Step: N = 1 cannot demonstrate circular-wait deadlock.");
            return;
        }
        if (!isRunning) {
            isRunning = true;
            startBtn.setEnabled(false);
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(false);
            philCountCombo.setEnabled(false);
            log("Manual step mode started...");
        }
        executeStep();
    }

    private void executeStep() {
        int n = numPhilosophers;
        simStep++;

        if (simStep == 1) {
            // Step 1: All philosophers become hungry
            for (Philosopher p : philosophers) {
                p.setState(Philosopher.State.HUNGRY);
            }
            log("All philosophers are HUNGRY.");
        } else if (simStep >= 2 && simStep <= n + 1) {
            // Step 2 .. N+1: Each philosopher acquires their right chopstick (C_i)
            int pIndex = simStep - 2;
            Philosopher p = philosophers.get(pIndex);
            Chopstick c = chopsticks.get(pIndex);

            c.pickUp(p.getId());
            p.setHeldRightChopstick(c.getId());
            p.setCustomStateText("Holding C" + c.getId());
            p.setState(Philosopher.State.WAITING);

            log(p.getName() + " acquired chopstick " + c.getName());
        } else if (simStep == n + 2) {
            // Step N+2: Each attempts to acquire their left chopstick
            for (int i = 0; i < n; i++) {
                Philosopher p = philosophers.get(i);
                int leftChopIdx = p.getLeftChopstickIndex(n);
                Chopstick leftChop = chopsticks.get(leftChopIdx);
                log(p.getName() + " is waiting for second chopstick " + leftChop.getName() + " (held by P" + leftChop.getHolderId() + ")");
            }
            log("All philosophers are now waiting for their second chopstick held by neighbors...");
        } else if (simStep == n + 3) {
            // Step N+3: Circular Wait Deadlock Confirmation
            tableView.setDeadlockDetected(true);
            log("==========================================");
            log("DEADLOCK DETECTED: Circular-Wait Condition!");
            log("Every philosopher holds 1 chopstick and waits indefinitely for another.");
            log("No philosopher can eat. System is permanently blocked.");
            log("==========================================");

            stopSimulation();
            startBtn.setEnabled(false);
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(false);
        }

        updateTables();
        tableView.repaint();
    }

    public void setPhilosopherCount(int count) {
        if (philCountCombo != null) {
            philCountCombo.setSelectedItem(count);
        }
    }
}
