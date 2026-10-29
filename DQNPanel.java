package diningphilosopher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Screen 3: DQN-Based Adaptive Resource Allocation.
 * Implements genuine Deep Q-Network Reinforcement Learning:
 *   - Compact Feedforward Neural Network estimating Q(s, a)
 *   - Epsilon-Greedy Exploration with decay
 *   - Experience Replay Buffer for stability
 *   - Target Network for stable Bellman targets
 *   - Environment enforces strict resource safety (no shared chopstick conflicts)
 *   - Evaluates learned greedy policy without exploration
 */
public class DQNPanel extends JPanel {

    private final Main mainApp;
    private int numPhilosophers = 5;

    private List<Philosopher> philosophers = new ArrayList<>();
    private List<Chopstick> chopsticks = new ArrayList<>();

    // Reinforcement Learning Agent
    private DQNAgent dqnAgent;

    // Simulation & Training Controls
    private Timer simTimer;
    private int episodeCount = 0;
    private boolean isRunning = false;
    private boolean isEvaluationMode = false;
    private int stepDelayMs = 900;
    private final Random random = new Random();

    // GUI Components
    private TableViewPanel tableView;
    private JComboBox<Integer> philCountCombo;
    private JTable philTable;
    private DefaultTableModel philTableModel;
    private JTable chopTable;
    private DefaultTableModel chopTableModel;
    private JTextArea logArea;

    // DQN Metrics Labels
    private JLabel episodeValLabel;
    private JLabel actionValLabel;
    private JLabel rewardValLabel;
    private JLabel epsilonValLabel;
    private JLabel cumRewardValLabel;
    private JLabel lossValLabel;
    private JLabel qValuesValLabel;

    private JButton startBtn;
    private JButton pauseBtn;
    private JButton resumeBtn;
    private JButton resetBtn;
    private JButton stepBtn;
    private JButton evalBtn;
    private JButton allHungryBtn;
    private JSlider speedSlider;

    public DQNPanel(Main mainApp) {
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

        episodeCount = 0;
        dqnAgent = new DQNAgent(n);
    }

