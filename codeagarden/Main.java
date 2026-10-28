package codeagarden;

import java.awt.*;
import javax.swing.*;

public class Main extends JFrame {

    private static final int GAME_LENGTH_SECONDS = 15 * 60;
    private InterpreterWindow scriptWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }

    public Main() throws HeadlessException {
        setTitle("Code A Garden");
        setSize(1000, 1000);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);

        // bottom bar
        // balance on the left, timer in the middle, page buttons on the right
        BottomBar botBar = new BottomBar(100);
        botBar.addNavButton("Code", () -> {
        scriptWindow.setLocation(getX() + getWidth() + 10, getY());
        scriptWindow.setVisible(true);});
        botBar.addNavButton("Shop", () -> cards.show(content, "shop"));
        botBar.addNavButton("Inventory", () -> cards.show(content, "inventory"));
        botBar.addNavButton("Farm", () -> cards.show(content, "farm"));
        add(botBar, BorderLayout.SOUTH);

        Inventory inventory = new Inventory();

        FarmPlot plot = new FarmPlot(inventory, botBar::addBalance);
        plot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        content.add(plot, "farm");                                   // first card = shown at startup
        content.add(new Store(botBar, inventory, plot), "shop");
        content.add(inventory, "inventory");
        DroneController controller = new DroneController(plot);
        scriptWindow = new InterpreterWindow(controller);
        add(content, BorderLayout.CENTER);

        // win when the balance reaches the goal
        // lose when the timer runs out
        botBar.setOnGoalReached(() -> endGame("You Win!",
                "You reached the goal!\nFinal balance: $" + String.format("%,d", botBar.getBalance())));
        botBar.startTimer(GAME_LENGTH_SECONDS, () -> endGame("Game Over",
                "Time's up!\nFinal balance: $" + String.format("%,d", botBar.getBalance())));

        setVisible(true);
    }

    private void endGame(String title, String message) {
        Object[] options = {"Play Again", "Quit"};
        int choice = JOptionPane.showOptionDialog(
                this,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            scriptWindow.shutdown();
            dispose();
            new Main(); // restarts game with new balance, empty farm, and new 15 minutes
        } else {
            System.exit(0);
        }
    }
}