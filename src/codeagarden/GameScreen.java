package codeagarden;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The game window. It puts the screens together, runs the one-second day timer, and
 * decides when the game is won or lost. The game state itself lives in GameSession.
 */
public class GameScreen extends JFrame {

    private static final Dimension DEFAULT_SIZE = new Dimension(1280, 720);
    private static final Dimension MIN_SIZE = new Dimension(1200, 600);   // keeps the bottom bar and a 6x6 farm from getting squashed
    private static final int DAY_TIMER_MILLIS = 1000;
    private static final int FARM_MARGIN = 20;
    private static final int SCRIPT_WINDOW_GAP = 10;

    private static final String FARM_CARD = "farm";
    private static final String SHOP_CARD = "shop";
    private static final String INVENTORY_CARD = "inventory";
    private static final String STORY_CARD = "story";

    private final GameSession session = GameSession.getInstance();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final BottomBar bottomBar = new BottomBar();
    private final FarmPanel farmPanel;
    private final InventoryPanel inventoryPanel;
    private final InterpreterWindow scriptWindow;
    private final Timer dayTimer;
    private boolean gameFinished = false;

    public GameScreen() {
        super("Code A Garden");
        setSize(DEFAULT_SIZE);
        setMinimumSize(MIN_SIZE);
        setResizable(true);   // needed for the maximize button
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);   // we ask about saving first (see onWindowClosing)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing();
            }
        });

        farmPanel = new FarmPanel(this::onMoneyChanged);
        farmPanel.setBorder(BorderFactory.createEmptyBorder(FARM_MARGIN, FARM_MARGIN, FARM_MARGIN, FARM_MARGIN));
        inventoryPanel = new InventoryPanel();

        content.add(farmPanel, FARM_CARD);   // first card = shown at startup
        content.add(new ShopPanel(this::onMoneyChanged, farmPanel::refreshGrid), SHOP_CARD);
        content.add(inventoryPanel, INVENTORY_CARD);
        content.add(new StoryPanel(this::onMoneyChanged), STORY_CARD);

        scriptWindow = new InterpreterWindow(new DroneController(farmPanel));
        setUpBottomBar();
        add(bottomBar, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        new FullscreenHelper(this);   // F11 toggles full screen, Esc leaves it

        dayTimer = new Timer(DAY_TIMER_MILLIS, e -> onSecondPassed());
        dayTimer.start();
        setVisible(true);
    }

    private void setUpBottomBar() {
        bottomBar.setMenuAction(this::onMenuPressed);
        bottomBar.addNavButton("Code", () -> {
            scriptWindow.setLocation(getX() + getWidth() + SCRIPT_WINDOW_GAP, getY());
            scriptWindow.setVisible(true);
        });
        bottomBar.addNavButton("Shop", () -> showCard(SHOP_CARD));
        bottomBar.addNavButton("Inventory", () -> showCard(INVENTORY_CARD));
        bottomBar.addNavButton("Farm", () -> showCard(FARM_CARD));
        bottomBar.addNavButton("Story", () -> showCard(STORY_CARD));
    }

    void showCard(String name) {
        if (INVENTORY_CARD.equals(name)) {
            inventoryPanel.refresh();   // seeds may have changed since it was last shown
        }
        cards.show(content, name);
    }

    // Called by the screens after anything that changes the balance.
    private void onMoneyChanged() {
        bottomBar.refresh();
        if (!gameFinished && session.isGoalReached()) {
            finishGame("You Win!", "You reached the goal!\n" + session.getStory().getFinalOutcome()
                    + "\nFinal balance: $" + formattedBalance());
        }
    }

    // Called once per real second.
    private void onSecondPassed() {
        session.tickSecond();
        bottomBar.refresh();
        if (!gameFinished && session.isSeasonOver()) {
            finishGame("Game Over", "You ran out of days!\nYou reached day " + GameSession.TOTAL_DAYS
                    + " without reaching the goal.\n" + session.getStory().getFinalOutcome()
                    + "\nFinal balance: $" + formattedBalance());
        }
    }

    private String formattedBalance() {
        return String.format("%,d", session.getBalance());
    }

    // ---- leaving the game: MENU button or the window's close button ----
    private void onMenuPressed() {
        if (askToLeave("Save before going", "to the menu?", "Return to the", "main menu?")) {
            stopEverything();
            dispose();
            new MainMenu();
        }
    }

    private void onWindowClosing() {
        if (gameFinished || session.getPlayer() == null) {
            System.exit(0);   // nothing to save
        }
        if (askToLeave("Save before", "quitting?", null, null)) {
            System.exit(0);
        }
    }

    /**
     * Shows SAVE & QUIT / QUIT / CANCEL (or YES / NO when there is no player to save for).
     * The day clock is paused while the pop-up is open. Returns true if the game should be left.
     */
    private boolean askToLeave(String saveLine1, String saveLine2, String noSaveLine1, String noSaveLine2) {
        boolean canSave = session.getPlayer() != null;
        dayTimer.stop();
        LeaveGameDialog.Choice choice = canSave
                ? LeaveGameDialog.ask(this, saveLine1, saveLine2, true)
                : LeaveGameDialog.ask(this, noSaveLine1, noSaveLine2, false);
        boolean leave = choice == LeaveGameDialog.Choice.LEAVE
                || (choice == LeaveGameDialog.Choice.SAVE_AND_LEAVE && saveGame());
        if (!leave) {
            dayTimer.start();   // stay in the game
        }
        return leave;
    }

    // Saves the game in progress. If it fails the player is told and stays in the game (returns false).
    private boolean saveGame() {
        try {
            new SaveRepository().save(session.getPlayer(), session.createSaveData());
            return true;
        } catch (SaveException e) {
            JOptionPane.showMessageDialog(this, "Your game could not be saved.\n" + e.getMessage()
                    + "\nPick QUIT to leave without saving.", "Save problem", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    // A won or lost game is saved as finished: its currency goes on the leaderboard, CONTINUE won't load it.
    private void recordResult() {
        if (session.getPlayer() == null) {
            return;
        }
        try {
            new SaveRepository().saveFinishedGame(session.getPlayer(), session.createSaveData());
        } catch (SaveException e) {
            JOptionPane.showMessageDialog(this, "Your result could not be saved.\n" + e.getMessage(),
                    "Save problem", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Stops every timer and the script so nothing keeps running in the background.
    private void stopEverything() {
        dayTimer.stop();
        farmPanel.stop();
        scriptWindow.shutdown();
    }

    private void finishGame(String title, String message) {
        gameFinished = true;
        stopEverything();
        recordResult();
        Object[] options = {"Play Again", "Quit"};
        int choice = JOptionPane.showOptionDialog(this, message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            dispose();
            session.reset();   // new balance, empty farm, a fresh 30-day season; same player
            new GameScreen();
        } else {
            System.exit(0);
        }
    }
}
