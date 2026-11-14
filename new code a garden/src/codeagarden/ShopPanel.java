package codeagarden;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** The shop screen: buy seeds and farm upgrades. Money and seeds live in GameSession. */
public class ShopPanel extends JPanel {

    private static final int BUY_BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 36;
    private static final float BUTTON_FONT_SIZE = 14f;
    private static final int MESSAGE_MILLIS = 2500;

    private final GameSession session = GameSession.getInstance();
    private final Runnable onMoneyChanged;
    private final Runnable onFarmExpanded;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel upgradeLabel = UIFactory.createLabel("", 16f, Theme.WOOD_DARK);
    private final JButton upgradeButton = UIFactory.createButton("Buy", BUY_BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
    private final Timer messageTimer;

    /**
     * onMoneyChanged runs after any purchase (so the bottom bar updates).
     * onFarmExpanded runs after a farm upgrade (so the farm screen shows the new plots).
     */
    public ShopPanel(Runnable onMoneyChanged, Runnable onFarmExpanded) {
        this.onMoneyChanged = onMoneyChanged;
        this.onFarmExpanded = onFarmExpanded;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.WOOD_DARK);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(UIFactory.createTitle("SHOP"));
        add(Box.createVerticalStrut(12));

        add(UIFactory.createHeader("Seeds"));
        add(Box.createVerticalStrut(6));
        List<Seed> seeds = SeedFactory.createAllSeeds();
        for (int i = 0; i < seeds.size(); i++) {
            add(createSeedRow(seeds.get(i)));
            add(Box.createVerticalStrut(i < seeds.size() - 1 ? 8 : 16));
        }

        add(UIFactory.createHeader("Upgrades"));
        add(Box.createVerticalStrut(6));
        add(createUpgradeRow());
        add(Box.createVerticalStrut(16));

        status.setFont(Theme.PIXEL.deriveFont(14f));
        status.setForeground(Theme.GOOD);
        status.setAlignmentX(LEFT_ALIGNMENT);
        status.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 24));
        add(status);   // shows "Bought ..." or "Not enough money"

        messageTimer = new Timer(MESSAGE_MILLIS, e -> status.setText(" "));
        messageTimer.setRepeats(false);
    }

    private void showStatus(String text, Color color) {
        status.setForeground(color);
        status.setText(text);
        messageTimer.restart();
    }

    private JPanel createSeedRow(Seed seed) {
        JPanel row = UIFactory.createRow(false);
        row.add(UIFactory.createLabel(seed.getName() + " - $" + seed.getPrice(), 16f, Theme.WOOD_DARK), BorderLayout.CENTER);
        JButton buy = UIFactory.createButton("Buy", BUY_BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
        buy.addActionListener(e -> buySeed(seed));
        row.add(buy, BorderLayout.EAST);
        return row;
    }

    private JPanel createUpgradeRow() {
        JPanel row = UIFactory.createRow(false);
        row.add(upgradeLabel, BorderLayout.CENTER);
        upgradeButton.addActionListener(e -> buyUpgrade());
        row.add(upgradeButton, BorderLayout.EAST);
        refreshUpgradeRow();
        return row;
    }

    // updates the upgrade row's text and button for the current farm size
    private void refreshUpgradeRow() {
        Farm farm = session.getFarm();
        int n = farm.getSize();
        if (farm.canExpand()) {
            upgradeLabel.setText("Bigger Farm: " + n + "x" + n + " to " + (n + 1) + "x" + (n + 1)
                    + " - $" + farm.getUpgradeCost());
            upgradeButton.setText("Buy");
            UIFactory.setButtonEnabled(upgradeButton, true);
        } else {
            upgradeLabel.setText("Farm size: " + n + "x" + n + " (max)");
            upgradeButton.setText("Maxed");
            UIFactory.setButtonEnabled(upgradeButton, false);
        }
    }

    private void buySeed(Seed seed) {
        if (session.spend(seed.getPrice())) {
            session.getInventory().addSeed(seed, 1);
            showStatus("Bought 1 " + seed.getName() + " seed!", Theme.GOOD);
            onMoneyChanged.run();
        } else {
            showStatus("Not enough money for " + seed.getName() + ".", Theme.BAD);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void buyUpgrade() {
        Farm farm = session.getFarm();
        if (!farm.canExpand()) {
            return;
        }
        if (session.spend(farm.getUpgradeCost())) {
            farm.expand();
            onFarmExpanded.run();
            int n = farm.getSize();
            showStatus("Farm upgraded to " + n + "x" + n + "!", Theme.GOOD);
            refreshUpgradeRow();
            onMoneyChanged.run();
        } else {
            showStatus("Not enough money for the upgrade.", Theme.BAD);
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
