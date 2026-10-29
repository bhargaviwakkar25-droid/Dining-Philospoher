package diningphilosopher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Experience Replay Buffer for Deep Q-Learning.
 * Breaks correlation between consecutive experiences by storing transitions
 * (s, a, r, s', done) and sampling uniformly distributed minibatches for training.
 */
public class ReplayBuffer {

    public static class Transition {
        public final double[] state;
        public final int action;
        public final double reward;
        public final double[] nextState;
        public final boolean done;

        public Transition(double[] state, int action, double reward, double[] nextState, boolean done) {
            this.state = state.clone();
            this.action = action;
            this.reward = reward;
            this.nextState = nextState.clone();
            this.done = done;
        }
    }

    private final int capacity;
    private final List<Transition> buffer;
    private int writeIndex = 0;
    private final Random random = new Random();

    public ReplayBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayList<>(capacity);
    }

    /**
     * Appends a new experience transition into the circular buffer.
     */
    public synchronized void add(double[] state, int action, double reward, double[] nextState, boolean done) {
        Transition t = new Transition(state, action, reward, nextState, done);
        if (buffer.size() < capacity) {
            buffer.add(t);
        } else {
            buffer.set(writeIndex, t);
            writeIndex = (writeIndex + 1) % capacity;
        }
    }

    /**
     * Randomly samples a minibatch of transitions for training.
     *
     * @param batchSize number of transitions to sample
     * @return list of sampled transitions
     */
    public synchronized List<Transition> sample(int batchSize) {
        int n = buffer.size();
        int sampleCount = Math.min(batchSize, n);
        List<Transition> batch = new ArrayList<>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            int idx = random.nextInt(n);
            batch.add(buffer.get(idx));
        }

        return batch;
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized void clear() {
        buffer.clear();
        writeIndex = 0;
    }
}
