package codeagarden.ui;

import codeagarden.game.GameSession;
import codeagarden.game.StoryModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * The story screen. Each chapter is read page by page with the Next button; on the last page
 * the replies appear, and after picking one, Continue goes to the next chapter.
 * The chapters and progress live in StoryModel (inside GameSession).
 */
public class StoryPanel extends JPanel {

    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 42;
    private static final float BUTTON_FONT_SIZE = 15f;

    private final GameSession session = GameSession.getInstance();
    private final Runnable onMoneyChanged;
    private final JLabel chapter = new JLabel();
    private final JTextArea dialogue = new JTextArea();
    private final JLabel feedback = new JLabel(" ");
    private final JPanel choices = new JPanel(new GridLayout(0, 1, 0, 8));
    private final JButton nextButton = UIFactory.createButton("Next", BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_FONT_SIZE);
    private int pageIndex;   // which page of the current chapter is shown (0 = first)

    /** onMoneyChanged runs after a reply that changes the balance (so the bottom bar updates). */
    public StoryPanel(Runnable onMoneyChanged) {
        this.onMoneyChanged = onMoneyChanged;
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.WOOD_DARK);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        chapter.setFont(Theme.PIXEL.deriveFont(24f));
        chapter.setForeground(Theme.TITLE_ORANGE);
        add(chapter, BorderLayout.NORTH);

        dialogue.setFont(Theme.PIXEL.deriveFont(15f));
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
        nextButton.addActionListener(e -> onNextPressed());
        action.add(nextButton, BorderLayout.EAST);
        bottom.add(action, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        if (session.getStory().isCompleted()) {   // possible after loading a saved game
            showStoryComplete();
        } else {
            showChapter();
        }
    }

    // Starts the current chapter at its first page.
    private void showChapter() {
        pageIndex = 0;
        showPage();
    }

    // Shows the current page. The last page also shows the reply buttons.
    private void showPage() {
        StoryModel story = session.getStory();
        StoryModel.StoryScene scene = story.currentScene();
        int pageCount = scene.pages().size();
        chapter.setText(scene.title() + "  (" + story.getSceneNumber() + "/" + story.getSceneCount() + ")");
        dialogue.setText(scene.pages().get(pageIndex));
        choices.removeAll();

        if (pageIndex < pageCount - 1) {   // more pages to read: Next
            feedback.setText("Page " + (pageIndex + 1) + " of " + pageCount);
            nextButton.setText("Next");
            UIFactory.setButtonEnabled(nextButton, true);
        } else {                           // last page: the replies
            for (int i = 0; i < scene.replies().size(); i++) {
                int replyIndex = i;
                JButton reply = UIFactory.createButton(scene.replies().get(i).text(), BUTTON_WIDTH, BUTTON_HEIGHT,
                        BUTTON_FONT_SIZE);
                reply.addActionListener(e -> chooseReply(replyIndex));
                choices.add(reply);
            }
            feedback.setText("Choose a reply to continue.");
            nextButton.setText("Continue");
            UIFactory.setButtonEnabled(nextButton, false);
        }
        revalidate();
        repaint();
    }

    // Next turns the page; on the last page (after a reply) it moves to the next chapter.
    private void onNextPressed() {
        int pageCount = session.getStory().currentScene().pages().size();
        if (pageIndex < pageCount - 1) {
            pageIndex++;
            showPage();
        } else {
            advanceStory();
        }
    }

    // Applies the reply's reward or cost and shows what happened.
    private void chooseReply(int replyIndex) {
        StoryModel.StoryReply reply = session.getStory().chooseReply(replyIndex);
        session.applyConsequence(reply.consequence());
        onMoneyChanged.run();
        feedback.setText("Consequence: " + describe(reply.consequence()));
        for (Component component : choices.getComponents()) {
            component.setEnabled(false);
        }
        UIFactory.setButtonEnabled(nextButton, true);
    }

    // Turns a consequence into one sentence, e.g. "+$5 balance. A quick harvest ...".
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

    // Goes to the next chapter, or shows the "story complete" page after the last one.
    private void advanceStory() {
        if (session.getStory().advance()) {
            showChapter();
            return;
        }
        showStoryComplete();
    }

    // The page shown after the last chapter: the game itself ends when the money goal or day 30 is reached.
    private void showStoryComplete() {
        chapter.setText("The Story So Far");
        dialogue.setText(session.getStory().getFinalOutcome()
                + "\n\nEvery chapter is done. Now the festival depends on the garden:\n"
                + "reach $10,000 before day 30 ends and the ending will start by itself.");
        choices.removeAll();
        feedback.setText("Your choices will be remembered at the festival.");
        nextButton.setText("Continue");
        UIFactory.setButtonEnabled(nextButton, false);
        revalidate();
        repaint();
    }
}
