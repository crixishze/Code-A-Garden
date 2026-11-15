package codeagarden;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/** The one fullscreen helper for every window: F11 toggles full screen, Esc leaves it. */
public final class FullscreenHelper {

    private final JFrame frame;
    private boolean fullScreen = false;
    private Rectangle windowedBounds;

    public FullscreenHelper(JFrame frame) {
        this.frame = frame;
        InputMap inputs = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = frame.getRootPane().getActionMap();
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullScreen");
        actions.put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFullScreen(!fullScreen);
            }
        });
        actions.put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fullScreen) {
                    setFullScreen(false);
                }
            }
        });
    }

    public void setFullScreen(boolean on) {
        if (on == fullScreen) {
            return;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (on) {
            windowedBounds = frame.getBounds();   // remember the normal size to come back to
        }
        frame.dispose();                          // window decoration can only change while it is not showing
        frame.setUndecorated(on);
        if (on) {
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
            frame.setBounds(windowedBounds);
        }
        frame.setVisible(true);
        fullScreen = on;
        frame.revalidate();
        frame.repaint();
    }
}
