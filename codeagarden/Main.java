package codeagarden;

import java.util.*;
import java.awt.*;
import javax.swing.*;


public class Main extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }

    public Main() throws HeadlessException {
        setTitle("Code A Garden");
        /*setAppIcon(); */
        setSize(1000, 1000);
        setResizable(false);

        //calls FarmPlot class, provides the grid for the plot Row X Column
        FarmPlot plot = new FarmPlot(10, 10);
        plot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(plot, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //bottombar containing player balance, time left, as well as the shop menu
        BottomBar botBar = new BottomBar(100);
        add(botBar, BorderLayout.SOUTH);

        setVisible(true);
    }
}
