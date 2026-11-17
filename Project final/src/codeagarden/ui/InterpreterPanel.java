package codeagarden.ui;

import codeagarden.interpreter.DroneController;
import codeagarden.interpreter.ScriptRunner;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * The code editor on the right side of the game window: where the player writes the
 * drone's program. Run starts it (and turns into Stop), Pause pauses it, and ? opens the
 * tutorial again. Keywords and command names are colored while typing.
 * Running the code is handed to ScriptRunner; this class is only the screen.
 */
public class InterpreterPanel extends JPanel {

    static final Color TEXT = new Color(0xf2e6b0);
    static final Color FRAME = new Color(0x4a4a4a);
    static final Color BODY = new Color(0x262626);
    static final Color OLIVE = new Color(0x6b7a14);
    static final Color ORANGE = new Color(0xe8731a);
    static final Color KEYWORD = new Color(0xf0a030);
    static final Color CALL = new Color(0xffe680);

    private static final int PANEL_WIDTH = 430;

    /** The program already in the editor when a game starts: farms the top row with carrots. */
    static final String SAMPLE = """
            while (true) {
                for (int i = 0; i < 6; i++) {
                    if (canHarvest()) {
                        harvest();
                    }
                    plant(Carrot);
                    if (i < 5) {
                        move(East);
                    }
                }
                for (int i = 0; i < 5; i++) {
                    move(West);
                }
            }
            """;

    private final JLabel status = new JLabel("Press the orange ▶ to run your code.");
    private final JTextPane editor = new JTextPane();
    private final SimpleAttributeSet plain = style(TEXT, false);
    private final SimpleAttributeSet keyword = style(KEYWORD, true);
    private final SimpleAttributeSet call = style(CALL, true);
    private final ScriptRunner runner;
    private final Runnable onHelp;
    private final IconButton runBtn;
    private final IconButton pauseBtn;

    /** controller moves the drone; onHelp is run when ? is pressed (GameScreen shows the tutorial). */
    public InterpreterPanel(DroneController controller, Runnable onHelp) {
        this.onHelp = onHelp;
        this.runner = new ScriptRunner(controller, new ScriptRunner.Listener() {
            @Override
            public void onStatus(String message) {
                status.setText(message);
            }

            @Override
            public void onStateChanged() {
                refreshButtons();
            }
        });

        // Run is a triangle (or a square while running = Stop); Pause is two bars (or a triangle = resume)
        runBtn = new IconButton(ORANGE, Color.WHITE, (g, w, h) -> {
            if (runner.isRunning()) {
                g.fillRect(w / 2 - 5, h / 2 - 5, 10, 10);
            } else {
                triangle(g, w, h);
            }
        });
        pauseBtn = new IconButton(ORANGE, Color.WHITE, (g, w, h) -> {
            if (runner.isPaused()) {
                triangle(g, w, h);
            } else {
                g.fillRect(w / 2 - 6, h / 2 - 6, 4, 12);
                g.fillRect(w / 2 + 2, h / 2 - 6, 4, 12);
            }
        });
        IconButton helpBtn = new IconButton(OLIVE, Color.WHITE, (g, w, h) -> {
            g.setStroke(new BasicStroke(2));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("?", (w - fm.stringWidth("?")) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        });
        runBtn.setToolTipText("Run / Stop");
        pauseBtn.setToolTipText("Pause / Resume");
        helpBtn.setToolTipText("How to code the drone");
        runBtn.addActionListener(e -> toggleRun());
        pauseBtn.addActionListener(e -> runner.togglePause());
        helpBtn.addActionListener(e -> showHelp());

        JLabel title = new JLabel("CODE");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(runBtn);
        left.add(pauseBtn);
        left.add(title);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(helpBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        editor.setBackground(BODY);
        editor.setCaretColor(TEXT);
        editor.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        editor.setCharacterAttributes(plain, true);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(InterpreterPanel.this::highlight);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(InterpreterPanel.this::highlight);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // style changes only; nothing to do
            }
        });
        editor.setText(SAMPLE);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BODY);

        status.setForeground(TEXT);
        status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        status.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));

        setLayout(new BorderLayout());
        setBackground(FRAME);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(PANEL_WIDTH, 600));
        add(bar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    /** Stops the running script. Called when the game ends or the player leaves. */
    public void stop() {
        runner.stop();
    }

    // Run starts the code in the editor; while running, the same button stops it.
    private void toggleRun() {
        if (runner.isRunning()) {
            runner.stop();
        } else {
            runner.start(editor.getText());
        }
    }

    // ? pressed: pause the script while the tutorial is open, then carry on where it was.
    private void showHelp() {
        boolean pauseNow = runner.isRunning() && !runner.isPaused();
        if (pauseNow) {
            runner.togglePause();
        }
        onHelp.run();
        if (pauseNow) {
            runner.togglePause();
        }
    }

    // Redraws Run and Pause so they show the right symbol.
    private void refreshButtons() {
        runBtn.repaint();
        pauseBtn.repaint();
    }

    // Colors keywords (if, else, while, for, int) and command names (anything followed by "(").
    private void highlight() {
        StyledDocument doc = editor.getStyledDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, text.length(), plain, true);
            Matcher m = Pattern.compile("\\b(if|else|while|for|int)\\b|\\b[A-Za-z_]\\w*(?=\\()").matcher(text);
            while (m.find()) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), m.group(1) != null ? keyword : call, true);
            }
        } catch (BadLocationException ignored) {
            // the text changed while coloring; the next change colors it again
        }
    }

    // A text style for the editor: monospaced, size 15, this color, bold or not.
    private static SimpleAttributeSet style(Color c, boolean bold) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontFamily(s, Font.MONOSPACED);
        StyleConstants.setFontSize(s, 15);
        StyleConstants.setForeground(s, c);
        StyleConstants.setBold(s, bold);
        return s;
    }

    // The "play" triangle used by Run and by Pause (as "resume").
    private static void triangle(Graphics2D g, int w, int h) {
        g.fillPolygon(new int[]{w / 2 - 4, w / 2 - 4, w / 2 + 6}, new int[]{h / 2 - 6, h / 2 + 6, h / 2}, 3);
    }

    /** A small square button with a symbol drawn on it (Run, Pause, Help). */
    static class IconButton extends JButton {
        /** Draws the symbol inside the button. */
        interface Painter {
            void paint(Graphics2D g, int w, int h);
        }

        private final Color bg;
        private final Color fg;
        private final Painter painter;

        IconButton(Color bg, Color fg, Painter painter) {
            this.bg = bg;
            this.fg = fg;
            this.painter = painter;
            setPreferredSize(new Dimension(28, 28));
            setFocusable(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
        }

        /** Draws the rounded colored square, then the symbol. */
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(bg);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g.setColor(fg);
            painter.paint(g, getWidth(), getHeight());
            g.dispose();
        }
    }
}
