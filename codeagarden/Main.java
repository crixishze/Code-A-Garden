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

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //bottombar containing player balance, time left, as well as the shop menu
        BottomBar botBar = new BottomBar(100);
        add(botBar, BorderLayout.SOUTH);

        //calls FarmPlot class, provides the grid for the plot Row X Column
        FarmPlot plot = new FarmPlot(10, 10, new Seed("Carrot", 10, 3, 20), botBar::addBalance);
        plot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(plot, BorderLayout.CENTER);

        setVisible(true);
    }
}
