package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SemaphorePanel extends JPanel {

    private final Main mainApp;
    private int numPhilosophers = 5;

    private List<Philosopher> philosophers = new ArrayList<>();
    private List<Chopstick> chopsticks = new ArrayList<>();

    // Concurrency: Room Semaphore & Chopstick Semaphores
    private Semaphore roomSemaphore;
    private Semaphore[] chopSemaphores;

    // Simulation Threads & Control
    private Thread[] philosopherThreads;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final Object pauseLock = new Object();
    private volatile int stepDelayMs = 1200;

    // GUI Components
    private TableViewPanel tableView;
    private JComboBox<Integer> philCountCombo;
    private JLabel roomStatusLabel;
    private JProgressBar roomBar;
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
    private JSlider speedSlider;

    public SemaphorePanel(Main mainApp) {
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

        // Room counting semaphore initialized to N - 1 (or 1 if N = 1)
        int roomCapacity = Math.max(1, n - 1);
        roomSemaphore = new Semaphore(roomCapacity, true);

        // Binary semaphores for chopsticks
        chopSemaphores = new Semaphore[n];
        for (int i = 0; i < n; i++) {
            chopSemaphores[i] = new Semaphore(1, true);
        }
    }

    private void buildUI() {
        removeAll();

        // 1. Top Header Bar
        JPanel headerBar = UIUtils.createHeaderBar("Dining Philosopher Simulator - Semaphore Solution", () -> {
            stopSimulation();
            mainApp.showScreen(Main.SCREEN_MENU);
        });
        add(headerBar, BorderLayout.NORTH);

        // Subheader with Philosopher count selector and Room Semaphore display
        JPanel subHeader = new JPanel(new BorderLayout(15, 0));
        subHeader.setBackground(Color.WHITE);
        subHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_CARD_BORDER));
        subHeader.setPreferredSize(new Dimension(1000, 48));

        JPanel leftSub = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        leftSub.setOpaque(false);
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
                tableView.updateData(philosophers, chopsticks);
                updateRoomDisplay();
                updateTables();
                log("Reset simulation for " + selected + " philosophers. Room permits = " + (Math.max(1, selected - 1)));
            }
        });
        leftSub.add(countLabel);
        leftSub.add(philCountCombo);

        // Room Semaphore Badge in subheader
        JPanel rightSub = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        rightSub.setOpaque(false);

        JLabel roomTitle = new JLabel("Room Semaphore (N-1):");
        roomTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        roomTitle.setForeground(UIUtils.COLOR_PRIMARY_DARK);

        roomStatusLabel = new JLabel(getRoomText());
        roomStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        roomStatusLabel.setForeground(new Color(46, 125, 50));

        roomBar = new JProgressBar(0, Math.max(1, numPhilosophers - 1));
        roomBar.setValue(Math.max(1, numPhilosophers - 1));
        roomBar.setPreferredSize(new Dimension(80, 18));
        roomBar.setForeground(UIUtils.COLOR_PRIMARY_DARK);

        rightSub.add(roomTitle);
        rightSub.add(roomStatusLabel);
        rightSub.add(roomBar);

        subHeader.add(leftSub, BorderLayout.WEST);
        subHeader.add(rightSub, BorderLayout.EAST);

        // 2. Main Content Split: Center Table View, Right Controls
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        tableView = new TableViewPanel(philosophers, chopsticks);
        tableView.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1));
        centerPanel.add(tableView, BorderLayout.CENTER);

        JPanel rightPanel = createRightPanel();
        centerPanel.add(rightPanel, BorderLayout.EAST);

        // 3. Bottom: Event Log
        JPanel bottomPanel = createBottomLogPanel();

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(subHeader, BorderLayout.NORTH);
        centerContainer.add(centerPanel, BorderLayout.CENTER);
        centerContainer.add(bottomPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        updateTables();
        updateRoomDisplay();
        log("Semaphore Solution ready. Initialized room = " + (Math.max(1, numPhilosophers - 1)) + " permits.");
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
        startBtn = UIUtils.createStyledButton("Start", UIUtils.COLOR_BTN_START, 0, 30);
        pauseBtn = UIUtils.createStyledButton("Pause", UIUtils.COLOR_BTN_PAUSE, 0, 30);
        resumeBtn = UIUtils.createStyledButton("Resume", UIUtils.COLOR_BTN_STEP, 0, 30);
        resumeBtn.setEnabled(false);
        pauseBtn.setEnabled(false);

        btnRow1.add(startBtn);
        btnRow1.add(pauseBtn);
        btnRow1.add(resumeBtn);

        JPanel btnRow2 = new JPanel(new GridLayout(1, 2, 6, 6));
        btnRow2.setOpaque(false);
        resetBtn = UIUtils.createStyledButton("Reset", UIUtils.COLOR_BTN_RESET, 0, 30);
        stepBtn = UIUtils.createStyledButton("Step Forward", UIUtils.COLOR_BTN_STEP, 0, 30);

        btnRow2.add(resetBtn);
        btnRow2.add(stepBtn);

        // Speed Slider: 1 = Slow, 5 = Medium, 10 = Fast
        JPanel speedBox = new JPanel(new BorderLayout(5, 2));
        speedBox.setOpaque(false);
        JLabel speedLbl = new JLabel("Simulation Speed:");
        speedLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));

        speedSlider = new JSlider(1, 10, 5);
        speedSlider.setOpaque(false);
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            // val 1 -> 2000ms (Slow), val 5 -> 1200ms (Medium), val 10 -> 250ms (Fast)
            stepDelayMs = 2200 - (val * 195);
        });

        JPanel labelBox = new JPanel(new BorderLayout());
        labelBox.setOpaque(false);
        JLabel slowLbl = new JLabel("Slow");
        slowLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel medLbl = new JLabel("Medium", SwingConstants.CENTER);
        medLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel fastLbl = new JLabel("Fast", SwingConstants.RIGHT);
        fastLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        labelBox.add(slowLbl, BorderLayout.WEST);
        labelBox.add(medLbl, BorderLayout.CENTER);
        labelBox.add(fastLbl, BorderLayout.EAST);

        speedBox.add(speedLbl, BorderLayout.NORTH);
        speedBox.add(speedSlider, BorderLayout.CENTER);
        speedBox.add(labelBox, BorderLayout.SOUTH);

        JPanel btnBox = new JPanel(new GridLayout(2, 1, 6, 6));
        btnBox.setOpaque(false);
        btnBox.add(btnRow1);
        btnBox.add(btnRow2);

        JPanel controlContent = new JPanel();
        controlContent.setLayout(new BoxLayout(controlContent, BoxLayout.Y_AXIS));
        controlContent.setOpaque(false);
        controlContent.add(btnBox);
        controlContent.add(Box.createRigidArea(new Dimension(0, 8)));
        controlContent.add(speedBox);

        controlsCard.add(controlContent, BorderLayout.CENTER);

        startBtn.addActionListener(e -> startSimulation());
        pauseBtn.addActionListener(e -> {
            if (isPaused.get()) {
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
        philScroll.setPreferredSize(new Dimension(300, 115));
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
        chopScroll.setPreferredSize(new Dimension(300, 115));
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

    private String getRoomText() {
        int avail = (roomSemaphore != null) ? roomSemaphore.availablePermits() : 0;
        int max = Math.max(1, numPhilosophers - 1);
        return avail + " / " + max + " available";
    }

    private void updateRoomDisplay() {
        if (roomStatusLabel != null && roomBar != null) {
            int max = Math.max(1, numPhilosophers - 1);
            int avail = (roomSemaphore != null) ? roomSemaphore.availablePermits() : 0;
            roomBar.setMaximum(max);
            roomBar.setValue(avail);
            roomStatusLabel.setText(avail + " / " + max + " available");
            if (avail == 0) {
                roomStatusLabel.setForeground(new Color(198, 40, 40));
            } else {
                roomStatusLabel.setForeground(new Color(46, 125, 50));
            }
        }
    }

    private void updateTables() {
        philTableModel.setRowCount(0);
        for (Philosopher p : philosophers) {
            philTableModel.addRow(new Object[]{p.getName(), p.getDisplayStatus()});
        }

        chopTableModel.setRowCount(0);
        for (Chopstick c : chopsticks) {
            chopTableModel.addRow(new Object[]{c.getName(), c.getStatusText()});
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String entry = "[" + UIUtils.getTimestamp() + "] " + message + "\n";
            logArea.append(entry);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // --- Simulation Logic ---

    private void startSimulation() {
        if (numPhilosophers == 1) {
            log("N = 1: Single philosopher requires 2 chopsticks to eat, but only 1 exists.");
            log("Dining cannot proceed safely without sufficient chopsticks.");
            return;
        }

        resetSimulation();
        isRunning.set(true);
        isPaused.set(false);
        startBtn.setEnabled(false);
        pauseBtn.setText("Pause");
        pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
        pauseBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        philCountCombo.setEnabled(false);

        log("Semaphore Simulation started with " + numPhilosophers + " philosophers.");
        log("Room Semaphore initialized to " + (numPhilosophers - 1) + " permits.");

        philosopherThreads = new Thread[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            final int index = i;
            philosopherThreads[i] = new Thread(() -> runPhilosopherLifecycle(index), "Philosopher-" + (index + 1));
            philosopherThreads[i].setDaemon(true);
            philosopherThreads[i].start();
        }
    }

    private void pauseSimulation() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            pauseBtn.setText("Resume");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_STEP);
            resumeBtn.setEnabled(true);
            log("Simulation paused.");
        }
    }

    private void resumeSimulation() {
        if (isRunning.get() && isPaused.get()) {
            synchronized (pauseLock) {
                isPaused.set(false);
                pauseLock.notifyAll();
            }
            pauseBtn.setText("Pause");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
            resumeBtn.setEnabled(false);
            log("Simulation resumed.");
        }
    }

    private void resetSimulation() {
        stopSimulation();

        initData(numPhilosophers);

        SwingUtilities.invokeLater(() -> {
            tableView.updateData(philosophers, chopsticks);
            updateTables();
            updateRoomDisplay();
            startBtn.setEnabled(true);
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(false);
            philCountCombo.setEnabled(true);
        });

        log("Simulation reset. All semaphores and resources refreshed.");
    }

    private void stopSimulation() {
        isRunning.set(false);
        isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        if (philosopherThreads != null) {
            for (Thread t : philosopherThreads) {
                if (t != null && t.isAlive()) {
                    t.interrupt();
                }
            }
        }
    }

    private void stepForward() {
        if (numPhilosophers == 1) {
            log("Step: Single philosopher cannot dine with only 1 chopstick.");
            return;
        }

        // If not running, start manual step mode
        if (!isRunning.get()) {
            isRunning.set(true);
            startBtn.setEnabled(false);
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(false);
            philCountCombo.setEnabled(false);
            log("Manual step mode active.");
        }

        // Trigger one philosopher to attempt dining
        new Thread(() -> {
            for (int i = 0; i < numPhilosophers; i++) {
                Philosopher p = philosophers.get(i);
                if (p.getState() == Philosopher.State.THINKING) {
                    executePhilosopherDiningStep(i);
                    break;
                }
            }
        }).start();
    }

    /**
     * Continuous loop for each philosopher thread under the N-1 Room Semaphore protocol.
     */
    private void runPhilosopherLifecycle(int idIndex) {
        Philosopher p = philosophers.get(idIndex);
        int n = numPhilosophers;
        int leftChopIdx = p.getLeftChopstickIndex(n);
        int rightChopIdx = p.getRightChopstickIndex(n);

        try {
            while (isRunning.get()) {
                checkPause();

                // 1. Thinking
                p.setState(Philosopher.State.THINKING);
                syncUI();
                sleep(stepDelayMs + ((idIndex * 350) % 900));
                checkPause();

                // 2. Hungry
                p.setState(Philosopher.State.HUNGRY);
                syncUI();
                log(p.getName() + " is HUNGRY and requesting Room permit.");
                sleep(stepDelayMs / 2);
                checkPause();

                // 3. Acquire Room Semaphore
                roomSemaphore.acquire();
                log(p.getName() + " entered Room. (Room permits left: " + roomSemaphore.availablePermits() + ")");
                syncUI();
                checkPause();

                try {
                    // 4. Acquire Left Chopstick
                    chopSemaphores[leftChopIdx].acquire();
                    chopsticks.get(leftChopIdx).pickUp(p.getId());
                    p.setHeldLeftChopstick(chopsticks.get(leftChopIdx).getId());
                    log(p.getName() + " acquired left chopstick " + chopsticks.get(leftChopIdx).getName());
                    syncUI();
                    sleep(stepDelayMs / 4);
                    checkPause();

                    // 5. Acquire Right Chopstick
                    chopSemaphores[rightChopIdx].acquire();
                    chopsticks.get(rightChopIdx).pickUp(p.getId());
                    p.setHeldRightChopstick(chopsticks.get(rightChopIdx).getId());
                    log(p.getName() + " acquired right chopstick " + chopsticks.get(rightChopIdx).getName());

                    // 6. Eating (Both chopsticks acquired)
                    p.setState(Philosopher.State.EATING);
                    p.incrementMealsEaten();
                    log(p.getName() + " is EATING (Meals completed: " + p.getMealsEaten() + ").");
                    syncUI();
                    sleep(stepDelayMs * 3 / 2);

                    // 7. Release Right Chopstick
                    chopsticks.get(rightChopIdx).putDown();
                    p.setHeldRightChopstick(-1);
                    chopSemaphores[rightChopIdx].release();
                    log(p.getName() + " released " + chopsticks.get(rightChopIdx).getName());

                    // 8. Release Left Chopstick
                    chopsticks.get(leftChopIdx).putDown();
                    p.setHeldLeftChopstick(-1);
                    chopSemaphores[leftChopIdx].release();
                    log(p.getName() + " released " + chopsticks.get(leftChopIdx).getName());

                } finally {
                    // 9. Release Room Semaphore
                    roomSemaphore.release();
                    log(p.getName() + " exited Room. (Room permits left: " + roomSemaphore.availablePermits() + ")");
                    p.setState(Philosopher.State.THINKING);
                    syncUI();
                }

                sleep(stepDelayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Executes a single one-shot dining cycle for manual step forward.
     */
    private void executePhilosopherDiningStep(int idIndex) {
        Philosopher p = philosophers.get(idIndex);
        int n = numPhilosophers;
        int leftChopIdx = p.getLeftChopstickIndex(n);
        int rightChopIdx = p.getRightChopstickIndex(n);

        p.setState(Philosopher.State.HUNGRY);
        syncUI();
        log("[Step] " + p.getName() + " is HUNGRY.");

        if (roomSemaphore.tryAcquire()) {
            log("[Step] " + p.getName() + " acquired Room permit. (Room permits: " + roomSemaphore.availablePermits() + ")");
            try {
                if (chopSemaphores[leftChopIdx].tryAcquire()) {
                    chopsticks.get(leftChopIdx).pickUp(p.getId());
                    p.setHeldLeftChopstick(chopsticks.get(leftChopIdx).getId());
                    log("[Step] " + p.getName() + " acquired left chopstick " + chopsticks.get(leftChopIdx).getName());

                    if (chopSemaphores[rightChopIdx].tryAcquire()) {
                        chopsticks.get(rightChopIdx).pickUp(p.getId());
                        p.setHeldRightChopstick(chopsticks.get(rightChopIdx).getId());
                        p.setState(Philosopher.State.EATING);
                        p.incrementMealsEaten();
                        log("[Step] " + p.getName() + " acquired right chopstick " + chopsticks.get(rightChopIdx).getName() + " and is EATING.");
                        syncUI();

                        // Short pause then release
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

                        chopsticks.get(rightChopIdx).putDown();
                        p.setHeldRightChopstick(-1);
                        chopSemaphores[rightChopIdx].release();

                        chopsticks.get(leftChopIdx).putDown();
                        p.setHeldLeftChopstick(-1);
                        chopSemaphores[leftChopIdx].release();
                        log("[Step] " + p.getName() + " finished eating and released both chopsticks.");
                    } else {
                        // Could not get right chopstick, release left
                        chopsticks.get(leftChopIdx).putDown();
                        p.setHeldLeftChopstick(-1);
                        chopSemaphores[leftChopIdx].release();
                        log("[Step] " + p.getName() + " right chopstick occupied. Released left chopstick.");
                    }
                } else {
                    log("[Step] " + p.getName() + " left chopstick occupied.");
                }
            } finally {
                roomSemaphore.release();
                p.setState(Philosopher.State.THINKING);
                syncUI();
            }
        } else {
            log("[Step] Room is full (Max " + (n - 1) + " philosophers). " + p.getName() + " must wait.");
        }
    }

    private void checkPause() throws InterruptedException {
        synchronized (pauseLock) {
            while (isPaused.get() && isRunning.get()) {
                pauseLock.wait();
            }
        }
    }

    private void sleep(int ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end && isRunning.get()) {
            try {
                checkPause();
                long remaining = end - System.currentTimeMillis();
                if (remaining > 0) {
                    Thread.sleep(Math.min(50, remaining));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void syncUI() {
        SwingUtilities.invokeLater(() -> {
            updateRoomDisplay();
            updateTables();
            tableView.repaint();
        });
    }

    public void setPhilosopherCount(int count) {
        if (philCountCombo != null) {
            philCountCombo.setSelectedItem(count);
        }
    }
}
