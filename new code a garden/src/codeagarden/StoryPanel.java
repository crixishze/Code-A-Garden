package codeagarden;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** The story screen. The chapters and progress live in StoryModel (inside GameSession). */
public class StoryPanel extends JPanel {

    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 42;
    private static final float BUTTON_FONT_SIZE = 15f;

    private final GameSession session = GameSession.getInstance();
    private final Runnable onMoneyChanged;
    private final JLabel chapter = new JLabel();
    private final JTextArea dialogue = new JTextArea();
    private final JLabel feedback = new JLabel("Choose a reply to continue.");
    private final JPanel choices = new JPanel(new GridLayout(0, 1, 0, 8));
    private final JButton continueButton = UIFactory.createButton("Continue", BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);

    /** onMoneyChanged runs after a reply that changes the balance (so the bottom bar updates). */
    public StoryPanel(Runnable onMoneyChanged) {
        this.onMoneyChanged = onMoneyChanged;
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        chapter.setFont(Theme.PIXEL.deriveFont(28f));
        chapter.setForeground(Theme.TITLE_ORANGE);
        add(chapter, BorderLayout.NORTH);

        dialogue.setFont(Theme.PIXEL.deriveFont(17f));
        dialogue.setForeground(Theme.WOOD_DARK);
        dialogue.setBackground(Theme.PARCHMENT);
        dialogue.setLineWrap(true);
        dialogue.setWrapStyleWord(true);
        dialogue.setEditable(false);
        dialogue.setFocusable(false);
        dialogue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.WOOD_LIGHT, 4),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        add(dialogue, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 12));
        bottom.setOpaque(false);
        choices.setOpaque(false);
        bottom.add(choices, BorderLayout.CENTER);

        JPanel action = new JPanel(new BorderLayout(8, 8));
        action.setOpaque(false);
        feedback.setFont(Theme.PIXEL.deriveFont(14f));
        feedback.setForeground(Theme.PARCHMENT);
        action.add(feedback, BorderLayout.CENTER);
        continueButton.addActionListener(e -> advanceStory());
        action.add(continueButton, BorderLayout.EAST);
        bottom.add(action, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        if (session.getStory().isCompleted()) {   // possible after loading a saved game
            showStoryComplete();
        } else {
            refresh();
        }
    }

    private void refresh() {
        StoryModel story = session.getStory();
        StoryModel.StoryScene scene = story.currentScene();
        chapter.setText(scene.title() + "  (" + story.getSceneNumber() + "/" + story.getSceneCount() + ")");
        dialogue.setText(scene.dialogue());
        choices.removeAll();
        for (int i = 0; i < scene.replies().size(); i++) {
            int replyIndex = i;
            JButton reply = UIFactory.createButton(scene.replies().get(i).text(), BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
            reply.addActionListener(e -> chooseReply(replyIndex));
            choices.add(reply);
        }
        feedback.setText("Choose a reply to continue.");
        UIFactory.setButtonEnabled(continueButton, false);
        revalidate();
        repaint();
    }

    private void chooseReply(int replyIndex) {
        StoryModel.StoryReply reply = session.getStory().chooseReply(replyIndex);
        session.applyConsequence(reply.consequence());
        onMoneyChanged.run();
        feedback.setText("Consequence: " + describe(reply.consequence()));
        for (Component component : choices.getComponents()) {
            component.setEnabled(false);
        }
        UIFactory.setButtonEnabled(continueButton, true);
    }

    private String describe(StoryModel.StoryConsequence consequence) {
        if (consequence.balanceDelta() > 0) {
            return "+$" + consequence.balanceDelta() + " balance. " + consequence.outcomeMessage();
        }
        if (consequence.balanceDelta() < 0) {
            return "-$" + (-consequence.balanceDelta()) + " balance. " + consequence.outcomeMessage();
        }
        if (consequence.seed() != null) {
            return "Received 1 " + consequence.seed().getName() + " seed. " + consequence.outcomeMessage();
        }
        return consequence.outcomeMessage();
    }

    private void advanceStory() {
        if (session.getStory().advance()) {
            refresh();
            return;
        }
        showStoryComplete();
    }

    private void showStoryComplete() {
        chapter.setText("Garden Story Complete");
        dialogue.setText(session.getStory().getFinalOutcome()
                + "\n\nKeep tending the plots and reach $10,000 before the 30-day season ends.");
        choices.removeAll();
        feedback.setText("Your choices will be remembered at the festival.");
        UIFactory.setButtonEnabled(continueButton, false);
        revalidate();
        repaint();
    }
}
