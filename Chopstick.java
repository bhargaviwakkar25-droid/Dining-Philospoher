package diningphilosopher;

import java.util.concurrent.Semaphore;

/**
 * Represents a Chopstick shared between neighboring philosophers.
 * Encapsulates ownership, availability status, and a binary semaphore
 * used for mutual exclusion.
 */
public class Chopstick {

    private final int id; // 1-based: C1, C2, ...
    private volatile boolean held = false;
    private volatile int holderId = 0; // 0 = available, > 0 = philosopher ID
    private final Semaphore semaphore = new Semaphore(1, true);

    public Chopstick(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return "C" + id;
    }

    public synchronized boolean isAvailable() {
        return !held;
    }

    public synchronized boolean isHeld() {
        return held;
    }

    public synchronized int getHolderId() {
        return holderId;
    }

    public synchronized boolean tryPickUp(int philosopherId) {
        if (!held) {
            held = true;
            holderId = philosopherId;
            return true;
        }
        return false;
    }

    public synchronized void pickUp(int philosopherId) {
        held = true;
        holderId = philosopherId;
    }

    public synchronized void putDown() {
        held = false;
        holderId = 0;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public synchronized void reset() {
        held = false;
        holderId = 0;
        // Drain and reset semaphore permits to 1
        semaphore.drainPermits();
        semaphore.release();
    }

    public synchronized String getStatusText() {
        if (held && holderId > 0) {
            return "In Use (by P" + holderId + ")";
        }
        return "Available";
    }
}