    private void buildUI() {
        removeAll();

        // 1. Top Header Bar
        JPanel headerBar = UIUtils.createHeaderBar("Dining Philosopher Simulator - DQN-Based Adaptive Resource Allocation", () -> {
            stopSimulation();
            mainApp.showScreen(Main.SCREEN_MENU);
        });
        add(headerBar, BorderLayout.NORTH);

        // Subheader with Philosopher Count Selector
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
                tableView.updateData(philosophers, chopsticks);
                updateTables();
                updateDqnMetricLabels("None", 0.0);
                log("Reset DQN environment for " + selected + " philosophers. Action space = " + selected + " actions.");
            }
        });

        subHeader.add(countLabel);
        subHeader.add(philCountCombo);

        // 2. Main Content Split: Center-Left Table, Right Control & DQN Stats Panel
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

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
        updateDqnMetricLabels("None", 0.0);
        log("DQN Agent initialized. Neural Network [Input: " + (numPhilosophers * 4) + " -> Hidden: 16 -> Output: " + numPhilosophers + "].");
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(350, 530));

        // Controls Card
        JPanel controlsCard = UIUtils.createCardPanel();
        controlsCard.setLayout(new BorderLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_PRIMARY_DARK, 1), "Controls");
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb.setTitleColor(UIUtils.COLOR_PRIMARY_DARK);
        controlsCard.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 6, 4, 6)));

        JPanel btnRow1 = new JPanel(new GridLayout(1, 3, 5, 5));
        btnRow1.setOpaque(false);
        startBtn = UIUtils.createStyledButton("Start Training", UIUtils.COLOR_BTN_START, 0, 28);
        pauseBtn = UIUtils.createStyledButton("Pause", UIUtils.COLOR_BTN_PAUSE, 0, 28);
        resumeBtn = UIUtils.createStyledButton("Resume", UIUtils.COLOR_BTN_STEP, 0, 28);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);

        btnRow1.add(startBtn);
        btnRow1.add(pauseBtn);
        btnRow1.add(resumeBtn);

        JPanel btnRow2 = new JPanel(new GridLayout(1, 2, 5, 5));
        btnRow2.setOpaque(false);
        resetBtn = UIUtils.createStyledButton("Reset", UIUtils.COLOR_BTN_RESET, 0, 28);
        stepBtn = UIUtils.createStyledButton("Step Forward", UIUtils.COLOR_BTN_STEP, 0, 28);
        btnRow2.add(resetBtn);
        btnRow2.add(stepBtn);

        JPanel btnRow3 = new JPanel(new GridLayout(1, 2, 5, 5));
        btnRow3.setOpaque(false);
        evalBtn = UIUtils.createStyledButton("Run Evaluation", UIUtils.COLOR_BTN_EVAL, 0, 28);
        allHungryBtn = UIUtils.createStyledButton("Make All Hungry", UIUtils.COLOR_BTN_ORANGE, 0, 28);
        btnRow3.add(evalBtn);
        btnRow3.add(allHungryBtn);

        // Speed Slider: 1 = Slow, 5 = Medium, 10 = Fast
        JPanel speedBox = new JPanel(new BorderLayout(4, 2));
        speedBox.setOpaque(false);
        JLabel speedLbl = new JLabel("Simulation Speed:");
        speedLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));

        speedSlider = new JSlider(1, 10, 5);
        speedSlider.setOpaque(false);
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            // val 1 -> 1800ms (Slow), val 5 -> 900ms (Medium), val 10 -> 200ms (Fast)
            stepDelayMs = 1950 - (val * 175);
            if (simTimer != null && simTimer.isRunning()) {
                simTimer.setDelay(stepDelayMs);
            }
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

        JPanel btnBox = new JPanel();
        btnBox.setLayout(new BoxLayout(btnBox, BoxLayout.Y_AXIS));
        btnBox.setOpaque(false);
        btnBox.add(btnRow1);
        btnBox.add(Box.createRigidArea(new Dimension(0, 4)));
        btnBox.add(btnRow2);
        btnBox.add(Box.createRigidArea(new Dimension(0, 4)));
        btnBox.add(btnRow3);
        btnBox.add(Box.createRigidArea(new Dimension(0, 6)));
        btnBox.add(speedBox);

        controlsCard.add(btnBox, BorderLayout.CENTER);

        startBtn.addActionListener(e -> startTraining());
        pauseBtn.addActionListener(e -> {
            if (isRunning) {
                pauseSimulation();
            } else {
                resumeSimulation();
            }
        });
        resumeBtn.addActionListener(e -> resumeSimulation());
        resetBtn.addActionListener(e -> resetSimulation());
        stepBtn.addActionListener(e -> stepForward());
        evalBtn.addActionListener(e -> runEvaluation());
        allHungryBtn.addActionListener(e -> makeAllHungry());

        right.add(controlsCard);
        right.add(Box.createRigidArea(new Dimension(0, 8)));

        // Tables Card (Philosopher & Chopstick Status Tables)
        JPanel tablesCard = UIUtils.createCardPanel();
        tablesCard.setLayout(new GridLayout(2, 1, 6, 6));

        // Philosopher Table
        philTableModel = new DefaultTableModel(new String[]{"Philosopher", "State"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        philTable = new JTable(philTableModel);
        UIUtils.styleTable(philTable);
        philTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        philTable.getColumnModel().getColumn(1).setPreferredWidth(165);
        philTable.setDefaultRenderer(Object.class, new UIUtils.PhilosopherCellRenderer());
        JScrollPane philScroll = new JScrollPane(philTable);
        philScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1), "Philosopher Status"));

        // Chopstick Table
        chopTableModel = new DefaultTableModel(new String[]{"Chopstick", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        chopTable = new JTable(chopTableModel);
        UIUtils.styleTable(chopTable);
        chopTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        chopTable.getColumnModel().getColumn(1).setPreferredWidth(165);
        chopTable.setDefaultRenderer(Object.class, new UIUtils.ChopstickCellRenderer());
        JScrollPane chopScroll = new JScrollPane(chopTable);
        chopScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_CARD_BORDER, 1), "Chopstick Status"));

        tablesCard.add(philScroll);
        tablesCard.add(chopScroll);

        right.add(tablesCard);

        return right;
    }

    private JPanel createDqnStatsCard() {
        JPanel card = UIUtils.createCardPanel();
        card.setLayout(new BorderLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BTN_PURPLE, 1), "DQN Learning Metrics");
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb.setTitleColor(UIUtils.COLOR_BTN_PURPLE);
        card.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 8, 4, 8)));

        JPanel grid = new JPanel(new GridLayout(3, 2, 6, 2));
        grid.setOpaque(false);

        grid.add(createMetricRow("Episode:", episodeValLabel = new JLabel("0")));
        grid.add(createMetricRow("Action:", actionValLabel = new JLabel("None")));
        grid.add(createMetricRow("Reward:", rewardValLabel = new JLabel("0.0")));
        grid.add(createMetricRow("Epsilon (\u03B5):", epsilonValLabel = new JLabel("1.00")));
        grid.add(createMetricRow("Cum. Reward:", cumRewardValLabel = new JLabel("0.0")));
        grid.add(createMetricRow("Loss (MSE):", lossValLabel = new JLabel("0.000")));

        // Q-Values Row
        JPanel qRow = new JPanel(new BorderLayout(4, 2));
        qRow.setOpaque(false);
        JLabel qLbl = new JLabel("Q-Values:");
        qLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        qValuesValLabel = new JLabel("[0.0, 0.0, 0.0, 0.0, 0.0]");
        qValuesValLabel.setFont(new Font("Consolas", Font.PLAIN, 10));
        qValuesValLabel.setForeground(new Color(21, 101, 192));
        qRow.add(qLbl, BorderLayout.WEST);
        qRow.add(qValuesValLabel, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        content.add(qRow, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricRow(String title, JLabel valLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setForeground(new Color(66, 66, 66));
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        valLabel.setForeground(UIUtils.COLOR_PRIMARY_DARK);
        p.add(t);
        p.add(valLabel);
        return p;
    }

    private JPanel createBottomLogPanel() {
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 12, 8, 12));
        bottom.setPreferredSize(new Dimension(1000, 135));

        // Left Container: Event Log
        JPanel logContainer = new JPanel(new BorderLayout(0, 0));
        logContainer.setOpaque(false);

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

        logContainer.add(logHeader, BorderLayout.NORTH);
        logContainer.add(scrollPane, BorderLayout.CENTER);

        // Right Container: DQN Learning Metrics Card (placed beside Event Log)
        JPanel statsCard = createDqnStatsCard();
        statsCard.setPreferredSize(new Dimension(420, 135));

        bottom.add(logContainer, BorderLayout.CENTER);
        bottom.add(statsCard, BorderLayout.EAST);
        return bottom;
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

    private void updateDqnMetricLabels(String actionText, double reward) {
        episodeValLabel.setText(String.valueOf(episodeCount));
        actionValLabel.setText(actionText);

        rewardValLabel.setText(String.format("%+.1f", reward));
        if (reward > 0) {
            rewardValLabel.setForeground(new Color(46, 125, 50));
        } else if (reward < 0) {
            rewardValLabel.setForeground(new Color(198, 40, 40));
        } else {
            rewardValLabel.setForeground(Color.DARK_GRAY);
        }

        double eps = dqnAgent.getEpsilon();
        epsilonValLabel.setText(String.format("%.2f", eps));

        double cumReward = dqnAgent.getCumulativeReward();
        cumRewardValLabel.setText(String.format("%+.1f", cumReward));

        double loss = dqnAgent.getLatestLoss();
        lossValLabel.setText(String.format("%.4f", loss));

        double[] qVals = dqnAgent.getLatestQValues();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < qVals.length; i++) {
            sb.append(String.format("P%d:%.1f", (i + 1), qVals[i]));
            if (i < qVals.length - 1) sb.append(", ");
        }
        sb.append("]");
        qValuesValLabel.setText(sb.toString());
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String entry = "[" + UIUtils.getTimestamp() + "] " + message + "\n";
            logArea.append(entry);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // --- Simulation & Learning Logic ---

    private void startTraining() {
        if (numPhilosophers == 1) {
            log("N = 1: Single philosopher needs 2 chopsticks. DQN cannot allocate dining without resource violation.");
            return;
        }

        isEvaluationMode = false;
        isRunning = true;
        startBtn.setEnabled(false);
        pauseBtn.setText("Pause");
        pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
        pauseBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        evalBtn.setEnabled(false);
        philCountCombo.setEnabled(false);

        log("DQN Training started (Epsilon = " + String.format("%.2f", dqnAgent.getEpsilon()) + ").");

        if (simTimer == null) {
            simTimer = new Timer(stepDelayMs, e -> executeDqnStep());
        } else {
            simTimer.setDelay(stepDelayMs);
        }
        simTimer.start();
    }

    private void pauseSimulation() {
        if (isRunning) {
            if (simTimer != null) {
                simTimer.stop();
            }
            isRunning = false;
            pauseBtn.setText("Resume");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_STEP);
            resumeBtn.setEnabled(true);
            evalBtn.setEnabled(true);
            log("DQN Training paused.");
        }
    }

    private void resumeSimulation() {
        if (!isRunning) {
            if (simTimer != null) {
                simTimer.start();
            } else {
                simTimer = new Timer(stepDelayMs, e -> executeDqnStep());
                simTimer.start();
            }
            isRunning = true;
            pauseBtn.setText("Pause");
            pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
            resumeBtn.setEnabled(false);
            evalBtn.setEnabled(false);
            log("DQN Training resumed.");
        }
    }

    private void resetSimulation() {
        stopSimulation();
        initData(numPhilosophers);

        tableView.updateData(philosophers, chopsticks);
        updateTables();
        updateDqnMetricLabels("None", 0.0);
        startBtn.setEnabled(true);
        pauseBtn.setText("Pause");
        pauseBtn.setBackground(UIUtils.COLOR_BTN_PAUSE);
        pauseBtn.setEnabled(false);
        resumeBtn.setEnabled(false);
        evalBtn.setEnabled(true);
        philCountCombo.setEnabled(true);

        log("DQN Environment & Replay Buffer reset.");
    }

    private void stopSimulation() {
        if (simTimer != null) {
            simTimer.stop();
            simTimer = null;
        }
        isRunning = false;
    }

    private void stepForward() {
        if (numPhilosophers == 1) {
            log("Step: N = 1 cannot perform safe dining.");
            return;
        }
        log("[Step] Manual step executed.");
        executeDqnStep();
    }

    private void runEvaluation() {
        if (numPhilosophers == 1) {
            log("Evaluation: Cannot evaluate with N = 1.");
            return;
        }

        isEvaluationMode = true;
        log("==========================================");
        log("Running Evaluation: Greedy policy (\u03B5 = 0, no exploration).");
        log("Testing learned scheduling decisions...");
        log("==========================================");

        // Run 5 evaluation steps
        Timer evalTimer = new Timer(800, null);
        final int[] evalSteps = {0};
        evalTimer.addActionListener(e -> {
            executeDqnStep();
            evalSteps[0]++;
            if (evalSteps[0] >= 5) {
                evalTimer.stop();
                isEvaluationMode = false;
                log("Evaluation completed.");
            }
        });
        evalTimer.start();
    }

    private void makeAllHungry() {
        for (Philosopher p : philosophers) {
            if (p.getState() != Philosopher.State.EATING) {
                p.setState(Philosopher.State.HUNGRY);
            }
        }
        updateTables();
        tableView.repaint();
        log("All non-eating philosophers set to HUNGRY.");
    }

    /**
     * Executes one DQN episode/step:
     *   1. Advance state of currently eating philosophers (eating timer countdown).
     *   2. Randomly make some thinking philosophers hungry.
     *   3. DQN Agent observes state s and chooses action a (philosopher index).
     *   4. Environment safety check: Both chopsticks must be available.
     *   5. Calculate reward r and nextState s'.
     *   6. Store transition in Replay Buffer and perform mini-batch training.
     */
    private void executeDqnStep() {
        episodeCount++;
        int n = numPhilosophers;

        // 1. Advance eating philosophers
        double completionReward = 0.0;
        for (int i = 0; i < n; i++) {
            Philosopher p = philosophers.get(i);
            if (p.getState() == Philosopher.State.EATING) {
                p.decrementEatingTicks();
                if (p.getEatingTicks() <= 0) {
                    // Finished eating: release chopsticks
                    int leftIdx = p.getLeftChopstickIndex(n);
                    int rightIdx = p.getRightChopstickIndex(n);
                    chopsticks.get(leftIdx).putDown();
                    chopsticks.get(rightIdx).putDown();
                    p.setHeldLeftChopstick(-1);
                    p.setHeldRightChopstick(-1);
                    p.setState(Philosopher.State.THINKING);
                    p.incrementMealsEaten();
                    completionReward += 5.0; // Bonus for meal completion
                    log(p.getName() + " finished EATING (Meal #" + p.getMealsEaten() + "). Released " + chopsticks.get(leftIdx).getName() + " & " + chopsticks.get(rightIdx).getName());
                }
            } else if (p.getState() == Philosopher.State.HUNGRY) {
                p.incrementWaitingTime();
            } else if (p.getState() == Philosopher.State.THINKING) {
                // Stochastic arrival of hunger (40% probability)
                if (random.nextDouble() < 0.40) {
                    p.setState(Philosopher.State.HUNGRY);
                }
            }
        }

        // 2. Observe State s
        double[] state = dqnAgent.encodeState(philosophers, chopsticks);

        // 3. Agent selects action a
        int action = dqnAgent.selectAction(state, isEvaluationMode);

        // Highlight selected philosopher
        for (int i = 0; i < n; i++) {
            philosophers.get(i).setDqnSelected(i == action);
        }

        Philosopher chosenPhil = philosophers.get(action);
        int leftChopIdx = chosenPhil.getLeftChopstickIndex(n);
        int rightChopIdx = chosenPhil.getRightChopstickIndex(n);

        // 4. Environment Safety & Reward Calculation
        double stepReward = 0.0;
        String actionLogMsg;

        if (chosenPhil.getState() == Philosopher.State.EATING) {
            // Inefficient action: philosopher is already eating
            stepReward = -2.0;
            actionLogMsg = "Selected " + chosenPhil.getName() + " (Already EATING). Penalty -2.0";
        } else if (chosenPhil.getState() == Philosopher.State.THINKING) {
            // Philosopher is not hungry yet
            stepReward = -1.0;
            actionLogMsg = "Selected " + chosenPhil.getName() + " (THINKING). Penalty -1.0";
        } else {
            // Philosopher is HUNGRY: verify chopstick availability (Safety Check)
            boolean leftAvail = chopsticks.get(leftChopIdx).isAvailable();
            boolean rightAvail = chopsticks.get(rightChopIdx).isAvailable();

            if (leftAvail && rightAvail) {
                // Safety Validated: Both chopsticks acquired
                chopsticks.get(leftChopIdx).pickUp(chosenPhil.getId());
                chopsticks.get(rightChopIdx).pickUp(chosenPhil.getId());
                chosenPhil.setHeldLeftChopstick(chopsticks.get(leftChopIdx).getId());
                chosenPhil.setHeldRightChopstick(chopsticks.get(rightChopIdx).getId());
                chosenPhil.setState(Philosopher.State.EATING);
                chosenPhil.setEatingTicks(2); // Eats for 2 ticks
                chosenPhil.resetWaitingTime();

                stepReward = 10.0; // Positive reward for conflict-free scheduling
                actionLogMsg = "Selected " + chosenPhil.getName() + " -> Acquired " + chopsticks.get(leftChopIdx).getName() + " & " + chopsticks.get(rightChopIdx).getName() + " (EATING). Reward +10.0";
            } else {
                // Resource Conflict: Neighbor holds a required chopstick
                stepReward = -5.0; // Penalty for resource conflict
                actionLogMsg = "Selected " + chosenPhil.getName() + " -> Resource Conflict! (" + chopsticks.get(leftChopIdx).getName() + " or " + chopsticks.get(rightChopIdx).getName() + " occupied). Action Rejected. Penalty -5.0";
            }
        }

        // Small penalty for hungry philosophers waiting to encourage fairness
        int hungryCount = 0;
        for (Philosopher p : philosophers) {
            if (p.getState() == Philosopher.State.HUNGRY) {
                hungryCount++;
            }
        }
        stepReward -= (hungryCount * 0.2);
        stepReward += completionReward;

        // 5. Observe nextState s'
        double[] nextState = dqnAgent.encodeState(philosophers, chopsticks);

        // 6. Record experience and train (unless in evaluation mode)
        if (!isEvaluationMode) {
            dqnAgent.recordExperience(state, action, stepReward, nextState, false);
            dqnAgent.trainStep();
        }

        // 7. Update UI
        updateDqnMetricLabels(chosenPhil.getName(), stepReward);
        updateTables();
        tableView.repaint();

        log("Episode " + episodeCount + ": " + actionLogMsg);
    }

    public void setPhilosopherCount(int count) {
        if (philCountCombo != null) {
            philCountCombo.setSelectedItem(count);
        }
    }
}
