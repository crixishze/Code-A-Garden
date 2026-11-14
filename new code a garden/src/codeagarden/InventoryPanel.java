package codeagarden;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The inventory screen: the seeds you own and which one is selected for planting. */
public class InventoryPanel extends JPanel {

    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 36;
    private static final float BUTTON_FONT_SIZE = 14f;

    private final GameSession session = GameSession.getInstance();
    private final JLabel selectedLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel list = new JPanel();

    public InventoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.WOOD_DARK);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        selectedLabel.setFont(Theme.PIXEL.deriveFont(16f));
        selectedLabel.setForeground(Theme.PARCHMENT);

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setOpaque(false);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        header.add(UIFactory.createTitle("INVENTORY"));
        header.add(selectedLabel);
        add(header, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        add(list, BorderLayout.CENTER);

        refresh();
    }

    /** Re-reads the inventory from GameSession. Called whenever this screen is shown. */
    public void refresh() {
        PlayerInventory inventory = session.getInventory();
        String selected = inventory.getSelectedName();
        selectedLabel.setText(selected == null ? "No seed selected" : "Planting: " + selected);

        list.removeAll();
        if (inventory.isEmpty()) {
            list.add(UIFactory.createLabel("Your inventory is empty.", 16f, Theme.PARCHMENT));
        } else {
            for (Map.Entry<String, Integer> entry : inventory.getCounts().entrySet()) {
                list.add(createSeedRow(entry.getKey(), entry.getValue(), entry.getKey().equals(selected)));
                list.add(Box.createVerticalStrut(8));
            }
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel createSeedRow(String name, int amount, boolean isSelected) {
        JPanel row = UIFactory.createRow(isSelected);
        row.add(UIFactory.createLabel(name + " x" + amount, 18f, Theme.WOOD_DARK), BorderLayout.CENTER);

        if (isSelected) {
            row.add(UIFactory.createSelectedButton("Selected", BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE), BorderLayout.EAST);
        } else {
            JButton select = UIFactory.createButton("Select", BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
            select.addActionListener(e -> {
                session.getInventory().select(name);
                refresh();
            });
            row.add(select, BorderLayout.EAST);
        }
        return row;
    }
}
