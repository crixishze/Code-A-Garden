import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MainMenu extends JFrame {

    private static final String BG_PATH     = "/images/background.png";
    private static final String TITLE_PATH  = "/images/title.png";
    private static final String BUTTON_PATH = "/images/button.png";

    private static final Font BUTTON_FONT = loadFont("/fonts/DD.otf", 30f);
    private static final int TEXT_OFFSET_Y = -6;

    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;
    private static final Dimension MIN_SIZE = new Dimension(2000, 1000);
    private static final Dimension DEFAULT_SIZE = new Dimension(1280, 720);

    private static final int   BOUNCE_HEIGHT = 10;
    private static final float BOUNCE_SPEED  = 4f;
    private static final int BUTTON_WIDTH  = 400;
    private static final int BUTTON_HEIGHT = 100;
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
        private final MenuButton play, cont;

        MenuPanel() {
            BufferedImage buttonImg = loadImage(BUTTON_PATH);
            play = new MenuButton(buttonImg, "Play", MainMenu.this::startGame);
            cont = new MenuButton(buttonImg, "Continue", MainMenu.this::continueGame);

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = GridBagConstraints.RELATIVE;
            c.insets = new Insets(0, 0, 40, 0);
            add(title, c);
            c.insets = new Insets(10, 0, 10, 0);
            add(play, c);
            add(cont, c);

            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    float s = Math.max(0.6f, Math.min(1.6f, getHeight() / 800f));
                    title.setScale(s);
                    play.setScale(s * BUTTON_SIZE);
                    cont.setScale(s * BUTTON_SIZE);
                }
            });
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
        private float scale = 1f;
        private boolean hover, pressed;
 
        MenuButton(BufferedImage image, String text, Runnable onClick) {
            this.image = image;
            this.text = text;
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
            return new Dimension(Math.round(BUTTON_WIDTH * scale) + PAD * 2,
                                 Math.round(BUTTON_HEIGHT * scale) + PAD * 2);
}

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int hop = (int) (Math.abs(Math.sin(System.currentTimeMillis() / 1000.0 * BOUNCE_SPEED + offset)) * BOUNCE_HEIGHT);

            int baseW = getWidth() - 2 * PAD;
            int baseH = getHeight() - 2 * PAD;

            float sx = pressed ? 1.04f : (hover ? 1.05f : 1f);
            float sy = pressed ? 0.88f : (hover ? 1.05f : 1f);

            int w = Math.round(baseW * sx);
            int h = Math.round(baseH * sy);
            int x = (getWidth() - w) / 2;
            int y = PAD + baseH - h - hop;

            g.drawImage(image, x, y, w, h, null);

            Graphics2D t = (Graphics2D) g.create();
            t.translate(x + w / 2.0, y + h / 2.0);
            t.scale(sx, sy);
            t.setFont(BUTTON_FONT.deriveFont(BUTTON_FONT.getSize2D() * scale));
            FontMetrics fm = t.getFontMetrics();
            t.setColor(BUTTON_TEXT_COLOR);
            t.drawString(text, -fm.stringWidth(text) / 2,
                 (fm.getAscent() - fm.getDescent()) / 2 + Math.round(TEXT_OFFSET_Y * scale));
            t.dispose();
        }
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(MainMenu.class.getResource(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + path, e);
        }
    }

    private static Font loadFont(String path, float size) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, MainMenu.class.getResourceAsStream(path)).deriveFont(size);
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + path, e);
        }
    }
}
