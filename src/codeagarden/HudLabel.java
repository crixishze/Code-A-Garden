package codeagarden;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/** A picture with text on top, used for the balance and the day counter in the bottom bar. */
class HudLabel extends JComponent {

    private final BufferedImage image;
    private final int capSrc;          // width of the rounded end of the picture that must not be stretched
    private final double yFrac;        // where the text's middle sits, 0 = top, 1 = bottom
    private final String widestText;   // the label is sized for this, so it never jumps around
    private final int padX;
    private double scale = 1.0;
    private String text = "";
    private Color color = Theme.HUD_TEXT;

    HudLabel(BufferedImage image, int capSrc, double yFrac, String widestText, int padX) {
        this.image = image;
        this.capSrc = capSrc;
        this.yFrac = yFrac;
        this.widestText = widestText;
        this.padX = padX;
        rescale(1.0);
    }

    /** Called when the window gets narrower or wider and everything shrinks or grows together. */
    void rescale(double newScale) {
        scale = newScale;
        int width = getFontMetrics(BottomBar.scaledFont(scale)).stringWidth(widestText)
                + (int) Math.round(2 * padX * scale);
        setPreferredSize(new Dimension(width, BottomBar.scaledHeight(scale)));
        revalidate();
    }

    void setText(String newText) {
        text = newText;
        repaint();
    }

    void setTextColor(Color newColor) {
        color = newColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Drawing.smooth(g2);
        Drawing.drawSliced(g2, image, getWidth(), getHeight(), capSrc);
        Drawing.drawCenteredText(g2, text, BottomBar.scaledFont(scale), color, getWidth(), getHeight(), yFrac);
        g2.dispose();
    }
}
