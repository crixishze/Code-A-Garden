package codeagarden.game;

import java.util.ArrayList;
import java.util.List;

/**
 * The farm: a fixed grid of 4 rows and 6 columns of plots, and where the drone is.
 * This is pure game logic, with no screen code. FarmPanel draws it; the player's code
 * changes it through the drone commands.
 */
public class Farm {

    public static final int ROWS = 4;
    public static final int COLS = 6;

    private final Plot[][] plots = new Plot[ROWS][COLS];
    private int droneRow = 0;   // the drone starts in the top-left plot
    private int droneCol = 0;

    /** Creates the farm with every plot empty. */
    public Farm() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                plots[row][col] = new Plot();
            }
        }
    }

    /** Returns the plot at this row and column (both counted from 0). */
    public Plot getPlot(int row, int col) {
        return plots[row][col];
    }

    /** True if this row and column are inside the 4 x 6 grid. */
    public static boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    /**
     * Advances growth on every plot. FarmPanel calls this several times a second.
     * Returns the plots whose crop became ready during this call (usually none).
     */
    public List<Plot> tick(long nowMillis) {
        List<Plot> becameReady = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (plots[row][col].update(nowMillis)) {
                    becameReady.add(plots[row][col]);
                }
            }
        }
        return becameReady;
    }

    /** Row of the plot the drone is above (0 = top row). */
    public int getDroneRow() {
        return droneRow;
    }

    /** Column of the plot the drone is above (0 = left column). */
    public int getDroneCol() {
        return droneCol;
    }

    /** The plot the drone is above. plant() and harvest() work on this plot. */
    public Plot getDronePlot() {
        return plots[droneRow][droneCol];
    }

    /**
     * Moves the drone one plot. North = up, South = down, East = right, West = left
     * (upper or lower case both work). Returns false, and does not move, if the
     * direction is unknown or the move would leave the farm.
     */
    public boolean moveDrone(String direction) {
        int row = droneRow;
        int col = droneCol;
        if ("North".equalsIgnoreCase(direction)) {
            row--;
        } else if ("South".equalsIgnoreCase(direction)) {
            row++;
        } else if ("East".equalsIgnoreCase(direction)) {
            col++;
        } else if ("West".equalsIgnoreCase(direction)) {
            col--;
        } else {
            return false;   // not a direction
        }
        if (!isInside(row, col)) {
            return false;   // the edge of the farm
        }
        droneRow = row;
        droneCol = col;
        return true;
    }
}
