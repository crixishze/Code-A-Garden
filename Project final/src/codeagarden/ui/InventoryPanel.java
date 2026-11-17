package codeagarden.ui;

import codeagarden.game.GameSession;
import codeagarden.game.PlayerInventory;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The inventory screen: the seeds you own and how many. Nothing is clicked here;
 * the player's code picks the seed by name, for example plant(Carrot).
 */
public class InventoryPanel extends JPanel {

    private final GameSession session = GameSession.getInstance();
    private final JPanel list = new JPanel();

    /** Builds the title, the hint line and the (empty) seed list. */
    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel hint = new JLabel("Plant a seed from your code, for example: plant(Carrot);", SwingConstants.CENTER);
        hint.setFont(Theme.PIXEL.deriveFont(15f));
        hint.setForeground(Theme.PARCHMENT);

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        header.add(UIFactory.createTitle("INVENTORY"));
        header.add(hint);
        add(header, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        add(list, BorderLayout.CENTER);

        refresh();
    }

    /** Re-reads the inventory from GameSession. Called whenever this screen is shown. */
    public void refresh() {
        PlayerInventory inventory = session.getInventory();
        list.removeAll();
        if (inventory.isEmpty()) {
            list.add(UIFactory.createLabel("Your inventory is empty. Buy seeds in the shop.", 16f, Theme.PARCHMENT));
        } else {
            for (Map.Entry<String, Integer> entry : inventory.getCounts().entrySet()) {
                JPanel row = UIFactory.createRow(false);
                row.add(UIFactory.createLabel(entry.getKey() + " x" + entry.getValue(), 18f, Theme.WOOD_DARK),
                        BorderLayout.CENTER);
                list.add(row);
                list.add(Box.createVerticalStrut(8));
            }
        }
        list.revalidate();
        list.repaint();
    }
}
