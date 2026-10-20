package codeagarden;

import java.util.*;
import java.awt.*;
import javax.swing.*;

public class Main extends JFrame {

    public static void main (String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main();
        });
    }

    public Main() throws HeadlessException {
        setTitle("Code A Garden");
        /*setAppIcon(); */
        setSize(600, 600);
        setResizable(false);

        FarmPlot plot = new FarmPlot(3, 3);
        plot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20));
        add(plot, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}