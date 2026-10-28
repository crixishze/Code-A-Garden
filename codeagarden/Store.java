package codeagarden;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class Store extends JPanel {

    // price of each farm upgrade: 3x3 to 4x4, 4x4 to 5x5, 5x5 to 6x6
    private static final int[] UPGRADE_COSTS = {150, 400, 1000};

    // ---------- palette ----------
    private static final Color WOOD_DARK    = Color.decode("#5b3a1e");
    private static final Color WOOD_LIGHT   = Color.decode("#a0713f");
    private static final Color PARCHMENT    = Color.decode("#f3dfa2");
    private static final Color TITLE_ORANGE = Color.decode("#ff8c1a");
    private static final Color BTN_GREEN    = Color.decode("#4cc23a");
    private static final Color BTN_HOVER    = Color.decode("#6fe05a");
    private static final Color BTN_BORDER   = Color.decode("#1e6b17");
    private static final Color BTN_OFF      = Color.decode("#8a8a8a");
    private static final Color GOOD         = Color.decode("#8dff7a");
    private static final Color BAD          = Color.decode("#ff6b5e");

    // font
    private static final Font PIXEL = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private final BottomBar bottomBar;
    private final Inventory inventory;
    private final FarmPlot plot;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel upgradeLabel = new JLabel();
    private final JButton upgradeButton = new JButton("Buy");

    // ---------- status fade ----------
    private Timer fadeTimer;
    private Color statusColor = GOOD;
    private int alpha = 255;

    public Store(BottomBar bottomBar, Inventory inventory, FarmPlot plot) {
        this.bottomBar = bottomBar;
        this.inventory = inventory;
        this.plot = plot;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("SHOP", SwingConstants.CENTER);
        title.setFont(PIXEL.deriveFont(32f));
        title.setForeground(TITLE_ORANGE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        add(title);
        add(Box.createVerticalStrut(12));

        add(makeHeader("Seeds"));
        add(Box.createVerticalStrut(6));
        add(makeSeedRow(new Seed("Carrot", 10, 3, 20)));
        add(Box.createVerticalStrut(8));
        add(makeSeedRow(new Seed("Corn", 20, 5, 45)));
        add(Box.createVerticalStrut(8));
        add(makeSeedRow(new Seed("Tomato", 30, 8, 70)));
        add(Box.createVerticalStrut(8));
        add(makeSeedRow(new Seed("Pumpkin", 50, 12, 120)));
        add(Box.createVerticalStrut(16));

        add(makeHeader("Upgrades"));
        add(Box.createVerticalStrut(6));
        add(makeUpgradeRow());
        add(Box.createVerticalStrut(16));

        status.setFont(PIXEL.deriveFont(14f));
        status.setForeground(GOOD);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        add(status); // shows "Bought ..." or "Not enough money"
    }

    // ---------- status helper ----------
    // shows a message, keeps it visible for 1.5s, then fades it out
    private void showStatus(String text, Color color) {
        if (fadeTimer != null) fadeTimer.stop(); // cancel any fade in progress

        statusColor = color;
        alpha = 255;
        status.setForeground(statusColor);
        status.setText(text);

        fadeTimer = new Timer(30, e -> {
            alpha -= 5;
            if (alpha <= 0) {
                status.setText(" ");
                ((Timer) e.getSource()).stop();
            } else {
                status.setForeground(new Color(
                        statusColor.getRed(),
                        statusColor.getGreen(),
                        statusColor.getBlue(),
                        alpha));
            }
        });
        fadeTimer.setInitialDelay(0700); // fully visible for 1.0s first
        fadeTimer.start();
    }

    private JPanel makeRowPanel() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(PARCHMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WOOD_LIGHT, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12))));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void styleButton(JButton b) {
        b.setFont(PIXEL.deriveFont(14f));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(BTN_GREEN);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 3));
        b.setPreferredSize(new Dimension(90, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(BTN_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(BTN_GREEN);
            }
        });
    }

    private JLabel makeHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(PIXEL.deriveFont(20f));
        header.setForeground(PARCHMENT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    // ---------- builders ----------
    private JPanel makeSeedRow(Seed seed) {
        JPanel row = makeRowPanel();

        JLabel label = new JLabel(seed.getName() + " - $" + seed.getPrice());
        label.setFont(PIXEL.deriveFont(16f));
        label.setForeground(WOOD_DARK);
        row.add(label, BorderLayout.CENTER);

        JButton buy = new JButton("Buy");
        styleButton(buy);
        buy.addActionListener(e -> buySeed(seed));
        row.add(buy, BorderLayout.EAST);

        return row;
    }

    private JPanel makeUpgradeRow() {
        JPanel row = makeRowPanel();

        upgradeLabel.setFont(PIXEL.deriveFont(16f));
        upgradeLabel.setForeground(WOOD_DARK);
        row.add(upgradeLabel, BorderLayout.CENTER);

        styleButton(upgradeButton);
        upgradeButton.addActionListener(e -> buyUpgrade());
        row.add(upgradeButton, BorderLayout.EAST);

        refreshUpgradeRow();
        return row;
    }

    private int upgradeCost() {
        return UPGRADE_COSTS[plot.getGridSize() - FarmPlot.START_SIZE];
    }

    // updates the upgrade row's text and button for the current farm size
    private void refreshUpgradeRow() {
        int n = plot.getGridSize();
        if (plot.canExpand()) {
            upgradeLabel.setText("Bigger Farm: " + n + "x" + n + " to " + (n + 1) + "x" + (n + 1)
                    + " - $" + upgradeCost());
            upgradeButton.setText("Buy");
            upgradeButton.setEnabled(true);
            upgradeButton.setBackground(BTN_GREEN);
        } else {
            upgradeLabel.setText("Farm size: " + n + "x" + n + " (max)");
            upgradeButton.setText("Maxed");
            upgradeButton.setEnabled(false);
            upgradeButton.setBackground(BTN_OFF);
        }
    }

    private void buySeed(Seed seed) {
        if (bottomBar.spend(seed.getPrice())) {
            inventory.addSeed(seed, 1);
            showStatus("Bought 1 " + seed.getName() + " seed!", GOOD);
        } else {
            showStatus("Not enough money for " + seed.getName() + ".", BAD);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void buyUpgrade() {
        if (!plot.canExpand()) {
            return;
        }
        if (bottomBar.spend(upgradeCost())) {
            plot.expand();
            int n = plot.getGridSize();
            showStatus("Farm upgraded to " + n + "x" + n + "!", GOOD);
            refreshUpgradeRow();
        } else {
            showStatus("Not enough money for the upgrade.", BAD);
            Toolkit.getDefaultToolkit().beep();
        }
    }
}