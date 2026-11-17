package codeagarden.ui;

import codeagarden.data.Report;
import codeagarden.data.ReportResult;
import codeagarden.data.SaveException;
import codeagarden.data.SaveRepository;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * The REPORTS window (from the main menu): a list of business questions on the left.
 * Clicking one runs its SQL query through SaveRepository (JDBC) and shows the answer as a
 * table on the right. The player never needs to know SQL.
 */
final class ReportsDialog {

    private static final Dimension SIZE = new Dimension(1000, 620);

    private ReportsDialog() {
        // Only static methods.
    }

    /** Opens the REPORTS window and waits until it is closed. */
    static void show(Window owner) {
        SaveRepository repository = new SaveRepository();
        List<Report> reports = repository.getReports();

        JDialog dialog = new JDialog(owner, "Reports", JDialog.ModalityType.APPLICATION_MODAL);
        JPanel screen = UIFactory.createScreenPanel(new BorderLayout(12, 12), 16, 16, 16, 16);
        screen.add(UIFactory.createTitle("REPORTS"), BorderLayout.NORTH);

        JList<Report> list = new JList<>(reports.toArray(new Report[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(Theme.PIXEL.deriveFont(14f));
        list.setBackground(Theme.PARCHMENT);
        list.setForeground(Theme.WOOD_DARK);
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(360, 0));

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   // reports are read-only
            }
        };
        JTable table = new JTable(model);
        table.setFont(Theme.PIXEL.deriveFont(14f));
        table.setRowHeight(24);
        table.getTableHeader().setFont(Theme.PIXEL.deriveFont(14f));
        JLabel info = UIFactory.createLabel("Pick a report on the left.", 14f, Theme.PARCHMENT);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(info, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                runReport(dialog, repository, list.getSelectedValue(), model, info);
            }
        });

        JButton close = UIFactory.createButton("CLOSE", 160, 44, 18f);
        close.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(close);

        screen.add(listScroll, BorderLayout.WEST);
        screen.add(right, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        dialog.setContentPane(screen);
        dialog.setSize(SIZE);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // Asks for the typed value if needed, runs the report, and fills the table (or explains the problem).
    private static void runReport(JDialog dialog, SaveRepository repository, Report report,
                                  DefaultTableModel model, JLabel info) {
        String input = null;
        if (report.needsInput()) {
            input = JOptionPane.showInputDialog(dialog, report.getInputPrompt(), report.getTitle(),
                    JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return;   // Cancel pressed
            }
        }
        try {
            ReportResult result = repository.runReport(report, input);
            model.setDataVector(toArray(result.getRows()), result.getColumns().toArray());
            info.setText(report.getTitle() + "  -  " + (result.isEmpty() ? "no rows found" : result.getRows().size()
                    + " row(s)") + "  -  SQL: " + report.getFeatures());
        } catch (SaveException e) {
            model.setDataVector(new Object[0][0], new Object[0]);
            info.setText("Could not run the report.");
            JOptionPane.showMessageDialog(dialog, e.getMessage(), "Report unavailable", JOptionPane.WARNING_MESSAGE);
        }
    }

    // A JTable model wants Object[row][column]; the result is a list of lists.
    private static Object[][] toArray(List<List<Object>> rows) {
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i).toArray();
        }
        return data;
    }
}
