package codeagarden;

import java.awt.*;
import java.util.function.IntConsumer;
import javax.swing.*;

public class FarmPlot extends JPanel {

    private enum TileState {
        EMPTY,
        GROWING,
        READY
    }

    private static final Color SOIL_COLOR = new Color(139, 90, 43);
    private static final Color GROWING_COLOR = new Color(218, 165, 32);
    private static final Color READY_COLOR = new Color(34, 139, 34);

    private final int rows;
    private final int cols;
    private final JButton[][] tiles;
    private final TileState[][] tileStates;
    private final Timer[][] growthTimers;
    private final Seed seed;
    private final IntConsumer harvestReward;

    public FarmPlot(int rows, int cols) {
        this(rows, cols, new Seed("Carrot", 10, 3, 20), reward -> {
        });
    }

    public FarmPlot(int rows, int cols, Seed seed, IntConsumer harvestReward) {
        this.rows = rows;
        this.cols = cols;
        this.seed = seed;
        this.harvestReward = harvestReward;
        this.tiles = new JButton[rows][cols];
        this.tileStates = new TileState[rows][cols];
        this.growthTimers = new Timer[rows][cols];

        setLayout(new GridLayout(rows, cols, 5, 5));

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton tile = new JButton("Empty");
                tile.setBackground(SOIL_COLOR);
                tile.setForeground(Color.WHITE);
                tile.setFocusPainted(false);

                final int r = i;
                final int c = j;
                tileStates[i][j] = TileState.EMPTY;
                tile.addActionListener(e -> onTileClicked(r, c));

                tiles[i][j] = tile;
                add(tile);
            }
        }
    }

    private void onTileClicked(int row, int col) {
        switch (tileStates[row][col]) {
            case EMPTY:
                plant(row, col);
                break;
            case READY:
                harvest(row, col);
                break;
            case GROWING:
                break;
        }
    }

    private void plant(int row, int col) {
        tileStates[row][col] = TileState.GROWING;
        JButton tile = tiles[row][col];
        tile.setText("Growing...");
        tile.setBackground(GROWING_COLOR);

        Timer timer = new Timer(seed.getGrowTime() * 1000, e -> {
            tileStates[row][col] = TileState.READY;
            tile.setText("Ready!");
            tile.setBackground(READY_COLOR);
            growthTimers[row][col].stop();
        });
        timer.setRepeats(false);
        growthTimers[row][col] = timer;
        timer.start();
    }

    private void harvest(int row, int col) {
        tileStates[row][col] = TileState.EMPTY;
        tiles[row][col].setText("Empty");
        tiles[row][col].setBackground(SOIL_COLOR);
        harvestReward.accept(seed.getSellPrice());
    }
}
