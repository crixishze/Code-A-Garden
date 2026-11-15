package codeagarden;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * The bar at the bottom of the game: balance on the left, day counter in the middle,
 * page buttons on the right. It only DISPLAYS: the numbers come from GameSession
 * every time refresh() is called. Everything shrinks together when the window gets narrow.
 */
public class BottomBar extends JPanel {

    // ---- sizes at full width ----
    // Poppins Black is wide, so the bar starts shrinking below 1400 pixels. At 1280 the balance,
    // day counter and six buttons would not all fit at full size.
    private static final double FULL_WIDTH = 1400.0;
    private static final double MIN_SCALE = 0.65;
    private static final float FONT_SIZE = 18f;      // same font size for balance, day counter and buttons
    private static final int ELEMENT_HEIGHT = 60;    // same height for balance, day counter and buttons
    private static final int BAR_HEIGHT = 84;
    private static final int BUTTON_GAP = 10;
    private static final int BAR_SIDE_MARGIN = 20;
    private static final int PICTURE_CAP = 60;       // rounded end of the wooden and red pictures

    private final BufferedImage buttonImage = Resources.loadImage("/images/RoundButton2_white.png");
    private final BufferedImage menuButtonImage = Resources.loadImage("/images/RoundButton2_wood.png");

    private final HudLabel balanceLabel;
    private final HudLabel dayLabel;
    private final FlowLayout buttonLayout;
    private final JPanel buttons;
    private final List<HudButton> buttonList = new ArrayList<>();
    private Runnable onMenu;
    private double scale = 1.0;

    public BottomBar() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.BAR_BACKGROUND);
        setPreferredSize(new Dimension(1000, BAR_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, BAR_SIDE_MARGIN, 0, BAR_SIDE_MARGIN));

        // money on the wooden container (sized for the biggest possible text)
        balanceLabel = new HudLabel(Resources.loadImage("/images/WoodenContainer1.png"),
                PICTURE_CAP, 0.48, "Balance: $10,000 / $10,000", 30);
        add(centeredHolder(balanceLabel), BorderLayout.WEST);

        // day counter on the red card; the text sits in the cream area below the red top
        dayLabel = new HudLabel(Resources.loadImage("/images/RedCard.png"),
                PICTURE_CAP, 0.62, "DAY 30 / 30", 26);
        add(centeredHolder(dayLabel), BorderLayout.CENTER);

        // page buttons on the right; MENU is always the first one
        buttonLayout = new FlowLayout(FlowLayout.RIGHT, BUTTON_GAP, (BAR_HEIGHT - ELEMENT_HEIGHT) / 2);
        buttons = new JPanel(buttonLayout);
        buttons.setOpaque(false);
        add(buttons, BorderLayout.EAST);
        addMenuButton();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyScale();
            }
        });
        applyScale();
        refresh();
    }

    // GridBagLayout centers its one child vertically
    private JPanel centeredHolder(HudLabel label) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(label);
        return holder;
    }

    // ---- sizes used by HudLabel and HudButton ----
    static Font scaledFont(double scale) {
        return Theme.HUD.deriveFont((float) (FONT_SIZE * scale));
    }

    static int scaledHeight(double scale) {
        return (int) Math.round(ELEMENT_HEIGHT * scale);
    }

    private void applyScale() {
        int w = getWidth();
        double newScale = w <= 0 ? 1.0 : Math.max(MIN_SCALE, Math.min(1.0, w / FULL_WIDTH));
        if (Math.abs(newScale - scale) < 0.01 && w > 0) {
            return;   // nothing meaningful changed; avoids relayout loops
        }
        scale = newScale;

        int barHeight = (int) Math.round(BAR_HEIGHT * scale);
        setPreferredSize(new Dimension(1000, barHeight));
        buttonLayout.setHgap((int) Math.round(BUTTON_GAP * scale));
        buttonLayout.setVgap((barHeight - scaledHeight(scale)) / 2);

        balanceLabel.rescale(scale);
        dayLabel.rescale(scale);
        for (HudButton b : buttonList) {
            b.rescale(scale);
        }
        revalidate();
        repaint();
    }

    /** Re-reads the balance and the day from GameSession. Call after anything changes them. */
    public void refresh() {
        GameSession session = GameSession.getInstance();
        balanceLabel.setText(String.format("Balance: $%,d / $%,d", session.getBalance(), GameSession.GOAL));
        int day = session.getCurrentDay();
        dayLabel.setText("DAY " + day + " / " + GameSession.TOTAL_DAYS);
        // highlight the final in-game day
        dayLabel.setTextColor(day >= GameSession.TOTAL_DAYS ? Theme.TIMER_WARNING : Theme.HUD_TEXT);
    }

    /** Adds a page button to the right side; buttons appear in the order they are added. */
    public void addNavButton(String text, Runnable action) {
        HudButton button = new HudButton(text, buttonImage);
        button.addActionListener(e -> action.run());
        addButton(button);
    }

    /** Sets what happens when the MENU button is pressed (GameScreen shows the save pop-up). */
    public void setMenuAction(Runnable action) {
        onMenu = action;
    }

    private void addMenuButton() {
        HudButton menu = new HudButton("MENU", menuButtonImage);
        menu.addActionListener(e -> {
            if (onMenu != null) {
                onMenu.run();
            }
        });
        addButton(menu);
    }

    private void addButton(HudButton button) {
        buttonList.add(button);
        buttons.add(button);
        buttons.revalidate();
        buttons.repaint();
    }
}
