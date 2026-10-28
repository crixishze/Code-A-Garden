package codeagarden;

import java.awt.*;
import javax.swing.*;

public class Store extends JPanel {

    // price of each farm upgrade: 3x3 to 4x4, 4x4 to 5x5, 5x5 to 6x6
    private static final int[] UPGRADE_COSTS = {150, 400, 1000};

    private final BottomBar bottomBar;
    private final Inventory inventory;
    private final FarmPlot plot;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel upgradeLabel = new JLabel();
    private final JButton upgradeButton = new JButton("Buy");

    public Store(BottomBar bottomBar, Inventory inventory, FarmPlot plot) {
        this.bottomBar = bottomBar;
        this.inventory = inventory;
        this.plot = plot;

        setLayout(new GridLayout(0, 1, 5, 5));
        setBackground(Color.decode("#fada5e"));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("SHOP", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title);

        add(makeHeader("Seeds"));
        add(makeSeedRow(new Seed("Carrot", 10, 3, 20)));
        add(makeSeedRow(new Seed("Corn", 20, 5, 45)));
        add(makeSeedRow(new Seed("Tomato", 30, 8, 70)));
        add(makeSeedRow(new Seed("Pumpkin", 50, 12, 120)));

        add(makeHeader("Upgrades"));
        add(makeUpgradeRow());

        add(status); // shows "Bought ..." or "Not enough money"
    }

    private JLabel makeHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        return header;
    }

    private JPanel makeSeedRow(Seed seed) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(210, 180, 140));

        JLabel label = new JLabel("  " + seed.getName() + " - $" + seed.getPrice());
        row.add(label, BorderLayout.CENTER);

        JButton buy = new JButton("Buy");
        buy.addActionListener(e -> buySeed(seed));
        row.add(buy, BorderLayout.EAST);

        return row;
    }

    private JPanel makeUpgradeRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(210, 180, 140));

        row.add(upgradeLabel, BorderLayout.CENTER);

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
            upgradeLabel.setText("  Bigger Farm: " + n + "x" + n + " to " + (n + 1) + "x" + (n + 1)
                    + " - $" + upgradeCost());
            upgradeButton.setText("Buy");
            upgradeButton.setEnabled(true);
        } else {
            upgradeLabel.setText("  Farm size: " + n + "x" + n + " (max)");
            upgradeButton.setText("Maxed");
            upgradeButton.setEnabled(false);
        }
    }

    private void buySeed(Seed seed) {
        if (bottomBar.spend(seed.getPrice())) {
            inventory.addSeed(seed, 1);
            status.setText("Bought 1 " + seed.getName() + " seed!");
        } else {
            status.setText("Not enough money for " + seed.getName() + ".");
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
            status.setText("Farm upgraded to " + n + "x" + n + "!");
            refreshUpgradeRow();
        } else {
            status.setText("Not enough money for the upgrade.");
            Toolkit.getDefaultToolkit().beep();
        }
    }
}