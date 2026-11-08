package codeagarden;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Swing view for the garden story; game state changes are delegated to Main. */
public class StoryPanel extends JPanel {
    private static final Color WOOD_DARK = Color.decode("#5b3a1e");
    private static final Color WOOD_LIGHT = Color.decode("#a0713f");
    private static final Color PARCHMENT = Color.decode("#f3dfa2");
    private static final Color TITLE_ORANGE = Color.decode("#ff8c1a");
    private static final Color BTN_GREEN = Color.decode("#4cc23a");
    private static final Color BTN_BORDER = Color.decode("#1e6b17");
    private static final Font PIXEL = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private final StoryModel story;
    private final Consumer<StoryModel.StoryConsequence> consequenceHandler;
    private final JLabel chapter = new JLabel();
    private final JTextArea dialogue = new JTextArea();
    private final JLabel feedback = new JLabel("Choose a reply to continue.");
    private final JPanel choices = new JPanel(new GridLayout(0, 1, 0, 8));
    private final JButton continueButton = new JButton("Continue");

    public StoryPanel(StoryModel story, Consumer<StoryModel.StoryConsequence> consequenceHandler) {
        this.story = story;
        this.consequenceHandler = consequenceHandler;
        setLayout(new BorderLayout(12, 12));
        setBackground(WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        chapter.setFont(PIXEL.deriveFont(28f));
        chapter.setForeground(TITLE_ORANGE);
        add(chapter, BorderLayout.NORTH);
        dialogue.setFont(PIXEL.deriveFont(17f));
        dialogue.setForeground(WOOD_DARK);
        dialogue.setBackground(PARCHMENT);
        dialogue.setLineWrap(true);
        dialogue.setWrapStyleWord(true);
        dialogue.setEditable(false);
        dialogue.setFocusable(false);
        dialogue.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(WOOD_LIGHT, 4), BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        add(dialogue, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 12));
        bottom.setOpaque(false);
        choices.setOpaque(false);
        bottom.add(choices, BorderLayout.CENTER);
        JPanel action = new JPanel(new BorderLayout(8, 8));
        action.setOpaque(false);
        feedback.setFont(PIXEL.deriveFont(14f));
        feedback.setForeground(PARCHMENT);
        action.add(feedback, BorderLayout.CENTER);
        styleButton(continueButton);
        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> advanceStory());
        action.add(continueButton, BorderLayout.EAST);
        bottom.add(action, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    private void refresh() {
        StoryModel.StoryScene scene = story.currentScene();
        chapter.setText(scene.title() + "  (" + story.getSceneNumber() + "/" + story.getSceneCount() + ")");
        dialogue.setText(scene.dialogue());
        choices.removeAll();
        for (int i = 0; i < scene.replies().size(); i++) {
            int replyIndex = i;
            JButton reply = new JButton(scene.replies().get(i).text());
            styleButton(reply);
            reply.addActionListener(e -> chooseReply(replyIndex));
            choices.add(reply);
        }
        feedback.setText("Choose a reply to continue.");
        continueButton.setText("Continue");
        continueButton.setEnabled(false);
        revalidate();
        repaint();
    }

    private void chooseReply(int replyIndex) {
        StoryModel.StoryReply reply = story.chooseReply(replyIndex);
        consequenceHandler.accept(reply.consequence());
        feedback.setText("Consequence: " + describe(reply.consequence()));
        for (Component component : choices.getComponents()) component.setEnabled(false);
        continueButton.setEnabled(true);
    }

    private String describe(StoryModel.StoryConsequence consequence) {
        if (consequence.balanceDelta() > 0) return "+$" + consequence.balanceDelta() + " balance. " + consequence.outcomeMessage();
        if (consequence.balanceDelta() < 0) return "-$" + (-consequence.balanceDelta()) + " balance. " + consequence.outcomeMessage();
        if (consequence.seed() != null) return "Received 1 " + consequence.seed().getName() + " seed. " + consequence.outcomeMessage();
        return consequence.outcomeMessage();
    }

    private void advanceStory() {
        if (story.advance()) {
            refresh();
            return;
        }
        chapter.setText("Garden Story Complete");
        dialogue.setText(story.getFinalOutcome() + "\n\nKeep tending the plots and reach $10,000 before the 30-day season ends.");
        choices.removeAll();
        feedback.setText("Your choices will be remembered at the festival.");
        continueButton.setEnabled(false);
        revalidate();
        repaint();
    }

    private void styleButton(JButton button) {
        button.setFont(PIXEL.deriveFont(15f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(BTN_GREEN);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 3));
        button.setPreferredSize(new Dimension(180, 42));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
