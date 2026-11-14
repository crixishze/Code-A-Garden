package codeagarden;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * DESIGN PATTERN: Factory Method (second use, next to SeedFactory).
 * Builds the buttons, rows, titles and panels for the shop, inventory and story screens.
 * Each screen asks for "a green button" or "a seed row" instead of repeating about
 * 15 lines of styling code. To change how every button looks, edit one method here.
 */
public final class UIFactory {

    private static final int ROW_HEIGHT = 56;
    private static final int TITLE_HEIGHT = 48;
    private static final int BUTTON_BORDER_WIDTH = 3;

    private UIFactory() {
        // Only static methods.
    }

    /** A dark wooden screen background with an empty margin. */
    public static JPanel createScreenPanel(LayoutManager layout, int top, int left, int bottom, int right) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Theme.WOOD_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return panel;
    }

    /** Big orange screen title, e.g. "SHOP". */
    public static JLabel createTitle(String text) {
        JLabel title = new JLabel(text, SwingConstants.CENTER);
        title.setFont(Theme.PIXEL.deriveFont(32f));
        title.setForeground(Theme.TITLE_ORANGE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, TITLE_HEIGHT));
        return title;
    }

    /** Section heading on parchment color, e.g. "Seeds". */
    public static JLabel createHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(Theme.PIXEL.deriveFont(20f));
        header.setForeground(Theme.PARCHMENT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    /** A plain label in the pixel font. */
    public static JLabel createLabel(String text, float size, java.awt.Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.PIXEL.deriveFont(size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** One parchment row (a seed in the shop, an owned seed in the inventory). */
    public static JPanel createRow(boolean selected) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(selected ? Theme.PARCHMENT_SELECTED : Theme.PARCHMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Theme.TITLE_ORANGE : Theme.WOOD_LIGHT, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(java.awt.Color.BLACK, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12))));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** A green button that lightens when the mouse is over it. */
    public static JButton createButton(String text, int width, int height, float fontSize) {
        final JButton button = new JButton(text);
        button.setFont(Theme.PIXEL.deriveFont(fontSize));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Theme.BUTTON_GREEN);
        button.setForeground(java.awt.Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER, BUTTON_BORDER_WIDTH));
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(Theme.BUTTON_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(Theme.BUTTON_GREEN);
                }
            }
        });
        return button;
    }

    /** The gold, non-clickable "Selected" button. */
    public static JButton createSelectedButton(String text, int width, int height, float fontSize) {
        JButton button = new JButton(text);
        button.setFont(Theme.PIXEL.deriveFont(fontSize));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Theme.BUTTON_SELECTED);
        button.setForeground(Theme.WOOD_DARK);
        button.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_SELECTED_BORDER, BUTTON_BORDER_WIDTH));
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    /** Turns a factory button on (green) or off (grey). */
    public static void setButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setBackground(enabled ? Theme.BUTTON_GREEN : Theme.BUTTON_DISABLED);
    }
}
