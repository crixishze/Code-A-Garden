package codeagarden;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Small drawing helpers shared by the bottom bar and the pop-up dialog. */
final class Drawing {

    private Drawing() {
        // Only static methods.
    }

    /** Turns on smooth scaling and smooth text. */
    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /**
     * Draws a picture stretched to w x h without squashing its rounded ends:
     * the picture is scaled to the height, then only the middle part is stretched sideways.
     */
    static void drawSliced(Graphics2D g, BufferedImage img, int w, int h, int capSrc) {
        double scale = (double) h / img.getHeight();
        int cap = Math.min((int) Math.round(capSrc * scale), w / 2);
        int srcCap = (int) Math.round(cap / scale);
        int iw = img.getWidth();
        int ih = img.getHeight();
        g.drawImage(img, 0, 0, cap, h, 0, 0, srcCap, ih, null);                    // left end
        g.drawImage(img, cap, 0, w - cap, h, srcCap, 0, iw - srcCap, ih, null);    // stretched middle
        g.drawImage(img, w - cap, 0, w, h, iw - srcCap, 0, iw, ih, null);          // right end
    }

    /** Draws text centered sideways; yFrac says where its middle sits (0 = top, 1 = bottom). */
    static void drawCenteredText(Graphics2D g, String text, Font font, Color color, int w, int h, double yFrac) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (int) (h * yFrac) + (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(color);
        g.drawString(text, x, y);
    }
}
