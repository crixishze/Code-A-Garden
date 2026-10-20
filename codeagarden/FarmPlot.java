package codeagarden;

import java.awt.*;
import javax.swing.*;

public class FarmPlot {

    int rows;
    int cols;
    JButton[][] tiles;

    public FarmPlot(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.tiles = new JButton[rows][cols];
    
        setLayout(new GridLayout(rows, cols, 5, 5));

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton tile = new JButton("X");
                tile.setBackground(new Color(139, 90, 43));
                tile.setForeground(Color.WHITE);
                tile.setFocusPainted(false);

                int r = i;
                int c = j;
                tile.addActionListener(e -> onTileClicked(r, c));

                tiles[i][j] = tile;
                add(tile);
            }  
        }
    }
}