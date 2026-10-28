package codeagarden;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class BottomBar extends JPanel {

    private static final int GOAL = 10_000;

    // ---- palette ----
    private static final Color WHITE = new Color(0xffffff);  // bar background
    private static final Color BLACK     = new Color(0x000000);  // all text on the art (best contrast on cream and wood)
    private static final Color RED       = new Color(0xD61904);  // timer warning (invalid / running out)

    // ---- resources: put these in your resources folder ----
    //   /fonts/Poppins-Black.ttf (bottom bar), /fonts/DD.otf (confirmation popup)
    //   /images/RoundButton2_white.png, /images/RoundButton2_wood.png,
    //   /images/RedCard.png, /images/WoodenContainer1.png,
    //   /images/Container1.png, /images/GreenButton.png, /images/RedButton.png
    private static final Font BASE_FONT = loadFont("/fonts/Poppins-Black.ttf", 18f);
    private static final Font DD_FONT = loadFont("/fonts/DD.otf", 22f);

    private static final BufferedImage BUTTON_IMAGE = loadImage("/images/RoundButton2_white.png");
    private static final BufferedImage BROWN_BUTTON_IMAGE = loadImage("/images/RoundButton2_wood.png");
    private static final BufferedImage TIMER_IMAGE = loadImage("/images/RedCard.png");
    private static final BufferedImage BALANCE_IMAGE = loadImage("/images/WoodenContainer1.png");
    private static final BufferedImage CONFIRMATION_IMAGE = loadImage("/images/Container1.png");
    private static final BufferedImage YES_IMAGE = loadImage("/images/GreenButton.png");
    private static final BufferedImage NO_IMAGE = loadImage("/images/RedButton.png");

    // ---- base sizes (full size; everything shrinks together when the window gets narrow) ----
    private static final double FULL_WIDTH = 1280.0;
    private static final double MIN_SCALE = 0.65;
    private static final float FONT_SIZE = 18f;      // same font size for balance, timer and buttons
    private static final int ELEMENT_HEIGHT = 60;    // same height for balance, timer and buttons
    private static final int BAR_HEIGHT = 84;
    private static final int BUTTON_GAP = 10;

    private int balance;
    private final SlicedLabel balanceLabel;
    private final SlicedLabel timerLabel;
    private final JPanel buttons;
    private final FlowLayout buttonLayout;
    private final List<ImageButton> buttonList = new ArrayList<>();
    private Timer dayCounter;
    private int secondsPerDay = 60;
    private int totalDays = 30;
    private int elapsedSeconds = 0;
    private Runnable onDayLimitReached;
    private Runnable onGoalReached;
    private Runnable onReturnToMenu;
    private boolean returnMenuButtonAdded = false;
    private boolean goalReached = false;
    private double scale = 1.0;

    public BottomBar(int balance) {
        this.balance = balance;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(WHITE);
        setPreferredSize(new Dimension(1000, BAR_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // money on the wooden container (width is sized for the biggest possible text so it never jumps around)
        balanceLabel = new SlicedLabel(BALANCE_IMAGE, 60, 0.48, "Balance: $10,000 / $10,000", 30);
        updateBalanceLabel();
        JPanel left = new JPanel(new GridBagLayout()); // GridBagLayout centers it vertically
        left.setOpaque(false);
        left.add(balanceLabel);
        add(left, BorderLayout.WEST);

        // timer on the red card; the text sits in the cream area below the red top
        timerLabel = new SlicedLabel(TIMER_IMAGE, 60, 0.62, "DAY 30 / 30", 26);
        timerLabel.setText("DAY 1 / 30");
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(timerLabel);
        add(middle, BorderLayout.CENTER);

        // page buttons on the right
        buttonLayout = new FlowLayout(FlowLayout.RIGHT, BUTTON_GAP, (BAR_HEIGHT - ELEMENT_HEIGHT) / 2);
        buttons = new JPanel(buttonLayout);
        buttons.setOpaque(false);
        add(buttons, BorderLayout.EAST);

        // Always create the MENU button as part of the bottom bar.
        // Main supplies the actual screen-transition action through setReturnToMenuAction().
        addReturnToMenuButton(() -> {
            if (onReturnToMenu != null) {
                onReturnToMenu.run();
            }
        });

        // shrink / grow everything when the window width changes
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyScale();
            }
        });
        applyScale();
    }

    // ---- scaling ----
    private void applyScale() {
        int w = getWidth();
        double newScale = w <= 0 ? 1.0 : Math.max(MIN_SCALE, Math.min(1.0, w / FULL_WIDTH));
        if (Math.abs(newScale - scale) < 0.01 && w > 0) {
            return; // nothing meaningful changed, avoids relayout loops
        }
        scale = newScale;

        int barHeight = (int) Math.round(BAR_HEIGHT * scale);
        setPreferredSize(new Dimension(1000, barHeight));
        buttonLayout.setHgap((int) Math.round(BUTTON_GAP * scale));
        buttonLayout.setVgap((barHeight - scaledHeight()) / 2);

        balanceLabel.rescale();
        timerLabel.rescale();
        for (ImageButton b : buttonList) {
            b.rescale();
        }
        revalidate();
        repaint();
    }

    private Font scaledFont() {
        return BASE_FONT.deriveFont((float) (FONT_SIZE * scale));
    }

    private int scaledHeight() {
        return (int) Math.round(ELEMENT_HEIGHT * scale);
    }

    // ---- loading ----
    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(BottomBar.class.getResource(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + path, e);
        }
    }

    private static Font loadFont(String path, float size) {
        try (InputStream in = BottomBar.class.getResourceAsStream(path)) {
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (Exception e) {
            System.err.println("Could not load font " + path + ", using SansSerif instead");
            return new Font("SansSerif", Font.BOLD, (int) size);
        }
    }

    // ---- drawing helpers ----

    // draws an image stretched to w x h without squashing the rounded ends:
    // the image is scaled to the height, then only the middle part is stretched sideways
    private static void drawSliced(Graphics2D g, BufferedImage img, int w, int h, int capSrc) {
        double scale = (double) h / img.getHeight();
        int cap = Math.min((int) Math.round(capSrc * scale), w / 2);
        int srcCap = (int) Math.round(cap / scale);
        int iw = img.getWidth();
        int ih = img.getHeight();
        g.drawImage(img, 0, 0, cap, h, 0, 0, srcCap, ih, null);                    // left end
        g.drawImage(img, cap, 0, w - cap, h, srcCap, 0, iw - srcCap, ih, null);    // stretched middle
        g.drawImage(img, w - cap, 0, w, h, iw - srcCap, 0, iw, ih, null);          // right end
    }

    private static void drawCenteredText(Graphics2D g, String text, Font font, Color color, int w, int h, double yFrac) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (int) (h * yFrac) + (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    // a picture with text on top (used for the money and the timer)
    private class SlicedLabel extends JComponent {
        private final BufferedImage image;
        private final int capSrc;
        private final double yFrac;   // where the text's middle sits, 0 = top, 1 = bottom
        private final String widestText;
        private final int padX;
        private String text = "";
        private Color color = BLACK;

        SlicedLabel(BufferedImage image, int capSrc, double yFrac, String widestText, int padX) {
            this.image = image;
            this.capSrc = capSrc;
            this.yFrac = yFrac;
            this.widestText = widestText;
            this.padX = padX;
            rescale();
        }

        void rescale() {
            int width = getFontMetrics(scaledFont()).stringWidth(widestText) + (int) Math.round(2 * padX * scale);
            setPreferredSize(new Dimension(width, scaledHeight()));
            revalidate();
        }

        void setText(String text) {
            this.text = text;
            repaint();
        }

        void setTextColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            drawSliced(g2, image, getWidth(), getHeight(), capSrc);
            drawCenteredText(g2, text, scaledFont(), color, getWidth(), getHeight(), yFrac);
            g2.dispose();
        }
    }

    // A round image button with its label drawn on top.
    private class ImageButton extends JButton {
        private final BufferedImage buttonImage;

        ImageButton(String text) {
            this(text, BUTTON_IMAGE);
        }

        ImageButton(String text, BufferedImage buttonImage) {
            super(text);
            this.buttonImage = buttonImage;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            rescale();
        }

        void rescale() {
            int width = getFontMetrics(scaledFont()).stringWidth(getText()) + (int) Math.round(56 * scale);
            setPreferredSize(new Dimension(width, scaledHeight()));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            boolean pressed = getModel().isPressed();
            if (pressed) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)); // dims a little when clicked
            }
            drawSliced(g2, buttonImage, getWidth(), getHeight(), buttonImage.getHeight() / 2);
            g2.setComposite(AlphaComposite.SrcOver);
            drawCenteredText(g2, getText(), scaledFont(), BLACK, getWidth(), getHeight(), pressed ? 0.52 : 0.48);
            g2.dispose();
        }
    }

    /**
     * Starts the in-game day counter.
     *
     * @param secondsPerDay real-world seconds per in-game day (60 = one minute per day)
     * @param totalDays number of in-game days before game over
     * @param onDayLimitReached action to run when the final day ends without winning
     */
    public void startDayCounter(int secondsPerDay, int totalDays, Runnable onDayLimitReached) {
        stopTimer();
        this.secondsPerDay = Math.max(1, secondsPerDay);
        this.totalDays = Math.max(1, totalDays);
        this.elapsedSeconds = 0;
        this.onDayLimitReached = onDayLimitReached;
        updateDayLabel();

        dayCounter = new Timer(1000, e -> {
            elapsedSeconds++;

            updateDayLabel();

            // The game ends after the full duration of the final day has elapsed.
            if (elapsedSeconds >= this.secondsPerDay * this.totalDays) {
                stopTimer();
                if (!goalReached && this.onDayLimitReached != null) {
                    this.onDayLimitReached.run();
                }
            }
        });
        dayCounter.start();
    }

    public void stopTimer() {
        if (dayCounter != null) {
            dayCounter.stop();
        }
    }

    private void updateDayLabel() {
        int currentDay = Math.min(totalDays, (elapsedSeconds / secondsPerDay) + 1);
        timerLabel.setText("DAY " + currentDay + " / " + totalDays);
        // Highlight the final in-game day.
        timerLabel.setTextColor(currentDay >= totalDays ? RED : BLACK);
    }

    // runs once when the balance first reaches the goal
    public void setOnGoalReached(Runnable onGoalReached) {
        this.onGoalReached = onGoalReached;
    }

    private void updateTimerLabel() {
        updateDayLabel();
    }

    private void updateBalanceLabel() {
        balanceLabel.setText(String.format("Balance: $%,d / $%,d", balance, GOAL));
    }

    // Adds the MENU option to the bottom bar. The confirmation UI lives here,
    // not in Main. The supplied action runs only after the player chooses YES.
    public void addReturnToMenuButton(Runnable returnToMenuAction) {
        setReturnToMenuAction(returnToMenuAction);
    }

    private void ensureReturnToMenuButton() {
        if (returnMenuButtonAdded) {
            return;
        }
        ImageButton button = new ImageButton("MENU", BROWN_BUTTON_IMAGE);
        button.addActionListener(e -> showReturnToMenuConfirmation(onReturnToMenu));
        buttonList.add(button);
        buttons.add(button);
        returnMenuButtonAdded = true;
        buttons.revalidate();
        buttons.repaint();
    }

    private void showReturnToMenuConfirmation(Runnable returnToMenuAction) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Return to Menu", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        // Give the art plenty of room for the question and two large buttons.
        // Keep Container1.png's original aspect ratio so its rounded corners are not distorted.
        final int popupWidth = 600;
        final int popupHeight = (int) Math.round(popupWidth
                * (double) CONFIRMATION_IMAGE.getHeight() / CONFIRMATION_IMAGE.getWidth());

        JPanel content = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                smooth(g2);
                g2.drawImage(CONFIRMATION_IMAGE, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(58, 38, 48, 38));

        JComponent question = new PromptLabel();
        ImageChoiceButton yes = new ImageChoiceButton("YES", YES_IMAGE);
        ImageChoiceButton no = new ImageChoiceButton("NO", NO_IMAGE);
        yes.addActionListener(e -> {
            dialog.dispose();
            if (returnToMenuAction != null) {
                stopTimer();
                returnToMenuAction.run();
            }
        });
        no.addActionListener(e -> dialog.dispose());

        JPanel choices = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        choices.setOpaque(false);
        choices.add(yes);
        choices.add(no);
        choices.setPreferredSize(new Dimension(500, 158));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 28, 0);
        content.add(question, c);

        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.NONE;
        content.add(choices, c);

        dialog.setContentPane(content);
        dialog.setSize(popupWidth, popupHeight);
        dialog.setMinimumSize(new Dimension(popupWidth, popupHeight));
        dialog.setLocationRelativeTo(owner != null ? owner : this);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private class ImageChoiceButton extends JButton {
        private final BufferedImage image;

        ImageChoiceButton(String text, BufferedImage image) {
            super(text);
            this.image = image;
            setFont(DD_FONT.deriveFont(25f));
            setForeground(Color.BLACK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Larger buttons give the DD lettering more breathing room. Preserve artwork ratio.
            int buttonHeight = 150;
            int buttonWidth = (int) Math.round(buttonHeight * (double) image.getWidth() / image.getHeight());
            setPreferredSize(new Dimension(buttonWidth, buttonHeight));
            setMinimumSize(new Dimension(buttonWidth, buttonHeight));
            setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            boolean pressed = getModel().isPressed();
            if (pressed) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
            }
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g2.setComposite(AlphaComposite.SrcOver);
            drawCenteredText(g2, getText(), getFont(), Color.BLACK, getWidth(), getHeight(), pressed ? 0.53 : 0.48);
            g2.dispose();
        }
    }

    // Sets what happens after confirmation and ensures MENU is present in the bottom bar.
    public void setReturnToMenuAction(Runnable action) {
        this.onReturnToMenu = action;
        ensureReturnToMenuButton();
    }

    // Draw the prompt directly so the custom DD font is used reliably (Swing HTML can ignore it).
    private class PromptLabel extends JComponent {
        private final String firstLine = "Return to the";
        private final String secondLine = "main menu?";

        PromptLabel() {
            setPreferredSize(new Dimension(480, 112));
            setMinimumSize(new Dimension(480, 112));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setFont(DD_FONT.deriveFont(28f));
            g2.setColor(Color.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            int firstX = (getWidth() - fm.stringWidth(firstLine)) / 2;
            int secondX = (getWidth() - fm.stringWidth(secondLine)) / 2;
            int firstY = getHeight() / 2 - fm.getHeight() / 2 + fm.getAscent() - 8;
            int secondY = firstY + fm.getHeight();
            g2.drawString(firstLine, firstX, firstY);
            g2.drawString(secondLine, secondX, secondY);
            g2.dispose();
        }
    }

    // adds a button to the right side; buttons appear in the order they're added
    public void addNavButton(String text, Runnable action) {
        ImageButton button = new ImageButton(text);
        button.addActionListener(e -> action.run());
        buttonList.add(button);
        buttons.add(button);
        buttons.revalidate();
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
