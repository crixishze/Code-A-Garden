package codeagarden.ui;

import codeagarden.data.SaveException;
import codeagarden.data.SaveRepository;
import codeagarden.game.GameSession;
import codeagarden.interpreter.DroneController;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The game window. The left side shows the Farm (or the Shop, Inventory or Story); the right
 * side is always the code editor. It runs the one-second day timer and ends the game by itself:
 * WIN as soon as the balance reaches $10,000, LOSE when day 30 is over without it.
 * The game state itself lives in GameSession.
 */
public class GameScreen extends JFrame {

    private static final Dimension DEFAULT_SIZE = new Dimension(1280, 720);
    private static final Dimension MIN_SIZE = new Dimension(1200, 600);
    private static final int DAY_TIMER_MILLIS = 1000;
    private static final int FARM_MARGIN = 20;
    private static final double FARM_SHARE = 0.62;   // part of the width given to the left side

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
    private final InterpreterPanel codePanel;
    private final Timer dayTimer;
    private boolean gameFinished = false;

    /** Opens the game window. showTutorial is true for a NEW GAME (the tutorial opens first). */
    public GameScreen(boolean showTutorial) {
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
        codePanel = new InterpreterPanel(new DroneController(farmPanel), this::showTutorial);

        content.add(farmPanel, FARM_CARD);   // first card = shown at startup
        content.add(new ShopPanel(this::onMoneyChanged), SHOP_CARD);
        content.add(inventoryPanel, INVENTORY_CARD);
        content.add(new StoryPanel(this::onMoneyChanged), STORY_CARD);

        // left: farm / shop / inventory / story   right: the code editor
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, content, codePanel);
        split.setResizeWeight(FARM_SHARE);
        split.setContinuousLayout(true);
        split.setBorder(null);

        setUpBottomBar();
        add(split, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        new FullscreenHelper(this);   // F11 toggles full screen, Esc leaves it

        dayTimer = new Timer(DAY_TIMER_MILLIS, e -> onSecondPassed());
        setVisible(true);
        split.setDividerLocation(FARM_SHARE);

        if (showTutorial) {
            // after the window is on screen: the tutorial first, then the clock starts
            SwingUtilities.invokeLater(() -> {
                TutorialDialog.show(this);
                dayTimer.start();
            });
        } else {
            dayTimer.start();
            SwingUtilities.invokeLater(this::checkForEnding);   // a loaded game might already be over
        }
    }

    // The page buttons at the bottom; each one shows a screen on the left side.
    private void setUpBottomBar() {
        bottomBar.setMenuAction(this::onMenuPressed);
        bottomBar.addNavButton("Farm", () -> showCard(FARM_CARD));
        bottomBar.addNavButton("Shop", () -> showCard(SHOP_CARD));
        bottomBar.addNavButton("Inventory", () -> showCard(INVENTORY_CARD));
        bottomBar.addNavButton("Story", () -> showCard(STORY_CARD));
    }

    // Shows one screen on the left side (the inventory is re-read first, seeds may have changed).
    private void showCard(String name) {
        if (INVENTORY_CARD.equals(name)) {
            inventoryPanel.refresh();
        }
        cards.show(content, name);
    }

    // ? in the code panel: the tutorial again, with the day clock paused while it is open.
    private void showTutorial() {
        boolean wasRunning = dayTimer.isRunning();
        dayTimer.stop();
        TutorialDialog.show(this);
        if (wasRunning && !gameFinished) {
            dayTimer.start();
        }
    }

    // Called by the screens after anything that changes the balance.
    private void onMoneyChanged() {
        bottomBar.refresh();
        // later, so the drone command that earned the money can finish first
        SwingUtilities.invokeLater(this::checkForEnding);
    }

    // Called once per real second.
    private void onSecondPassed() {
        session.tickSecond();
        bottomBar.refresh();
        checkForEnding();
    }

    // The two ways a game ends. Reaching the goal wins even on the last second.
    private void checkForEnding() {
        if (gameFinished) {
            return;
        }
        if (session.isGoalReached()) {
            finishGame(true);
        } else if (session.isSeasonOver()) {
            finishGame(false);
        }
    }

    // ---- leaving the game: MENU button or the window's close button ----

    // MENU: ask about saving, then go back to the main menu.
    private void onMenuPressed() {
        if (askToLeave("Save before going", "to the menu?", "Return to the", "main menu?")) {
            stopEverything();
            dispose();
            new MainMenu();
        }
    }

    // The window's X: ask about saving, then quit the program.
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
            GameDialog.message(this, "Save problem", "Your game could not be saved.\n" + e.getMessage()
                    + "\nPick QUIT to leave without saving.");
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
            GameDialog.message(this, "Save problem", "Your result could not be saved.\n" + e.getMessage());
        }
    }

    // Stops every timer and the script so nothing keeps running in the background.
    private void stopEverything() {
        dayTimer.stop();
        farmPanel.stop();
        codePanel.stop();
    }

    // The ending: farming stops, the result is saved, the story ending is shown, then Play Again or Main Menu.
    private void finishGame(boolean won) {
        gameFinished = true;
        stopEverything();
        recordResult();
        String title = won ? "YOU WIN!" : "SEASON OVER";
        String message = won
                ? session.getStory().getWinEnding(session.getCurrentDay(), session.getBalance())
                : session.getStory().getLoseEnding(session.getBalance());
        int choice = GameDialog.choose(this, title, message, "Play Again", "Main Menu");
        dispose();
        if (choice == 0) {
            session.reset();   // new balance, empty farm, a fresh 30-day season; same player
            new GameScreen(false);
        } else {
            new MainMenu();
        }
    }
}
