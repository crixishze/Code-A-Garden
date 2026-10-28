package codeagarden;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MainMenu extends JFrame {

    private static final String BG_PATH          = "/images/Background1.png";
    private static final String TITLE_PATH       = "/images/title.png";
    private static final String NEW_GAME_PATH    = "/images/RoundButton2_white.png";
    private static final String CONTINUE_PATH    = "/images/RoundButton2_white.png";
    private static final String EXIT_PATH        = "/images/RoundButton2_wood.png";

    private static final Font BUTTON_FONT = loadFont("/fonts/DD.otf", 30f);
    private static final int TEXT_OFFSET_Y = -6;

    private static final Color NEW_GAME_TEXT_COLOR = new Color(139, 78, 57);
    private static final Color CONTINUE_TEXT_COLOR = new Color(255, 248, 220);
    private static final Dimension MIN_SIZE = new Dimension(800, 500);
    private static final Dimension DEFAULT_SIZE = new Dimension(1280, 720);

    private static final int   BOUNCE_HEIGHT = 10;
    private static final float BOUNCE_SPEED  = 4f;
    private static final int BUTTON_WIDTH  = 360;
    private static final float BUTTON_SIZE = 1f;
    private boolean fullscreen = false;

    public MainMenu() {
        super("Grow a Garden");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(MIN_SIZE);
        setContentPane(new MenuPanel());
        setSize(DEFAULT_SIZE);
        setLocationRelativeTo(null);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F11"), "fs");
        getRootPane().getActionMap().put("fs", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setFullscreen(!fullscreen); }
        });

        setVisible(true);
    }

    private void startGame() {
        dispose();
        new Main();
    }

    private void continueGame() {
    }

    private void setFullscreen(boolean on) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullscreen = on;
        if (on) {
            dispose();
            setUndecorated(true);
            device.setFullScreenWindow(this);
        } else {
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setSize(DEFAULT_SIZE);
            setLocationRelativeTo(null);
        }
        setVisible(true);
    }

    private class MenuPanel extends JPanel {
        private final BufferedImage background = loadImage(BG_PATH);
        private final ImageLabel title = new ImageLabel(loadImage(TITLE_PATH));
        private final MenuButton play, cont, exit;

        MenuPanel() {
            play = new MenuButton(loadImage(NEW_GAME_PATH), "NEW GAME",
                    NEW_GAME_TEXT_COLOR, true, MainMenu.this::startGame);
            cont = new MenuButton(loadImage(CONTINUE_PATH), "CONTINUE",
                    NEW_GAME_TEXT_COLOR, true, MainMenu.this::continueGame);
            exit = new MenuButton(loadImage(EXIT_PATH), "EXIT",
                    CONTINUE_TEXT_COLOR, true, () -> System.exit(0));

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = GridBagConstraints.RELATIVE;
            c.insets = new Insets(0, 0, 12, 0);
            add(title, c);
            c.insets = new Insets(4, 0, 4, 0);
            add(play, c);
            add(cont, c);
            add(exit, c);

            // Apply an initial size immediately, then recalculate when the window changes.
            updateScales(DEFAULT_SIZE.width, DEFAULT_SIZE.height);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    updateScales(getWidth(), getHeight());
                }
            });
        }

        private void updateScales(int windowWidth, int windowHeight) {
            // Reserve space for all three buttons so the complete menu remains visible.
            float maxTitleWidth = Math.min(800f, windowWidth * 0.72f);
            float maxTitleHeight = Math.min(220f, windowHeight * 0.30f);
            float titleScale = Math.min(maxTitleWidth / title.img.getWidth(),
                                        maxTitleHeight / title.img.getHeight());
            title.setScale(Math.max(0.1f, titleScale));

            float buttonScale = Math.max(0.40f, Math.min(0.58f, windowHeight / 1200f));
            play.setScale(buttonScale * BUTTON_SIZE);
            cont.setScale(buttonScale * BUTTON_SIZE);
            exit.setScale(buttonScale * BUTTON_SIZE);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            int iw = background.getWidth(), ih = background.getHeight();
            double s = Math.max(1.0, Math.max((double) w / iw, (double) h / ih));
            int dw = (int) Math.ceil(iw * s), dh = (int) Math.ceil(ih * s);
            g.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        }
    }

    private static class ImageLabel extends JComponent {
        private final BufferedImage img;
        private float scale = 1f;

        ImageLabel(BufferedImage img) { this.img = img; }

        void setScale(float s) { scale = s; revalidate(); repaint(); }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Math.round(img.getWidth() * scale), Math.round(img.getHeight() * scale));
        }

        @Override protected void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }
    }

    private static class MenuButton extends JComponent {
        private static final int PAD = 20;
        private static int count = 0;
        private final float offset = count++ * 0.6f;
        private final BufferedImage image;
        private final String text;
        private final Color textColor;
        private final boolean drawText;
        private float scale = 1f;
        private boolean hover, pressed;

        MenuButton(BufferedImage image, String text, Color textColor,
                   boolean drawText, Runnable onClick) {
            this.image = image;
            this.text = text;
            this.textColor = textColor;
            this.drawText = drawText;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean clicked = pressed && hover;
                    pressed = false;
                    repaint();
                    if (clicked) onClick.run();
                }
            });
        }

        void setScale(float s) { scale = s; revalidate(); repaint(); }

        @Override public Dimension getPreferredSize() {
            int buttonWidth = Math.round(BUTTON_WIDTH * scale);
            int buttonHeight = Math.round(buttonWidth
                    * image.getHeight() / (float) image.getWidth());
            return new Dimension(buttonWidth + PAD * 2, buttonHeight + PAD * 2);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int hop = (int) (Math.abs(Math.sin(System.currentTimeMillis() / 1000.0 * BOUNCE_SPEED + offset)) * BOUNCE_HEIGHT);

            int baseW = getWidth() - 2 * PAD;
            int baseH = Math.round(baseW * image.getHeight() / (float) image.getWidth());

            float sx = pressed ? 1.04f : (hover ? 1.05f : 1f);
            float sy = pressed ? 0.88f : (hover ? 1.05f : 1f);

            int w = Math.round(baseW * sx);
            int h = Math.round(baseH * sy);
            int x = (getWidth() - w) / 2;
            int y = PAD + baseH - h - hop;

            g.drawImage(image, x, y, w, h, null);

            if (drawText && !text.isEmpty()) {
                Graphics2D t = (Graphics2D) g.create();
                t.translate(x + w / 2.0, y + h / 2.0);
                t.scale(sx, sy);
                float fontSize = text.equals("NEW GAME") ? 27f : 25f;
                t.setFont(BUTTON_FONT.deriveFont(fontSize * scale));
                FontMetrics fm = t.getFontMetrics();
                t.setColor(textColor);
                t.drawString(text, -fm.stringWidth(text) / 2,
                     (fm.getAscent() - fm.getDescent()) / 2
                             + Math.round(TEXT_OFFSET_Y * scale));
                t.dispose();
            }
        }
    }

    private static BufferedImage loadImage(String path) {
        try {
            java.net.URL resource = MainMenu.class.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + path
                        + " (check that it is inside src/main/resources" + path + ")");
            }
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported or invalid image: " + path);
            }
            return image;
        } catch (Exception e) {
            throw new RuntimeException("Could not load image resource " + path, e);
        }
    }

    private static Font loadFont(String path, float size) {
        try {
            java.io.InputStream stream = MainMenu.class.getResourceAsStream(path);
            if (stream == null) {
                throw new IllegalArgumentException("Font resource not found: " + path);
            }
            try (stream) {
                return Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load font resource " + path, e);
        }
    }
}
