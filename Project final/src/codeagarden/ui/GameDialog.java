package codeagarden.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * The game's own pop-up, in the same style as the Tutorial and the Leaderboard: a wooden
 * window, an orange title, the message on parchment and green buttons (all from UIFactory
 * and Theme). Used for the NEW GAME and CONTINUE messages, the Load / Delete choice, the
 * win and lose endings and save problems, instead of Java's plain grey JOptionPane.
 */
final class GameDialog {

    private static final int MIN_TEXT_WIDTH = 380;   // pixels of text, not counting the border
    private static final int MAX_TEXT_WIDTH = 700;
    private static final int BUTTON_HEIGHT = 44;
    private static final float TEXT_SIZE = 15f;
    private static final float BUTTON_TEXT_SIZE = 16f;

    private GameDialog() {
        // Only static methods.
    }

    /**
     * Shows the message with one button per entry in buttons and waits until one is pressed.
     * Returns the index of the button pressed (0 = first), or -1 if the window was closed.
     * The first button is the default: Enter presses it.
     */
    static int choose(Window owner, String title, String message, String... buttons) {
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        int[] picked = {-1};   // an array so the button actions below can change it

        JPanel screen = UIFactory.createScreenPanel(new BorderLayout(0, 14), 18, 22, 18, 22);
        screen.add(UIFactory.createTitle(title.toUpperCase()), BorderLayout.NORTH);
        screen.add(createText(message), BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        for (int i = 0; i < buttons.length; i++) {
            int index = i;
            JButton button = UIFactory.createButton(buttons[i], buttonWidth(buttons[i]), BUTTON_HEIGHT, BUTTON_TEXT_SIZE);
            button.addActionListener(e -> {
                picked[0] = index;
                dialog.dispose();
            });
            row.add(button);
            if (i == 0) {
                dialog.getRootPane().setDefaultButton(button);
            }
        }
        screen.add(row, BorderLayout.SOUTH);

        dialog.setContentPane(screen);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // modal: waits here until a button is pressed or the window is closed
        return picked[0];
    }

    /** A message with one OK button. */
    static void message(Window owner, String title, String message) {
        choose(owner, title, message, "OK");
    }

    /** A Yes / No question. True only if Yes was pressed. */
    static boolean yesNo(Window owner, String title, String message) {
        return choose(owner, title, message, "Yes", "No") == 0;
    }

    // The parchment text box. Its width fits the longest line (between the min and max), long lines wrap.
    private static JTextArea createText(String message) {
        JTextArea text = new JTextArea(message);
        Font font = Theme.PIXEL.deriveFont(TEXT_SIZE);
        text.setFont(font);
        text.setForeground(Theme.WOOD_DARK);
        text.setBackground(Theme.PARCHMENT);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.WOOD_LIGHT, 4),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        FontMetrics metrics = text.getFontMetrics(font);
        int longest = 0;
        for (String line : message.split("\n")) {
            longest = Math.max(longest, metrics.stringWidth(line));
        }
        Insets border = text.getInsets();
        int width = Math.max(MIN_TEXT_WIDTH, Math.min(MAX_TEXT_WIDTH, longest + 4)) + border.left + border.right;
        text.setSize(width, Short.MAX_VALUE);   // a fixed width first, so the wrapped height can be measured
        text.setPreferredSize(new Dimension(width, text.getPreferredSize().height));
        return text;
    }

    // Wide enough for the button's text.
    private static int buttonWidth(String text) {
        return Math.max(130, text.length() * 12 + 40);
    }
}
