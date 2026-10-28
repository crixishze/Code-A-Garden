package codeagarden;

import java.awt.*;
import javax.swing.*;

public class BottomBar extends JPanel {

    private static final int GOAL = 500_000;

    private int balance;
    private final JLabel balanceLabel;
    private final JLabel timerLabel;
    private final JPanel buttons;
    private Timer countdown;
    private int secondsLeft;
    private Runnable onGoalReached;
    private boolean goalReached = false;

    public BottomBar(int balance) {
        this.balance = balance;
        setLayout(new BorderLayout()); //changed it so that the interpreter button won't hide from you
        setPreferredSize(new Dimension(1000, 60));
        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        balanceLabel = new JLabel();
        balanceLabel.setForeground(Color.BLACK);
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        updateBalanceLabel();
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(balanceLabel, BorderLayout.WEST); // stays on the left, centered vertically
        add(left, BorderLayout.WEST); //added border BorderLayout.WEST

        timerLabel = new JLabel("15:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(timerLabel, BorderLayout.CENTER); // added BorderLayout.CENTER

        buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setOpaque(false);
        add(buttons, BorderLayout.EAST); // added BorderLayout.EAST
    }

    // starts counting down; runs onTimeUp once when it reaches zero
    public void startTimer(int seconds, Runnable onTimeUp) {
        secondsLeft = seconds;
        updateTimerLabel();

        countdown = new Timer(1000, e -> {
            secondsLeft--;
            updateTimerLabel();
            if (secondsLeft <= 0) {
                countdown.stop();
                onTimeUp.run();
            }
        });
        countdown.start();
    }

    public void stopTimer() {
        if (countdown != null) {
            countdown.stop();
        }
    }

    // runs once when the balance first reaches the goal
    public void setOnGoalReached(Runnable onGoalReached) {
        this.onGoalReached = onGoalReached;
    }

    private void updateTimerLabel() {
        timerLabel.setText(String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60));
        // turns red for the final minute
        timerLabel.setForeground(secondsLeft <= 60 ? Color.RED : Color.BLACK);
    }

    private void updateBalanceLabel() {
        balanceLabel.setText(String.format("Balance: $%,d / $%,d", balance, GOAL));
    }

    // adds a button to the right side; buttons appear in the order they're added
    public void addNavButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        buttons.add(button);
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
        updateBalanceLabel();

        if (balance >= GOAL && !goalReached) {
            goalReached = true;
            stopTimer(); // so the game-over popup can't also appear
            if (onGoalReached != null) {
                onGoalReached.run();
            }
        }
    }

    public void addBalance(int amount) {
        setBalance(balance + amount);
    }

    // returns false (and charges nothing) if the player can't afford it
    public boolean spend(int amount) {
        if (balance < amount) {
            return false;
        }
        setBalance(balance - amount);
        return true;
    }
}