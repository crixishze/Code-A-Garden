package codeagarden;

import java.awt.*;
import javax.swing.*;

public class Store extends JFrame {
    public Store() {
        setTitle("Seed Store");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(0, 1, 5, 5));
        setBackground(Color.decode("#fada5e"));

        JLabel title = new JLabel("SEED STORE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title);

        add(makeRow("Carrot", 10));
        add(makeRow("Corn", 20));
        add(makeRow("Tomato", 30));
        add(makeRow("Pumpkin", 50));
    }

    private JPanel makeRow(String name, int price) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(210, 180, 140));

        JLabel label = new JLabel("  " + name + " - $" + price);
        row.add(label, BorderLayout.CENTER);

        JButton buy = new JButton("Buy");
        buy.addActionListener(e -> System.out.println("Bought " + name));
        row.add(buy, BorderLayout.EAST);

        return row;
    }
}
