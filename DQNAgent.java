package diningphilosopher;

import java.util.List;
import java.util.Random;

public class DQNAgent {

    private int numPhilosophers;
    private int inputDim;
    private int actionDim;

    private NeuralNetwork qNetwork;
    private NeuralNetwork targetNetwork;
    private final ReplayBuffer replayBuffer;

    // Hyperparameters
    private double gamma = 0.90;          // Discount factor
    private double epsilon = 1.0;         // Initial exploration rate
    private double epsilonMin = 0.05;     // Minimum exploration rate
    private double epsilonDecay = 0.985;  // Decay factor per training step
    private double learningRate = 0.02;   // Alpha
    private int batchSize = 8;
    private int targetUpdateFreq = 10;
    private int trainStepCount = 0;

    // Latest statistics for GUI
    private double latestLoss = 0.0;
    private double cumulativeReward = 0.0;
    private double[] latestQValues;
    private final Random random = new Random();

    public DQNAgent(int numPhilosophers) {
        this.replayBuffer = new ReplayBuffer(1000);
        reinitialize(numPhilosophers);
    }

    public synchronized void reinitialize(int numPhilosophers) {
        this.numPhilosophers = numPhilosophers;
        this.actionDim = numPhilosophers;
        // Each philosopher: state(1) + waitTime(1) + mealsEaten(1) = 3 features
        // Each chopstick: available(1) = 1 feature
        this.inputDim = numPhilosophers * 3 + numPhilosophers;

        int hiddenDim = 16;
        this.qNetwork = new NeuralNetwork(inputDim, hiddenDim, actionDim);
        this.targetNetwork = new NeuralNetwork(inputDim, hiddenDim, actionDim);
        this.targetNetwork.copyWeightsFrom(qNetwork);

        this.epsilon = 1.0;
        this.trainStepCount = 0;
        this.latestLoss = 0.0;
        this.cumulativeReward = 0.0;
        this.latestQValues = new double[actionDim];
        this.replayBuffer.clear();
    }

    public double[] encodeState(List<Philosopher> philosophers, List<Chopstick> chopsticks) {
        double[] state = new double[inputDim];
        int idx = 0;

        // 1. Philosopher features
        for (int i = 0; i < numPhilosophers; i++) {
            Philosopher p = philosophers.get(i);
            // State: 0 = Thinking, 1 = Hungry, 2 = Eating, 3 = Waiting
            double stateVal = 0.0;
            switch (p.getState()) {
                case THINKING: stateVal = 0.0; break;
                case HUNGRY:   stateVal = 1.0; break;
                case EATING:   stateVal = 2.0; break;
                case WAITING:  stateVal = 3.0; break;
            }
            state[idx++] = stateVal / 3.0; // Normalized to [0, 1]
            state[idx++] = Math.min(1.0, p.getWaitingTime() / 10.0);
            state[idx++] = Math.min(1.0, p.getMealsEaten() / 10.0);
        }

        // 2. Chopstick availability features
        for (int i = 0; i < numPhilosophers; i++) {
            if (i < chopsticks.size()) {
                state[idx++] = chopsticks.get(i).isAvailable() ? 1.0 : 0.0;
            } else {
                state[idx++] = 0.0;
            }
        }

        return state;
    }

    public synchronized int selectAction(double[] state, boolean greedy) {
        latestQValues = qNetwork.forward(state);

        if (!greedy && random.nextDouble() < epsilon) {
            // Exploration: choose random philosopher
            return random.nextInt(actionDim);
        } else {
            // Exploitation: choose action with highest estimated Q-value
            int bestAction = 0;
            double maxQ = latestQValues[0];
            for (int a = 1; a < actionDim; a++) {
                if (latestQValues[a] > maxQ) {
                    maxQ = latestQValues[a];
                    bestAction = a;
                }
            }
            return bestAction;
        }
    }

    public synchronized void recordExperience(double[] state, int action, double reward, double[] nextState, boolean done) {
        cumulativeReward += reward;
        replayBuffer.add(state, action, reward, nextState, done);
    }


    public synchronized double trainStep() {
        if (replayBuffer.size() < batchSize) {
            return 0.0;
        }

        List<ReplayBuffer.Transition> batch = replayBuffer.sample(batchSize);
        double totalLoss = 0.0;

        for (ReplayBuffer.Transition t : batch) {
            double target;
            if (t.done) {
                target = t.reward;
            } else {
                // Bellman Optimality Equation: target = r + gamma * max_a' Q_target(s', a')
                double[] nextQ = targetNetwork.forward(t.nextState);
                double maxNextQ = nextQ[0];
                for (int a = 1; a < actionDim; a++) {
                    if (nextQ[a] > maxNextQ) {
                        maxNextQ = nextQ[a];
                    }
                }
                target = t.reward + gamma * maxNextQ;
            }

            // Train Q-network for the selected action
            double loss = qNetwork.trainStep(t.state, t.action, target, learningRate);
            totalLoss += loss;
        }

        latestLoss = totalLoss / batch.size();
        trainStepCount++;

        // Decay exploration rate epsilon
        if (epsilon > epsilonMin) {
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        // Periodically synchronize Target Network
        if (trainStepCount % targetUpdateFreq == 0) {
            targetNetwork.copyWeightsFrom(qNetwork);
        }

        return latestLoss;
    }

    public synchronized double getEpsilon() {
        return epsilon;
    }

    public synchronized void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public synchronized double getLatestLoss() {
        return latestLoss;
    }

    public synchronized double getCumulativeReward() {
        return cumulativeReward;
    }

    public synchronized void resetCumulativeReward() {
        this.cumulativeReward = 0.0;
    }

    public synchronized double[] getLatestQValues() {
        return latestQValues != null ? latestQValues.clone() : new double[actionDim];
    }

    public synchronized int getReplayBufferSize() {
        return replayBuffer.size();
    }
}
