package codeagarden;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

public class Inventory extends JPanel {

    // ---------- palette (same as Store) ----------
    private static final Color WOOD_DARK    = Color.decode("#5b3a1e");
    private static final Color WOOD_LIGHT   = Color.decode("#a0713f");
    private static final Color PARCHMENT    = Color.decode("#f3dfa2");
    private static final Color PARCHMENT_HI = Color.decode("#fff3c4"); // selected row
    private static final Color TITLE_ORANGE = Color.decode("#ff8c1a");
    private static final Color BTN_GREEN    = Color.decode("#4cc23a");
    private static final Color BTN_HOVER    = Color.decode("#6fe05a");
    private static final Color BTN_BORDER   = Color.decode("#1e6b17");
    private static final Color SELECTED_BG  = Color.decode("#e0a82e"); // gold "Selected" button
    private static final Color SELECTED_BORDER = Color.decode("#8a5a00");

    // change this path to your font file (same one the Store uses)
    private static final Font PIXEL = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private final Map<String, Seed> seeds = new LinkedHashMap<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private String selected; // name of the seed chosen for planting (null = none)

    private final JLabel selectedLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel list = new JPanel();

    public Inventory() {
        setLayout(new BorderLayout());
        setBackground(WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("INVENTORY", SwingConstants.CENTER);
        title.setFont(PIXEL.deriveFont(32f));
        title.setForeground(TITLE_ORANGE);

        selectedLabel.setFont(PIXEL.deriveFont(16f));
        selectedLabel.setForeground(PARCHMENT);

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        header.add(title);
        header.add(selectedLabel);
        add(header, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        add(list, BorderLayout.CENTER);

        refresh();
    }

    // ---------- styling helpers ----------

    private JPanel makeRowPanel(boolean isSelected) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(isSelected ? PARCHMENT_HI : PARCHMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? TITLE_ORANGE : WOOD_LIGHT, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12))));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    // `clickable` = false for the currently selected seed (gold, no hover, no action)
    private void styleButton(JButton b, boolean clickable) {
        b.setFont(PIXEL.deriveFont(14f));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(110, 36));

        if (clickable) {
            b.setBackground(BTN_GREEN);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 3));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOVER); }
                @Override public void mouseExited(MouseEvent e)  { b.setBackground(BTN_GREEN); }
            });
        } else {
            b.setBackground(SELECTED_BG);
            b.setForeground(WOOD_DARK);
            b.setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 3));
        }
    }

    // called by the Store; the first seed you own is selected automatically
    public void addSeed(Seed seed, int amount) {
        seeds.putIfAbsent(seed.getName(), seed);
        counts.merge(seed.getName(), amount, Integer::sum);
        if (selected == null) {
            selected = seed.getName();
        }
        refresh();
    }

    // called by FarmPlot: returns the selected seed and uses up one, or null if none
    public Seed takeSelectedSeed() {
        if (selected == null) {
            return null;
        }
        Seed seed = seeds.get(selected);
        int left = counts.get(selected) - 1;
        if (left <= 0) {
            counts.remove(selected);
            seeds.remove(selected);
            // move the selection to another seed you still have, if any
            selected = counts.isEmpty() ? null : counts.keySet().iterator().next();
        } else {
            counts.put(selected, left);
        }
        refresh();
        return seed;
    }

    private void refresh() {
        selectedLabel.setText(selected == null ? "No seed selected" : "Planting: " + selected);

        list.removeAll();
        if (counts.isEmpty()) {
            JLabel empty = new JLabel("Your inventory is empty.");
            empty.setFont(PIXEL.deriveFont(16f));
            empty.setForeground(PARCHMENT);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(empty);
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String name = entry.getKey();
                boolean isSelected = name.equals(selected);

                JPanel row = makeRowPanel(isSelected);

                JLabel label = new JLabel(name + " x" + entry.getValue());
                label.setFont(PIXEL.deriveFont(18f));
                label.setForeground(WOOD_DARK);
                row.add(label, BorderLayout.CENTER);

                JButton select = new JButton(isSelected ? "Selected" : "Select");
                styleButton(select, !isSelected);
                if (!isSelected) {
                    select.addActionListener(e -> {
                        selected = name;
                        refresh();
                    });
                }
                row.add(select, BorderLayout.EAST);

                list.add(row);
                list.add(Box.createVerticalStrut(8));
            }
        }
        list.revalidate();
        list.repaint();
    }
}