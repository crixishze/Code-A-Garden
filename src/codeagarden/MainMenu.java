package codeagarden;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** The title screen: NEW GAME, CONTINUE, LEADERBOARD and EXIT over the garden picture. */
public class MainMenu extends JFrame {

    private static final String BACKGROUND_PATH = "/images/Background1.png";
    private static final String TITLE_PATH = "/images/title.png";
    private static final String WHITE_BUTTON_PATH = "/images/RoundButton2_white.png";
    private static final String WOOD_BUTTON_PATH = "/images/RoundButton2_wood.png";

    private static final Dimension MIN_SIZE = new Dimension(800, 500);
    private static final Dimension DEFAULT_SIZE = new Dimension(1280, 720);
    private static final int BUTTON_WIDTH = 360;      // button picture width at scale 1
    private static final int BUTTON_PADDING = 20;
    private static final int TEXT_OFFSET_Y = -6;      // nudges the text up into the button's face
    private static final float NEW_GAME_FONT_SIZE = 27f;
    private static final float OTHER_FONT_SIZE = 25f;
    private static final int BUTTON_COUNT = 4;
    private static final int BUTTON_GAP = 8;             // top + bottom inset around each button
    private static final float BUTTON_SHARE = 0.66f;     // part of the window height the buttons may use
    private static final float BUTTON_IMAGE_RATIO = 161f / 362f;   // height / width of the button picture

    public MainMenu() {
        super("Code A Garden");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(MIN_SIZE);
        setContentPane(new MenuPanel());
        setSize(DEFAULT_SIZE);
        setLocationRelativeTo(null);
        new FullscreenHelper(this);   // F11 toggles full screen, Esc leaves it
        setVisible(true);
    }

    // ---- NEW GAME: log in (or create the player), then start from day 1 ----
    private void startNewGame() {
        LoginForm login = LoginForm.ask(this, "New Game",
                "Pick a player name and password. You need them to CONTINUE later.");
        if (login == null) {
            return;
        }
        try {
            Player player = findOrCreatePlayer(login);
            if (player != null) {
                startGame(player, null);
            }
        } catch (SaveException e) {
            if (!e.isConnectionProblem()) {
                showWarning("New Game", e.getMessage());   // e.g. "name already taken"
            } else if (askYesNo("No database", e.getMessage() + "\n\nPlay without saving?")) {
                startGame(null, null);
            }
        }
    }

    // A new name creates a player. An existing name must come with the right password.
    // Returns null if the player decides not to replace their saved game.
    private Player findOrCreatePlayer(LoginForm login) throws SaveException {
        SaveRepository repository = new SaveRepository();
        if (!repository.playerExists(login.getName())) {
            return repository.createPlayer(login.getName(), login.getPassword());
        }
        Player player;
        try {
            player = repository.logIn(login.getName(), login.getPassword());
        } catch (SaveException wrongPassword) {
            throw new SaveException("The name \"" + login.getName() + "\" is already taken "
                    + "(or the password is wrong). Pick another name.");
        }
        if (repository.hasGameInProgress(player)
                && !askYesNo("New Game", "You already have a saved game. A NEW GAME replaces it the next "
                        + "time you save.\nStart a new game?")) {
            return null;
        }
        return player;
    }

