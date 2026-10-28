package codeagarden;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

public class Inventory extends JPanel {

    private final Map<String, Seed> seeds = new LinkedHashMap<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private String selected; // name of the seed chosen for planting (null = none)

    private final JLabel selectedLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel list = new JPanel();

    public Inventory() {
        setLayout(new BorderLayout());
        setBackground(new Color(222, 184, 135));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("INVENTORY", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        selectedLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setOpaque(false);
        header.add(title);
        header.add(selectedLabel);
        add(header, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        add(list, BorderLayout.CENTER);

        refresh();
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
            list.add(new JLabel("Your inventory is empty."));
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String name = entry.getKey();
                boolean isSelected = name.equals(selected);

                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

                JLabel label = new JLabel(name + " x" + entry.getValue());
                label.setFont(new Font("SansSerif", Font.PLAIN, 18));
                row.add(label, BorderLayout.CENTER);

                JButton select = new JButton(isSelected ? "Selected" : "Select");
                select.setEnabled(!isSelected);
                select.addActionListener(e -> {
                    selected = name;
                    refresh();
                });
                row.add(select, BorderLayout.EAST);

                list.add(row);
            }
        }
        list.revalidate();
        list.repaint();
    }
}