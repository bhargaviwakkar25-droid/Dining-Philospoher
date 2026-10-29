package diningphilosopher;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Visual canvas for the Dining Philosophers simulation.
 * Dynamically renders the central circular dining table, noodle bowls,
 * chopsticks, philosopher avatars, status badges, and DQN highlight rings
 * for N = 1 to 5 philosophers.
 */
public class TableViewPanel extends JPanel {

    private List<Philosopher> philosophers;
    private List<Chopstick> chopsticks;
    private boolean deadlockDetected = false;

    public TableViewPanel(List<Philosopher> philosophers, List<Chopstick> chopsticks) {
        this.philosophers = philosophers;
        this.chopsticks = chopsticks;
        setBackground(new Color(245, 248, 252)); // Light-blue/neutral soft background
    }

    public synchronized void updateData(List<Philosopher> philosophers, List<Chopstick> chopsticks) {
        this.philosophers = philosophers;
        this.chopsticks = chopsticks;
        repaint();
    }

    public synchronized void setDeadlockDetected(boolean detected) {
        this.deadlockDetected = detected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Anti-aliasing for smooth rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;

        if (philosophers == null || philosophers.isEmpty()) {
            return;
        }

        int n = philosophers.size();

        // Calculate dynamic table radius based on available panel dimensions
        int maxRadiusByHeight = (height - 230) / 2;
        int maxRadiusByWidth = (width - 250) / 2;
        int tableRadius = Math.min(maxRadiusByHeight, maxRadiusByWidth);
        if (tableRadius > 115) tableRadius = 115;
        if (tableRadius < 85) tableRadius = 85;

        int centerY = height / 2 + 35;
        if (centerY - tableRadius < 135) {
            centerY = tableRadius + 135;
        }

        // 1. Draw Wooden Dining Table
        drawTable(g2, centerX, centerY, tableRadius);

        // 2. Draw Chairs, Philosophers, and Bowls
        for (int i = 0; i < n; i++) {
            double angle = (n == 1) ? -Math.PI / 2 : (2 * Math.PI * i / n - Math.PI / 2);
            Philosopher p = philosophers.get(i);

            // Chair position
            int chairDist = tableRadius + 58;
            int chairX = (int) (centerX + chairDist * Math.cos(angle));
            int chairY = (int) (centerY + chairDist * Math.sin(angle));
            drawChair(g2, chairX, chairY, angle);

            // Philosopher avatar position
            int pDist = tableRadius + 46;
            int pX = (int) (centerX + pDist * Math.cos(angle));
            int pY = (int) (centerY + pDist * Math.sin(angle));
            drawPhilosopher(g2, pX, pY, p, angle);

            // Noodle bowl in front of philosopher
            int bowlDist = (int) (tableRadius * 0.62);
            int bowlX = (int) (centerX + bowlDist * Math.cos(angle));
            int bowlY = (int) (centerY + bowlDist * Math.sin(angle));
            drawBowl(g2, bowlX, bowlY);
        }

        // 3. Draw Chopsticks and Labels
        if (chopsticks != null) {
            int chopCount = chopsticks.size();
            for (int i = 0; i < chopCount; i++) {
                Chopstick c = chopsticks.get(i);
                double chopAngle;
                if (n == 1) {
                    chopAngle = 0; // right of table for single philosopher
                } else {
                    chopAngle = 2 * Math.PI * i / n - Math.PI / 2 + Math.PI / n;
                }
                drawChopstick(g2, c, chopAngle, centerX, centerY, tableRadius);
            }
        }

        // 4. Draw Status Badges for Philosophers
        drawBadges(g2, centerX, centerY, tableRadius, n);

        // 5. Draw Deadlock Warning Banner if deadlock is reached
        if (deadlockDetected) {
            drawDeadlockBanner(g2, centerX, centerY);
        } else if (n == 1) {
            drawN1Notice(g2, centerX, centerY);
        }
    }

    private void drawTable(Graphics2D g2, int cx, int cy, int radius) {
        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillOval(cx - radius + 4, cy - radius + 6, radius * 2, radius * 2);

        // Outer wooden rim
        g2.setColor(new Color(152, 98, 48));
        g2.fillOval(cx - radius - 3, cy - radius - 3, (radius + 3) * 2, (radius + 3) * 2);

        // Rich wood table surface
        g2.setColor(new Color(192, 133, 76));
        g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Subtle concentric wood grain rings
        g2.setColor(new Color(180, 120, 65, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - (int)(radius * 0.75), cy - (int)(radius * 0.75), (int)(radius * 1.5), (int)(radius * 1.5));
        g2.drawOval(cx - (int)(radius * 0.45), cy - (int)(radius * 0.45), (int)(radius * 0.9), (int)(radius * 0.9));
    }

    private void drawChair(Graphics2D g2, int x, int y, double angle) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle + Math.PI / 2);

