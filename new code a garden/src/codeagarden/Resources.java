package codeagarden;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * The one place that loads pictures and fonts. Every screen uses this instead of
 * keeping its own copy. Paths start with "/" and are looked up on the classpath,
 * for example "/images/title.png" means the file src/images/title.png.
 */
public final class Resources {

    private Resources() {
        // Only static methods; nobody should create a Resources object.
    }

    /** Loads a picture. A missing picture is a real bug, so this stops with the exact path. */
    public static BufferedImage loadImage(String path) {
        URL url = Resources.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Image not found on classpath: " + path);
        }
        try (InputStream in = url.openStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalStateException("Not a valid image: " + path);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read image: " + path, e);
        }
    }

    /**
     * Loads a font at the given size. A missing font should not crash the game, so it
     * falls back to a plain bold sans-serif font and prints a warning.
     */
    public static Font loadFont(String path, float size) {
        try (InputStream in = Resources.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Font not found on classpath: " + path);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (Exception e) {
            System.err.println("Warning: could not load font " + path + " (" + e.getMessage()
                    + "). Using SansSerif instead.");
            return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size));
        }
    }
}
