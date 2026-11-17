package codeagarden.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * The short tutorial shown when a NEW GAME starts (and again with the ? button in the code
 * panel): how to use the code editor, the drone commands, the directions, and how the game ends.
 * Back / Next move between the pages; Start Farming closes it.
 */
final class TutorialDialog {

    private static final Dimension SIZE = new Dimension(760, 560);

    private static final String[] TITLES = {
        "1. Welcome to Code A Garden",
        "2. The code panel",
        "3. Moving the drone",
        "4. Planting and harvesting",
        "5. Writing loops and decisions",
        "6. How the game ends"
    };

    private static final String[] PAGES = {
        "Your goal: earn $10,000 before the 30-day season ends.\n"
                + "One day lasts 60 real seconds, so you have 30 minutes.\n\n"
                + "The farm on the left has 4 rows and 6 columns of plots.\n"
                + "You can NOT click the plots. A drone does all the farming,\n"
                + "and the drone only follows code that you write in the panel\n"
                + "on the right.\n\n"
                + "The buttons at the bottom open the Shop (buy seeds),\n"
                + "the Inventory (your seeds), and the Farm again.",

        "The code panel is on the right side of the screen.\n\n"
                + "  ▶  Run    starts your code (the button becomes ■ Stop)\n"
                + "  ❙❙ Pause  pauses the code; press again to continue\n"
                + "  ?      opens this tutorial again\n\n"
                + "The line under the editor shows the command the drone is doing,\n"
                + "for example  plant(Carrot) . Messages under the farm tell you\n"
                + "what happened, for example \"No Carrot seeds! Buy some in the shop.\"\n\n"
                + "Every line ends with ;  and blocks go inside { }.",

        "move(North);   moves the drone one plot UP\n"
                + "move(South);   moves the drone one plot DOWN\n"
                + "move(East);    moves the drone one plot RIGHT\n"
                + "move(West);    moves the drone one plot LEFT\n\n"
                + "The drone starts in the top-left plot.\n"
                + "It cannot leave the farm: at an edge, the move is skipped\n"
                + "and a message says so.\n\n"
                + "Rows go from top to bottom (4 rows), columns from left\n"
                + "to right (6 columns).",

        "These are the methods the drone understands:\n\n"
                + "plant(Carrot);    plants one Carrot seed under the drone\n"
                + "                  (also Corn, Tomato or Pumpkin)\n"
                + "harvest();        sells the ready crop under the drone\n"
                + "canHarvest()      true if the crop under the drone is ready\n\n"
                + "plant uses a seed from your Inventory, so buy seeds in the\n"
                + "Shop first. Planting on a full plot does nothing.\n\n"
                + "Crop     Cost   Grows   Sells\n"
                + "Carrot   $10    3 s     $20\n"
                + "Corn     $20    5 s     $45\n"
                + "Tomato   $30    8 s     $70\n"
                + "Pumpkin  $50    12 s    $120",

        "int x = 0;                    makes a number variable\n"
                + "if (canHarvest()) { ... }     does something only when true\n"
                + "if (x < 3) { ... } else { ... }\n"
                + "while (true) { ... }          repeats forever (until Stop)\n"
                + "for (int i = 0; i < 6; i++) { ... }   repeats 6 times\n\n"
                + "You can use  + - * /  and  < <= > >= == !=\n\n"
                + "Example: plant a whole row going East\n"
                + "for (int i = 0; i < 6; i++) {\n"
                + "    plant(Carrot);\n"
                + "    move(East);\n"
                + "}",

        "The game ends by itself:\n\n"
                + "  WIN   as soon as your balance reaches $10,000\n"
                + "  LOSE  when day 30 is over and you have less than $10,000\n\n"
                + "When it ends, the drone stops and you can no longer farm.\n"
                + "You can then Play Again or go back to the Main Menu.\n"
                + "Your result is saved for the leaderboard.\n\n"
                + "Good luck, and happy farming!"
    };

    private TutorialDialog() {
        // Only a static method.
    }

    /** Shows the tutorial and waits until the player closes it. */
    static void show(Window owner) {
        JDialog dialog = new JDialog(owner, "How to play", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel screen = UIFactory.createScreenPanel(new BorderLayout(0, 12), 16, 20, 16, 20);

        JLabel title = UIFactory.createTitle(TITLES[0]);
        JTextArea text = new JTextArea(PAGES[0]);
        text.setFont(Theme.PIXEL.deriveFont(15f));
        text.setForeground(Theme.WOOD_DARK);
        text.setBackground(Theme.PARCHMENT);
        text.setEditable(false);
        text.setFocusable(false);
        text.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.WOOD_LIGHT, 4));

        JLabel pageNumber = UIFactory.createLabel("", 14f, Theme.PARCHMENT);
        JButton back = UIFactory.createButton("Back", 120, 40, 15f);
        JButton next = UIFactory.createButton("Next", 180, 40, 15f);
        int[] page = {0};   // an array so the button actions below can change it

        Runnable showPage = () -> {
            title.setText(TITLES[page[0]]);
            text.setText(PAGES[page[0]]);
            text.setCaretPosition(0);
            pageNumber.setText("Page " + (page[0] + 1) + " of " + PAGES.length);
            UIFactory.setButtonEnabled(back, page[0] > 0);
            next.setText(page[0] == PAGES.length - 1 ? "Start Farming" : "Next");
        };
        back.addActionListener(e -> {
            page[0]--;
            showPage.run();
        });
        next.addActionListener(e -> {
            if (page[0] == PAGES.length - 1) {
                dialog.dispose();
            } else {
                page[0]++;
                showPage.run();
            }
        });
        showPage.run();

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(next);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(pageNumber, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        screen.add(title, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        dialog.setContentPane(screen);
        dialog.setSize(SIZE);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // modal: waits here until the dialog is closed
    }
}