        g2.setColor(new Color(145, 92, 45));
        g2.fillRoundRect(-28, -16, 56, 32, 12, 12);

        g2.setColor(new Color(105, 62, 28));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(-28, -16, 56, 32, 12, 12);

        g2.setColor(new Color(175, 115, 60));
        g2.fillRoundRect(-22, -12, 44, 24, 8, 8);

        g2.setTransform(old);
    }

    private void drawPhilosopher(Graphics2D g2, int x, int y, Philosopher p, double angle) {
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);

        // Highlight ring if selected by DQN
        if (p.isDqnSelected()) {
            g2.setColor(new Color(156, 39, 176, 120));
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawOval(-30, -38, 60, 72);
        }

        int id = p.getId();
        boolean isFemale = (id == 2 || id == 5);

        // Shirt Colors
        Color shirtColor;
        Color hairColor;
        switch (id) {
            case 1:
                shirtColor = new Color(52, 73, 94); // Dark slate blue
                hairColor = new Color(40, 30, 25);
                break;
            case 2:
                shirtColor = new Color(240, 128, 128); // Coral pink
                hairColor = new Color(105, 55, 30);
                break;
            case 3:
                shirtColor = new Color(33, 115, 185); // Blue
                hairColor = new Color(45, 30, 25);
                break;
            case 4:
                shirtColor = new Color(38, 135, 95); // Forest green
                hairColor = new Color(75, 50, 35);
                break;
            case 5:
            default:
                shirtColor = new Color(245, 185, 65); // Warm yellow
                hairColor = new Color(85, 45, 25);
                break;
        }

        // 1. Shoulders / Shirt
        g2.setColor(shirtColor);
        g2.fillRoundRect(-24, 2, 48, 30, 16, 16);
        g2.setColor(new Color(40, 40, 40));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(-24, 2, 48, 30, 16, 16);

        // White Collar
        g2.setColor(Color.WHITE);
        g2.fillPolygon(new int[]{-6, 0, 6}, new int[]{2, 8, 2}, 3);

        // 2. Head (Face)
        Color skinColor = new Color(255, 228, 205);
        g2.setColor(skinColor);
        g2.fillOval(-19, -28, 38, 36);
        g2.setColor(new Color(70, 50, 40));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(-19, -28, 38, 36);

        // 3. Hair
        g2.setColor(hairColor);
        if (isFemale) {
            g2.fillArc(-21, -33, 42, 34, 15, 150);
            g2.fillRoundRect(-22, -18, 7, 24, 6, 6);
            g2.fillRoundRect(15, -18, 7, 24, 6, 6);
            if (id == 2) {
                g2.fillOval(17, -30, 13, 15);
            }
        } else {
            g2.fillArc(-21, -33, 42, 32, 10, 160);
        }

        // 4. Facial Features
        g2.setColor(new Color(60, 40, 30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(-12, -18, -5, -19);
        g2.drawLine(5, -19, 12, -18);

        Philosopher.State state = p.getState();
        if (state == Philosopher.State.EATING) {
            // Happy closed eyes (^ ^)
            g2.setColor(new Color(40, 30, 20));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawArc(-13, -17, 8, 7, 25, 130);
            g2.drawArc(5, -17, 8, 7, 25, 130);

            // Happy open smiling mouth
            g2.setColor(new Color(220, 45, 45));
            g2.fillArc(-6, -8, 12, 11, 180, 180);

            // Joyful sparkle rays
            g2.setColor(new Color(255, 195, 0));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(22, -28, 31, -35);
            g2.drawLine(26, -17, 35, -17);
            g2.drawLine(22, -6, 31, 1);
        } else if (state == Philosopher.State.HUNGRY) {
            // Big hungry eyes
            g2.setColor(new Color(40, 30, 20));
            g2.fillOval(-11, -17, 7, 8);
            g2.fillOval(4, -17, 7, 8);
            g2.setColor(Color.WHITE);
            g2.fillOval(-10, -16, 3, 3);
            g2.fillOval(5, -16, 3, 3);

            // Open mouth
            g2.setColor(new Color(180, 40, 40));
            g2.fillOval(-5, -8, 10, 7);
        } else if (state == Philosopher.State.WAITING) {
            // Worried / waiting eyes
            g2.setColor(new Color(40, 30, 20));
            g2.fillOval(-11, -16, 6, 7);
            g2.fillOval(5, -16, 6, 7);
            // Straight/puzzled mouth
            g2.setColor(new Color(80, 30, 30));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawLine(-5, -6, 5, -6);
        } else {
            // Calm thinking eyes
            g2.setColor(new Color(40, 30, 20));
            g2.fillOval(-10, -16, 6, 7);
            g2.fillOval(4, -16, 6, 7);
            g2.setColor(Color.WHITE);
            g2.fillOval(-9, -15, 2, 2);
            g2.fillOval(5, -15, 2, 2);

            // Gentle smile
            g2.setColor(new Color(60, 30, 30));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawArc(-5, -7, 10, 5, 190, 160);
        }

        // Blush cheeks
        g2.setColor(new Color(255, 150, 150, 140));
        g2.fillOval(-16, -10, 7, 5);
        g2.fillOval(9, -10, 7, 5);

        // Hands with chopsticks when eating
        if (state == Philosopher.State.EATING) {
            g2.setColor(skinColor);
            g2.fillOval(-6, 12, 12, 10);
            g2.setColor(new Color(225, 30, 30));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(-2, 16, -14, 26);
            g2.drawLine(2, 16, -8, 26);
        }

        g2.setTransform(old);
    }

    private void drawBowl(Graphics2D g2, int x, int y) {
        // Ceramic bowl
        g2.setColor(new Color(110, 130, 150));
        g2.fillOval(x - 20, y - 13, 40, 26);
        g2.setColor(new Color(75, 95, 115));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x - 20, y - 13, 40, 26);

        // Noodles
        g2.setColor(new Color(255, 215, 65));
        g2.fillOval(x - 15, y - 9, 30, 18);

        // Noodle squiggles
        g2.setColor(new Color(230, 175, 30));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawArc(x - 10, y - 6, 10, 7, 30, 220);
        g2.drawArc(x - 3, y - 4, 12, 6, 20, 200);

        // Green garnish
        g2.setColor(new Color(55, 165, 55));
        g2.fillOval(x - 2, y - 5, 5, 5);

        // Red chili bits
        g2.setColor(new Color(225, 45, 45));
        g2.fillOval(x - 6, y - 1, 3, 3);
        g2.fillOval(x + 3, y - 2, 3, 3);
    }

    private boolean isChopstickBeingEatenWith(Chopstick c) {
        if (!c.isHeld()) {
            return false;
        }
        int holderId = c.getHolderId();
        if (philosophers != null && holderId >= 1 && holderId <= philosophers.size()) {
            Philosopher p = philosophers.get(holderId - 1);
            return p.getState() == Philosopher.State.EATING;
        }
        return false;
    }

    private void drawChopstick(Graphics2D g2, Chopstick c, double angle, int cx, int cy, int radius) {
        boolean isEating = isChopstickBeingEatenWith(c);

        int startR = (int) (radius * 0.58);
        int endR = (int) (radius * 0.94);

        int x1 = (int) (cx + startR * Math.cos(angle));
        int y1 = (int) (cy + startR * Math.sin(angle));
        int x2 = (int) (cx + endR * Math.cos(angle));
        int y2 = (int) (cy + endR * Math.sin(angle));

        double perpAngle = angle + Math.PI / 2;
        int offset = 3;
        int ox = (int) (offset * Math.cos(perpAngle));
        int oy = (int) (offset * Math.sin(perpAngle));

        if (isEating) {
            // Highlighted in Red ONLY when actively used for eating
            g2.setColor(new Color(225, 30, 30));
            g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else {
            // Dark Wood Brown at all other times
            g2.setColor(new Color(90, 50, 25));
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }

        // Draw exactly ONE single chopstick line (N philosophers = N chopsticks)
        g2.drawLine(x1, y1, x2, y2);

        // Chopstick Label C1, C2, ...
        int labelR = (int) (radius * 1.07);
        int lx = (int) (cx + labelR * Math.cos(angle));
        int ly = (int) (cy + labelR * Math.sin(angle));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(33, 37, 41));
        String label = "C" + c.getId();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, lx - fm.stringWidth(label) / 2, ly + fm.getAscent() / 2 - 2);
    }

    private void drawBadges(Graphics2D g2, int cx, int cy, int radius, int n) {
        int pDist = radius + 46;

        for (int i = 0; i < n; i++) {
            Philosopher p = philosophers.get(i);
            double angle = (n == 1) ? -Math.PI / 2 : (2 * Math.PI * i / n - Math.PI / 2);

            int pX = (int) (cx + pDist * Math.cos(angle));
            int pY = (int) (cy + pDist * Math.sin(angle));

            int bx;
            int by;

            if (n == 5) {
                switch (i) {
                    case 0: // P1: Top Center -> Badge directly above head
                        bx = cx;
                        by = pY - 56;
                        break;
                    case 1: // P2: Top Right -> Badge safely above head, shifted slightly outward
                        bx = pX + 16;
                        by = pY - 56;
                        break;
                    case 2: // P3: Bottom Right -> Badge safely below shoulders
                        bx = pX + 12;
                        by = pY + 54;
                        break;
                    case 3: // P4: Bottom Left -> Badge safely below shoulders
                        bx = pX - 12;
                        by = pY + 54;
                        break;
                    case 4: // P5: Top Left -> Badge safely above head, shifted slightly outward
                    default:
                        bx = pX - 16;
                        by = pY - 56;
                        break;
                }
            } else {
                // Generalized for N = 1, 2, 3, 4
                if (Math.sin(angle) < -0.1) {
                    // Upper half: place badge above head
                    bx = pX + (int) (20 * Math.cos(angle));
                    by = pY - 56;
                } else {
                    // Lower half: place badge below shoulders
                    bx = pX + (int) (20 * Math.cos(angle));
                    by = pY + 54;
                }
            }

            drawStatusBadge(g2, p, bx, by);
        }
    }

    private void drawStatusBadge(Graphics2D g2, Philosopher p, int bx, int by) {
        int badgeW = 90;
        int badgeH = 42;
        int x = bx - badgeW / 2;
        int y = by - badgeH / 2;

        // Keep inside bounds
        if (x < 6) x = 6;
        if (x + badgeW > getWidth() - 6) x = getWidth() - badgeW - 6;
        if (y < 6) y = 6;
        if (y + badgeH > getHeight() - 6) y = getHeight() - badgeH - 6;
        int centerX = x + badgeW / 2;

        Philosopher.State state = p.getState();
        Color bg = state.getBgColor();
        Color border = state.getBorderColor();
        Color textColor = state.getPrimaryColor();

        if (p.isDqnSelected()) {
            bg = new Color(243, 229, 245);
            border = new Color(156, 39, 176);
            textColor = new Color(106, 27, 154);
        }

        // Badge rounded box
        g2.setColor(bg);
        g2.fillRoundRect(x, y, badgeW, badgeH, 12, 12);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(x, y, badgeW, badgeH, 12, 12);

        // Line 1: Philosopher ID
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(33, 37, 41));
        FontMetrics fm1 = g2.getFontMetrics();
        String idText = p.getName();
        g2.drawString(idText, centerX - fm1.stringWidth(idText) / 2, y + 16);

        // Line 2: Status text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(textColor);
        FontMetrics fm2 = g2.getFontMetrics();
        String statusText = p.getDisplayStatus();
        g2.drawString(statusText, centerX - fm2.stringWidth(statusText) / 2, y + 33);
    }

    private void drawDeadlockBanner(Graphics2D g2, int cx, int cy) {
        int w = 220;
        int h = 48;
        int x = cx - w / 2;
        int y = cy - h / 2;

        g2.setColor(new Color(211, 47, 47, 230));
        g2.fillRoundRect(x, y, w, h, 14, 14);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String msg1 = "DEADLOCK DETECTED";
        g2.drawString(msg1, cx - fm.stringWidth(msg1) / 2, y + 20);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm2 = g2.getFontMetrics();
        String msg2 = "Circular Wait Reached";
        g2.drawString(msg2, cx - fm2.stringWidth(msg2) / 2, y + 38);
    }

    private void drawN1Notice(Graphics2D g2, int cx, int cy) {
        int w = 180;
        int h = 38;
        int x = cx - w / 2;
        int y = cy - h / 2;

        g2.setColor(new Color(33, 150, 243, 220));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 12, 12);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String msg = "N = 1 Philosopher";
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, y + 17);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        FontMetrics fm2 = g2.getFontMetrics();
        String msg2 = "Needs 2 chopsticks to eat";
        g2.drawString(msg2, cx - fm2.stringWidth(msg2) / 2, y + 30);
    }
}
