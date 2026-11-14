package codeagarden;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/** The LEADERBOARD window: every player and their currency, highest first. */
final class LeaderboardDialog {

    private static final Dimension SIZE = new Dimension(520, 660);
    private static final float ROW_FONT_SIZE = 20f;
    private static final int RANK_WIDTH = 60;

    private LeaderboardDialog() {
        // Only a static method.
    }

    static void show(Window owner) {
        List<LeaderboardEntry> entries;
        try {
            entries = new SaveRepository().getLeaderboard();   // already sorted by the SQL (ORDER BY currency DESC)
        } catch (SaveException e) {
            JOptionPane.showMessageDialog(owner, e.getMessage(), "Leaderboard unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(owner, "Leaderboard", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel screen = UIFactory.createScreenPanel(new BorderLayout(0, 12), 16, 16, 16, 16);
        screen.add(UIFactory.createTitle("LEADERBOARD"), BorderLayout.NORTH);
        screen.add(createList(entries), BorderLayout.CENTER);

        JButton close = UIFactory.createButton("CLOSE", 160, 44, 18f);
        close.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(close);
        screen.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(screen);
        dialog.setSize(SIZE);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JScrollPane createList(List<LeaderboardEntry> entries) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.WOOD_DARK);
        if (entries.isEmpty()) {
            list.add(UIFactory.createLabel("No players yet. Start a NEW GAME!", ROW_FONT_SIZE, Theme.PARCHMENT));
        }
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            list.add(createRow(rank, entry));
            rank++;
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.WOOD_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // One parchment row: rank on the left, name in the middle, currency on the right.
    private static JPanel createRow(int rank, LeaderboardEntry entry) {
        JPanel row = UIFactory.createRow(rank == 1);   // the leader gets the highlighted row
        JLabel rankLabel = UIFactory.createLabel("#" + rank, ROW_FONT_SIZE, Theme.WOOD_DARK);
        rankLabel.setPreferredSize(new Dimension(RANK_WIDTH, 0));
        JLabel name = UIFactory.createLabel(entry.getPlayerName(), ROW_FONT_SIZE, Theme.WOOD_DARK);
        JLabel currency = UIFactory.createLabel("$" + String.format("%,d", entry.getCurrency()),
                ROW_FONT_SIZE, Theme.WOOD_DARK);
        currency.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(rankLabel, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(currency, BorderLayout.EAST);
        return row;
    }
}
