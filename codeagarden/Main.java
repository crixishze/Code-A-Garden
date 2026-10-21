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
        setSize(1000, 1000);
        setResizable(false);
        
        //calls FarmPlot class, provides the grid for the plot ROW X COLUMN
        //Didy grid :skull:
        FarmPlot plot = new FarmPlot(3, 3);
        plot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(plot, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);

        //bottombar containing player balance, time left, as well as the shop menu
        BottomBar botBar = new BottomBar(100);
        add(botBar, BorderLayout.SOUTH); //BorderLayout.SOUTH puts it at the bottom  of the display.
    }


}