package codeagarden;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class BottomBar extends JPanel{
    
    private int balance;
    private final JLabel balanceLabel;

    public BottomBar(int balance) {
        this.balance = balance;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 60));
        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        balanceLabel = new JLabel("Balance: $" + balance);
        balanceLabel.setForeground(Color.BLACK);
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(balanceLabel, BorderLayout.WEST);
    }

    public void setBalance(int balance) {
        this.balance = balance;
        balanceLabel.setText("Balance: $" + balance);
    }
}
