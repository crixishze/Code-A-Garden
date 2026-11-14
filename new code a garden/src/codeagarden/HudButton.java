package codeagarden;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/** A round picture button with its label drawn on top (the page buttons in the bottom bar). */
class HudButton extends JButton {

    private static final int TEXT_PADDING = 56;
    private static final double TEXT_Y_FRACTION = 0.48;

    private final BufferedImage buttonImage;
    private double scale = 1.0;

    HudButton(String text, BufferedImage buttonImage) {
        super(text);
        this.buttonImage = buttonImage;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rescale(1.0);
    }

    void rescale(double newScale) {
        scale = newScale;
        int width = getFontMetrics(BottomBar.scaledFont(scale)).stringWidth(getText())
                + (int) Math.round(TEXT_PADDING * scale);
        setPreferredSize(new Dimension(width, BottomBar.scaledHeight(scale)));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Drawing.smooth(g2);
        Drawing.drawSliced(g2, buttonImage, getWidth(), getHeight(), buttonImage.getHeight() / 2);
        Drawing.drawCenteredText(g2, getText(), BottomBar.scaledFont(scale), Theme.HUD_TEXT,
                getWidth(), getHeight(), TEXT_Y_FRACTION);
        g2.dispose();
    }
}
