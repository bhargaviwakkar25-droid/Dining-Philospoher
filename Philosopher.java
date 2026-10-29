package diningphilosopher;

import java.awt.Color;
public class Philosopher {

    public enum State {
        THINKING("Thinking", new Color(46, 125, 50), new Color(232, 245, 233), new Color(76, 175, 80)),
        HUNGRY("Hungry", new Color(245, 127, 23), new Color(255, 249, 196), new Color(251, 192, 45)),
        EATING("Eating", new Color(21, 101, 192), new Color(227, 242, 253), new Color(33, 150, 243)),
        WAITING("Waiting", new Color(198, 40, 40), new Color(255, 235, 238), new Color(229, 57, 53));

        private final String displayName;
        private final Color primaryColor;
        private final Color bgColor;
        private final Color borderColor;

        State(String displayName, Color primaryColor, Color bgColor, Color borderColor) {
            this.displayName = displayName;
            this.primaryColor = primaryColor;
            this.bgColor = bgColor;
            this.borderColor = borderColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Color getPrimaryColor() {
            return primaryColor;
        }

        public Color getBgColor() {
            return bgColor;
        }

        public Color getBorderColor() {
            return borderColor;
        }
    }

    private final int id; // 1-based: P1, P2, ...
    private volatile State state;
    private volatile String customStateText; // e.g. "Holding C1" in Deadlock
    private int heldLeftChopstick = -1;
    private int heldRightChopstick = -1;
    private int waitingTime = 0;
    private int mealsEaten = 0;
    private int eatingTicks = 0;
    private boolean dqnSelected = false;

    public Philosopher(int id) {
        this.id = id;
        this.state = State.THINKING;
        this.customStateText = null;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return "P" + id;
    }

    public State getState() {
        return state;
    }

    public synchronized void setState(State state) {
        this.state = state;
        this.customStateText = null;
    }

    public String getDisplayStatus() {
        if (customStateText != null && !customStateText.isEmpty()) {
            return customStateText;
        }
        return state.getDisplayName();
    }

    public synchronized void setCustomStateText(String text) {
        this.customStateText = text;
    }

    public int getHeldLeftChopstick() {
        return heldLeftChopstick;
    }

    public synchronized void setHeldLeftChopstick(int chopstickId) {
        this.heldLeftChopstick = chopstickId;
    }

    public int getHeldRightChopstick() {
        return heldRightChopstick;
    }

    public synchronized void setHeldRightChopstick(int chopstickId) {
        this.heldRightChopstick = chopstickId;
    }

    public boolean holdsAnyChopstick() {
        return heldLeftChopstick != -1 || heldRightChopstick != -1;
    }

    public boolean holdsBothChopsticks() {
        return heldLeftChopstick != -1 && heldRightChopstick != -1;
    }
    
    public int getLeftChopstickIndex(int totalPhilosophers) {
        if (totalPhilosophers <= 1) return 0;
        return (id - 2 + totalPhilosophers) % totalPhilosophers;
    }

    public int getRightChopstickIndex(int totalPhilosophers) {
        if (totalPhilosophers <= 1) return 0;
        return (id - 1) % totalPhilosophers;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void incrementWaitingTime() {
        this.waitingTime++;
    }

    public void resetWaitingTime() {
        this.waitingTime = 0;
    }

    public int getMealsEaten() {
        return mealsEaten;
    }

    public void incrementMealsEaten() {
        this.mealsEaten++;
    }

    public int getEatingTicks() {
        return eatingTicks;
    }

    public void setEatingTicks(int ticks) {
        this.eatingTicks = ticks;
    }

    public void decrementEatingTicks() {
        if (this.eatingTicks > 0) {
            this.eatingTicks--;
        }
    }

    public boolean isDqnSelected() {
        return dqnSelected;
    }

    public void setDqnSelected(boolean dqnSelected) {
        this.dqnSelected = dqnSelected;
    }

    public synchronized void reset() {
        this.state = State.THINKING;
        this.customStateText = null;
        this.heldLeftChopstick = -1;
        this.heldRightChopstick = -1;
        this.waitingTime = 0;
        this.mealsEaten = 0;
        this.eatingTicks = 0;
        this.dqnSelected = false;
    }
}
