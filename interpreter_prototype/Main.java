import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.regex.*;

public class Main extends JFrame {
    static final Color TEXT = new Color(0xf2e6b0);
    static final Color FRAME = new Color(0x4a4a4a);
    static final Color BODY = new Color(0x262626);
    static final Color YELLOW = new Color(0xe6e04a);
    static final Color OLIVE = new Color(0x6b7a14);
    static final Color ORANGE = new Color(0xe8731a);
    static final Color KEYWORD = new Color(0xf0a030);
    static final Color CALL    = new Color(0xffe680);

    static final String SAMPLE = """
            while (true) {
                for (int i = 0; i < 3; i++) {
                    harvest();
                    plant(Carrots);
                    move(North);
                }
                move(East);
                for (int i = 0; i < 3; i++) {
                    if (canHarvest()) {
                        harvest();
                        plant(Bush);
                        move(North);
                    }
                }
                move(East);
            }
            """;

    private final JLabel status = new JLabel(" ");
    private final JTextPane editor = new JTextPane();
    private final SimpleAttributeSet plain = style(TEXT, false);
    private final SimpleAttributeSet keyword = style(KEYWORD, true);
    private final SimpleAttributeSet call = style(CALL, true);
    private IconButton closeBtn;
    private IconButton minBtn;
    private IconButton pauseBtn;
    private IconButton runBtn;
    private Interpreter interp;

    Main() {
        setUndecorated(true);
        setSize(380, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        closeBtn = new IconButton(OLIVE, YELLOW, (g, w, h) -> {
            g.setStroke(new BasicStroke(3));
            g.drawLine(w / 2 - 5, h / 2 - 5, w / 2 + 5, h / 2 + 5);
            g.drawLine(w / 2 + 5, h / 2 - 5, w / 2 - 5, h / 2 + 5);
        });
        minBtn = new IconButton(OLIVE, YELLOW, (g, w, h) -> g.fillRect(w / 2 - 6, h / 2 + 3, 12, 3));
        pauseBtn = new IconButton(ORANGE, Color.WHITE, (g, w, h) -> {
            if (interp != null && interp.paused) triangle(g, w, h);
            else {
                g.fillRect(w / 2 - 6, h / 2 - 6, 4, 12);
                g.fillRect(w / 2 + 2, h / 2 - 6, 4, 12);
            }
        });
        runBtn = new IconButton(ORANGE, Color.WHITE, (g, w, h) -> {
            if (interp != null) g.fillRect(w / 2 - 5, h / 2 - 5, 10, 10);
            else triangle(g, w, h);
        });
        closeBtn.addActionListener(e -> System.exit(0));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        pauseBtn.addActionListener(e -> togglePause());
        runBtn.addActionListener(e -> toggleRun());

        JLabel title = new JLabel("f1");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(runBtn);
        left.add(pauseBtn);
        left.add(title);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(minBtn); right.add(closeBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            Point grab;
            public void mousePressed(MouseEvent e) {
                grab = new Point(e.getXOnScreen() - getX(), e.getYOnScreen() - getY());
            }
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - grab.x, e.getYOnScreen() - grab.y);
            }
        };
        for (JComponent c : new JComponent[]{bar, left, right, title}) {
            c.addMouseListener(drag);
            c.addMouseMotionListener(drag);
        }

        editor.setBackground(BODY);
        editor.setCaretColor(TEXT);
        editor.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        editor.setCharacterAttributes(plain, true);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){ 
                SwingUtilities.invokeLater(Main.this::highlight);}
            public void removeUpdate(DocumentEvent e){ 
                SwingUtilities.invokeLater(Main.this::highlight);}
            public void changedUpdate(DocumentEvent e){}
        });
        editor.setText(SAMPLE);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BODY);

        status.setForeground(TEXT);
        status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        status.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(FRAME);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(bar, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void toggleRun(){
        if (interp != null) {
            interp.stopped = true;
            return;
        }

        List<Stmt> program;
        try {
            program = new Parser(Lexer.lex(editor.getText())).parse();
        } catch (RuntimeException ex) {
            say("Couldn't read the script");
            return;
        }

        Interpreter it = new Interpreter(this::doAction);
        interp = it;
        refreshButtons();
        say("Running");
        Thread t = new Thread(() -> {
            try {
                it.run(program);
            } finally {
                SwingUtilities.invokeLater(() -> { interp = null; refreshButtons(); say("Stopped"); });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void togglePause(){
        if (interp == null) return;
        interp.paused = !interp.paused;
        refreshButtons();
        say(interp.paused ? "Paused" : "Running");
    }

    private Object doAction(String name, List<Object> args){
        say(name + "(" + String.join(", ", args.stream().map(a -> "" + a).toList()) + ")");
        Interpreter.sleep(300);
        if (name.equals("canHarvest")) 
            return Math.random() < 0.5;

        return 0;
    }

    private void say(String msg) {
        SwingUtilities.invokeLater(() -> status.setText(msg));
    }

    private void refreshButtons(){
        runBtn.repaint();
        pauseBtn.repaint();
    }

    private void highlight(){
        StyledDocument doc = editor.getStyledDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, text.length(), plain, true);
            Matcher m = Pattern.compile("\\b(if|else|while|for|int)\\b|\\b[A-Za-z_]\\w*(?=\\()").matcher(text);
            while (m.find()) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), m.group(1) != null ? keyword : call, true);
            }
        } catch (BadLocationException ignored){}
    }

    private static SimpleAttributeSet style(Color c, boolean bold){
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontFamily(s, Font.MONOSPACED);
        StyleConstants.setFontSize(s, 15);
        StyleConstants.setForeground(s, c);
        StyleConstants.setBold(s, bold);
        return s;
    }

    private static void triangle(Graphics2D g, int w, int h){
        g.fillPolygon(new int[]{w / 2 - 4, w / 2 - 4, w / 2 + 6}, new int[]{h / 2 - 6, h / 2 + 6, h / 2}, 3);
    }

    static class IconButton extends JButton{
        interface Painter{void paint(Graphics2D g, int w, int h); }
        private final Color bg, fg;
        private final Painter painter;

        IconButton(Color bg, Color fg, Painter painter){
            this.bg = bg; this.fg = fg; this.painter = painter;
            setPreferredSize(new Dimension(28, 28));
            setFocusable(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
        }

        @Override protected void paintComponent(Graphics g0){
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(bg);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g.setColor(fg);
            painter.paint(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}