package codeagarden.ui;

import codeagarden.game.GameSession;
import codeagarden.game.Seed;
import codeagarden.game.SeedFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** The shop screen: buy seeds, 1 or 10 at a time. Money and seeds live in GameSession. */
public class ShopPanel extends JPanel {

    private static final int BUY_BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 36;
    private static final float BUTTON_FONT_SIZE = 14f;
    private static final int MESSAGE_MILLIS = 2500;
    private static final int BULK_AMOUNT = 10;

    private final GameSession session = GameSession.getInstance();
    private final Runnable onMoneyChanged;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final Timer messageTimer;

    /** onMoneyChanged runs after any purchase (so the bottom bar updates). */
    public ShopPanel(Runnable onMoneyChanged) {
        this.onMoneyChanged = onMoneyChanged;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(UIFactory.createTitle("SHOP"));
        add(Box.createVerticalStrut(12));

        add(UIFactory.createHeader("Seeds"));
        add(Box.createVerticalStrut(6));
        List<Seed> seeds = SeedFactory.createAllSeeds();
        for (Seed seed : seeds) {
            add(createSeedRow(seed));
            add(Box.createVerticalStrut(8));
        }
        add(Box.createVerticalStrut(8));

        status.setFont(Theme.PIXEL.deriveFont(14f));
        status.setForeground(Theme.GOOD);
        status.setAlignmentX(LEFT_ALIGNMENT);
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        add(status);   // shows "Bought ..." or "Not enough money"

        messageTimer = new Timer(MESSAGE_MILLIS, e -> status.setText(" "));
        messageTimer.setRepeats(false);
    }

    // Shows a message under the list for a few seconds.
    private void showStatus(String text, Color color) {
        status.setForeground(color);
        status.setText(text);
        messageTimer.restart();
    }

    // One parchment row: name, price, grow time and sell price, then "Buy 1" and "Buy 10".
    private JPanel createSeedRow(Seed seed) {
        JPanel row = UIFactory.createRow(false);
        row.add(UIFactory.createLabel(seed.getName() + "  $" + seed.getPrice() + "  (grows " + seed.getGrowTime()
                + "s, sells $" + seed.getSellPrice() + ")", 15f, Theme.WOOD_DARK), BorderLayout.CENTER);

        JButton buyOne = UIFactory.createButton("Buy 1", BUY_BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
        buyOne.addActionListener(e -> buySeeds(seed, 1));
        JButton buyMany = UIFactory.createButton("Buy " + BULK_AMOUNT, BUY_BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
        buyMany.addActionListener(e -> buySeeds(seed, BULK_AMOUNT));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(buyOne);
        buttons.add(buyMany);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    // Pays for the seeds and puts them in the inventory, or says there is not enough money.
    private void buySeeds(Seed seed, int amount) {
        if (session.spend(seed.getPrice() * amount)) {
            session.getInventory().addSeed(seed, amount);
            showStatus("Bought " + amount + " " + seed.getName() + " seed" + (amount > 1 ? "s" : "") + "!", Theme.GOOD);
            onMoneyChanged.run();
        } else {
            showStatus("Not enough money for " + amount + " " + seed.getName() + ".", Theme.BAD);
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