    // ---- CONTINUE: log in, then Load or Delete this player's save ----
    private void continueGame() {
        LoginForm login = LoginForm.ask(this, "Continue", "Enter your player name and password.");
        if (login == null) {
            return;
        }
        try {
            SaveRepository repository = new SaveRepository();
            Player player = repository.logIn(login.getName(), login.getPassword());
            SaveData data = repository.load(player);
            String choice = askLoadOrDelete(player, data);
            if ("Load".equals(choice)) {
                startGame(player, data);
            } else if ("Delete save".equals(choice) && confirmDelete(player)) {
                repository.deleteSave(player);
                JOptionPane.showMessageDialog(this, player.getName() + " was deleted.",
                        "Delete save", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SaveException e) {
            showWarning("Could not load your game", e.getMessage());
        }
    }

    // Shows what is saved and asks Load / Delete save / Cancel. Returns the button text, or null.
    private String askLoadOrDelete(Player player, SaveData data) {
        String message;
        String[] options;
        if (data == null) {
            message = "Hi " + player.getName() + "! You have no game in progress. Start a NEW GAME.";
            options = new String[] {"Delete save", "Cancel"};
        } else {
            int day = Math.min(GameSession.TOTAL_DAYS, data.getElapsedSeconds() / GameSession.SECONDS_PER_DAY + 1);
            message = "Welcome back, " + player.getName() + "!\nDay " + day + " of " + GameSession.TOTAL_DAYS
                    + "   Balance: $" + String.format("%,d", data.getBalance());
            options = new String[] {"Load", "Delete save", "Cancel"};
        }
        int picked = JOptionPane.showOptionDialog(this, message, "Continue", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return picked >= 0 ? options[picked] : null;
    }

    private boolean confirmDelete(Player player) {
        return askYesNo("Delete save", "Delete " + player.getName() + "'s save and remove them from the "
                + "leaderboard?\nThis cannot be undone.");
    }

    // Opens the game window. data == null means a brand-new game.
    private void startGame(Player player, SaveData data) {
        GameSession session = GameSession.getInstance();
        if (data == null) {
            session.reset();
        } else {
            session.restore(data);
        }
        session.setPlayer(player);   // null = playing without saving
        dispose();
        new GameScreen();
    }

    private void showLeaderboard() {
        LeaderboardDialog.show(this);
    }

    private void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private boolean askYesNo(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** Draws the background and arranges the title and the four buttons. */
    private class MenuPanel extends JPanel {
        private final BufferedImage background = Resources.loadImage(BACKGROUND_PATH);
        private final ImageLabel title = new ImageLabel(Resources.loadImage(TITLE_PATH));
        private final MenuButton newGame;
        private final MenuButton continueButton;
        private final MenuButton leaderboard;
        private final MenuButton exit;

        MenuPanel() {
            BufferedImage whiteButton = Resources.loadImage(WHITE_BUTTON_PATH);
            BufferedImage woodButton = Resources.loadImage(WOOD_BUTTON_PATH);
            newGame = new MenuButton(whiteButton, "NEW GAME", NEW_GAME_FONT_SIZE,
                    Theme.MENU_TEXT_BROWN, Theme.MENU_TEXT_BROWN_HOVER, MainMenu.this::startNewGame);
            continueButton = new MenuButton(whiteButton, "CONTINUE", OTHER_FONT_SIZE,
                    Theme.MENU_TEXT_BROWN, Theme.MENU_TEXT_BROWN_HOVER, MainMenu.this::continueGame);
            leaderboard = new MenuButton(whiteButton, "LEADERBOARD", OTHER_FONT_SIZE,
                    Theme.MENU_TEXT_BROWN, Theme.MENU_TEXT_BROWN_HOVER, MainMenu.this::showLeaderboard);
            exit = new MenuButton(woodButton, "EXIT", OTHER_FONT_SIZE,
                    Theme.MENU_TEXT_CREAM, Color.WHITE, () -> System.exit(0));

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = GridBagConstraints.RELATIVE;
            c.insets = new Insets(0, 0, 12, 0);
            add(title, c);
            c.insets = new Insets(4, 0, 4, 0);
            add(newGame, c);
            add(continueButton, c);
            add(leaderboard, c);
            add(exit, c);

            // size things now, then again whenever the window changes size
            updateScales(DEFAULT_SIZE.width, DEFAULT_SIZE.height);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateScales(getWidth(), getHeight());
                }
            });
        }

        // Reserve space for all four buttons so the complete menu stays visible.
        private void updateScales(int windowWidth, int windowHeight) {
            float maxTitleWidth = Math.min(800f, windowWidth * 0.72f);
            float maxTitleHeight = Math.min(220f, windowHeight * 0.24f);
            float titleScale = Math.min(maxTitleWidth / title.image.getWidth(),
                    maxTitleHeight / title.image.getHeight());
            title.setScale(Math.max(0.1f, titleScale));

            // The scale at which all four buttons (picture + padding + gap) fit in BUTTON_SHARE of the height.
            float spaceForPictures = windowHeight * BUTTON_SHARE - BUTTON_COUNT * (2 * BUTTON_PADDING + BUTTON_GAP);
            float fittingScale = spaceForPictures / (BUTTON_COUNT * BUTTON_WIDTH * BUTTON_IMAGE_RATIO);
            float buttonScale = Math.max(0.2f, Math.min(0.52f, fittingScale));
            newGame.setScale(buttonScale);
            continueButton.setScale(buttonScale);
            leaderboard.setScale(buttonScale);
            exit.setScale(buttonScale);
        }

        // background picture fills the window (scaled to cover, cropping the overflow)
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            int iw = background.getWidth();
            int ih = background.getHeight();
            double s = Math.max(1.0, Math.max((double) w / iw, (double) h / ih));
            int dw = (int) Math.ceil(iw * s);
            int dh = (int) Math.ceil(ih * s);
            g.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        }
    }

    /** A picture that can be scaled (the game title). */
    private static class ImageLabel extends JComponent {
        private final BufferedImage image;
        private float scale = 1f;

        ImageLabel(BufferedImage image) {
            this.image = image;
        }

        void setScale(float newScale) {
            scale = newScale;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Math.round(image.getWidth() * scale), Math.round(image.getHeight() * scale));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }
    }

    /** A picture button with text on it. The text changes color while the mouse is over it. */
    private static class MenuButton extends JComponent {
        private final BufferedImage image;
        private final String text;
        private final float fontSize;
        private final Color textColor;
        private final Color hoverTextColor;
        private float scale = 1f;
        private boolean hover;
        private boolean pressed;

        MenuButton(BufferedImage image, String text, float fontSize, Color textColor,
                   Color hoverTextColor, Runnable onClick) {
            this.image = image;
            this.text = text;
            this.fontSize = fontSize;
            this.textColor = textColor;
            this.hoverTextColor = hoverTextColor;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean clicked = pressed && hover;   // released while still over the button
                    pressed = false;
                    if (clicked) {
                        onClick.run();
                    }
                }
            });
        }

        void setScale(float newScale) {
            scale = newScale;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int buttonWidth = Math.round(BUTTON_WIDTH * scale);
            int buttonHeight = Math.round(buttonWidth * image.getHeight() / (float) image.getWidth());
            return new Dimension(buttonWidth + BUTTON_PADDING * 2, buttonHeight + BUTTON_PADDING * 2);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int w = getWidth() - 2 * BUTTON_PADDING;
            int h = Math.round(w * image.getHeight() / (float) image.getWidth());
            g.drawImage(image, BUTTON_PADDING, BUTTON_PADDING, w, h, null);

            g.setFont(Theme.DIALOG.deriveFont(fontSize * scale));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(hover ? hoverTextColor : textColor);
            g.drawString(text,
                    BUTTON_PADDING + (w - fm.stringWidth(text)) / 2,
                    BUTTON_PADDING + h / 2 + (fm.getAscent() - fm.getDescent()) / 2 + Math.round(TEXT_OFFSET_Y * scale));
            g.dispose();
        }
    }
}
