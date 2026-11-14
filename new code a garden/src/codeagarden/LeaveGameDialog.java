package codeagarden;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * The pop-up shown when the player leaves a game (MENU button or closing the window).
 * With a logged-in player:  SAVE AND QUIT / QUIT / CANCEL.
 * Without one (no database): YES / NO, because there is nothing to save to.
 * Replaces the old ReturnToMenuDialog.
 */
final class LeaveGameDialog {

    /** What the player picked. */
    enum Choice { SAVE_AND_LEAVE, LEAVE, CANCEL }

    private static final int POPUP_WIDTH = 600;
    private static final int CHOICE_BUTTON_HEIGHT = 150;
    private static final int CANCEL_BUTTON_WIDTH = 190;
    private static final float CHOICE_FONT_SIZE = 25f;
    private static final float TWO_LINE_FONT_SIZE = 19f;   // "SAVE AND / QUIT" must fit the same button
    private static final float CANCEL_FONT_SIZE = 22f;
    private static final double TEXT_CENTER_Y = 0.48;   // text sits slightly above the picture's middle
    private static final double LINE_SPACING = 0.85;

    private LeaveGameDialog() {
        // Only a static method.
    }

    /**
     * Shows the pop-up and waits until the player picks something.
     * firstLine/secondLine is the question, e.g. "Save before going" / "to the menu?".
     */
    static Choice ask(Window owner, String firstLine, String secondLine, boolean canSave) {
        BufferedImage containerImage = Resources.loadImage("/images/Container1.png");
        BufferedImage greenImage = Resources.loadImage("/images/GreenButton.png");
        BufferedImage redImage = Resources.loadImage("/images/RedButton.png");
        BufferedImage woodImage = Resources.loadImage("/images/RoundButton2_wood.png");

        JDialog dialog = new JDialog(owner, "Leave Game", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        Choice[] picked = {Choice.CANCEL};   // an array so the button actions below can change it

        // Keep the picture's own proportions so its rounded corners are not distorted.
        int popupHeight = (int) Math.round(POPUP_WIDTH * (double) containerImage.getHeight() / containerImage.getWidth());

        JPanel content = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Drawing.smooth(g2);
                g2.drawImage(containerImage, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(58, 38, 48, 38));

        JPanel choices = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        choices.setOpaque(false);
        choices.add(choiceButton(canSave ? "SAVE AND\nQUIT" : "YES", greenImage, CHOICE_BUTTON_HEIGHT,
                canSave ? TWO_LINE_FONT_SIZE : CHOICE_FONT_SIZE,
                canSave ? Choice.SAVE_AND_LEAVE : Choice.LEAVE, picked, dialog));
        choices.add(choiceButton(canSave ? "QUIT" : "NO", redImage, CHOICE_BUTTON_HEIGHT, CHOICE_FONT_SIZE,
                canSave ? Choice.LEAVE : Choice.CANCEL, picked, dialog));
        choices.setPreferredSize(new Dimension(500, CHOICE_BUTTON_HEIGHT + 8));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 20, 0);
        content.add(new PromptLabel(firstLine, secondLine), c);

        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.NONE;
        content.add(choices, c);

        if (canSave) {
            int cancelHeight = (int) Math.round(CANCEL_BUTTON_WIDTH * (double) woodImage.getHeight() / woodImage.getWidth());
            JButton cancel = choiceButton("CANCEL", woodImage, cancelHeight, CANCEL_FONT_SIZE, Choice.CANCEL, picked, dialog);
            cancel.setForeground(Theme.MENU_TEXT_CREAM);
            c.gridy = 2;
            c.insets = new Insets(14, 0, 0, 0);
            content.add(cancel, c);
        }

        dialog.setContentPane(content);
        dialog.setSize(POPUP_WIDTH, popupHeight);
        dialog.setMinimumSize(new Dimension(POPUP_WIDTH, popupHeight));
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);   // modal: this line waits until the dialog is closed
        return picked[0];
    }

    private static JButton choiceButton(String text, BufferedImage image, int height, float fontSize,
                                        Choice choice, Choice[] picked, JDialog dialog) {
        ChoiceButton button = new ChoiceButton(text, image, height, fontSize);
        button.addActionListener(e -> {
            picked[0] = choice;
            dialog.dispose();
        });
        return button;
    }

    /** A button drawn from a picture, with its text centered on it. */
    private static class ChoiceButton extends JButton {
        private final BufferedImage image;

        ChoiceButton(String text, BufferedImage image, int height, float fontSize) {
            super(text);
            this.image = image;
            setFont(Theme.DIALOG.deriveFont(fontSize));
            setForeground(Color.BLACK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int width = (int) Math.round(height * (double) image.getWidth() / image.getHeight());
            Dimension size = new Dimension(width, height);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Drawing.smooth(g2);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            // "\n" in the text makes a second line (the pixel font is too wide for "SAVE AND QUIT" on one)
            String[] lines = getText().split("\n");
            double lineShare = g2.getFontMetrics(getFont()).getHeight() * LINE_SPACING / getHeight();
            for (int i = 0; i < lines.length; i++) {
                double centerY = TEXT_CENTER_Y + (i - (lines.length - 1) / 2.0) * lineShare;
                Drawing.drawCenteredText(g2, lines[i], getFont(), getForeground(), getWidth(), getHeight(), centerY);
            }
            g2.dispose();
        }
    }

    /** The two-line question, drawn directly so the custom font is used reliably. */
    private static class PromptLabel extends JComponent {
        private final String firstLine;
        private final String secondLine;

        PromptLabel(String firstLine, String secondLine) {
            this.firstLine = firstLine;
            this.secondLine = secondLine;
            Dimension size = new Dimension(480, 100);
            setPreferredSize(size);
            setMinimumSize(size);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Drawing.smooth(g2);
            g2.setFont(Theme.DIALOG.deriveFont(28f));
            g2.setColor(Color.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            int firstX = (getWidth() - fm.stringWidth(firstLine)) / 2;
            int secondX = (getWidth() - fm.stringWidth(secondLine)) / 2;
            int firstY = getHeight() / 2 - fm.getHeight() / 2 + fm.getAscent() - 8;
            int secondY = firstY + fm.getHeight();
            g2.drawString(firstLine, firstX, firstY);
            g2.drawString(secondLine, secondX, secondY);
            g2.dispose();
        }
    }
}
