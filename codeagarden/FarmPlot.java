package codeagarden;

import java.awt.*;
import javax.swing.*;

public class FarmPlot extends JPanel {

    private final int rows;
    private final int cols;
    private final JButton[][] tiles;

    public FarmPlot(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.tiles = new JButton[rows][cols];

        setLayout(new GridLayout(rows, cols, 5, 5)); //5px gaps between tiles

        //2D array for farm plot.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton tile = new JButton("X"); //creates a tile "X"
                tile.setBackground(new Color(139, 90, 43)); // soil brown
                tile.setForeground(Color.WHITE);
                tile.setFocusPainted(false);

                final int r = i; //sets the row and column to the current tile position
                final int c = j;
                tile.addActionListener(e -> onTileClicked(r, c)); //the tile clicked will be replaced with an "O"

                tiles[i][j] = tile; //sets tile "X" for the current position
                add(tile); //adds the tile
            }
        }
    }

    private void onTileClicked(int row, int col) {
        //replaces "X" with "O"
        tiles[row][col].setText("O");
        tiles[row][col].setBackground(new Color(34, 139, 34)); // green
    }

}
