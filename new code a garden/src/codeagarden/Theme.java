package codeagarden;

import java.awt.Color;
import java.awt.Font;

/**
 * The look of the game in one place: colors and fonts. Screens read these constants
 * instead of declaring their own copies, so changing a color here changes it everywhere.
 */
public final class Theme {

    private Theme() {
        // Constants only; no objects.
    }

    // ---- wood and parchment (shop, inventory, story screens) ----
    public static final Color WOOD_DARK = Color.decode("#5b3a1e");
    public static final Color WOOD_LIGHT = Color.decode("#a0713f");
    public static final Color PARCHMENT = Color.decode("#f3dfa2");
    public static final Color PARCHMENT_SELECTED = Color.decode("#fff3c4");
    public static final Color TITLE_ORANGE = Color.decode("#ff8c1a");

    // ---- buttons ----
    public static final Color BUTTON_GREEN = Color.decode("#4cc23a");
    public static final Color BUTTON_HOVER = Color.decode("#6fe05a");
    public static final Color BUTTON_BORDER = Color.decode("#1e6b17");
    public static final Color BUTTON_DISABLED = Color.decode("#8a8a8a");
    public static final Color BUTTON_SELECTED = Color.decode("#e0a82e");
    public static final Color BUTTON_SELECTED_BORDER = Color.decode("#8a5a00");

    // ---- messages ----
    public static final Color GOOD = Color.decode("#8dff7a");
    public static final Color BAD = Color.decode("#ff6b5e");

    // ---- bottom bar (HUD) and main menu ----
    public static final Color BAR_BACKGROUND = Color.WHITE;
    public static final Color HUD_TEXT = Color.BLACK;
    public static final Color TIMER_WARNING = new Color(0xD61904);
    public static final Color MENU_TEXT_BROWN = new Color(139, 78, 57);
    public static final Color MENU_TEXT_BROWN_HOVER = new Color(95, 50, 35);   // darker brown while the mouse is over a menu button
    public static final Color MENU_TEXT_CREAM = new Color(255, 248, 220);

    // ---- font files ----
    private static final String HUD_FONT_PATH = "/fonts/Poppins-Black.ttf";
    private static final String DIALOG_FONT_PATH = "/fonts/DD.otf";

    // ---- fonts (use deriveFont(size) to get another size) ----
    /** Monospaced bold font used by the shop, inventory and story screens. */
    public static final Font PIXEL = new Font(Font.MONOSPACED, Font.BOLD, 16);
    /** Poppins Black: bottom bar balance, day counter and page buttons. */
    public static final Font HUD = Resources.loadFont(HUD_FONT_PATH, 18f);
    /** DD font: main menu buttons and the "Return to menu" popup. */
    public static final Font DIALOG = Resources.loadFont(DIALOG_FONT_PATH, 30f);
}
