package codeagarden;

import java.util.ArrayList;
import java.util.List;

/** The farm grid, its size upgrades and where the drone is. No screen code here. */
public class Farm {

    public static final int START_SIZE = 3;
    public static final int MAX_SIZE = 6;

    // price of each upgrade: 3x3 to 4x4, 4x4 to 5x5, 5x5 to 6x6
    private static final int[] UPGRADE_COSTS = {150, 400, 1000};

    // built for the biggest size; only the first size x size plots are in use
    private final Plot[][] plots = new Plot[MAX_SIZE][MAX_SIZE];
    private int size = START_SIZE;
    private int droneRow = 0;
    private int droneCol = 0;

    public Farm() {
        for (int row = 0; row < MAX_SIZE; row++) {
            for (int col = 0; col < MAX_SIZE; col++) {
                plots[row][col] = new Plot();
            }
        }
    }

    public int getSize() {
        return size;
    }

    public boolean canExpand() {
        return size < MAX_SIZE;
    }

    /** Price of the next upgrade. Only valid while canExpand() is true. */
    public int getUpgradeCost() {
        return UPGRADE_COSTS[size - START_SIZE];
    }

    /** Makes the farm one row and column bigger (the shop charges the money). */
    public void expand() {
        if (canExpand()) {
            size++;
        }
    }

    public Plot getPlot(int row, int col) {
        return plots[row][col];
    }

    /**
     * Advances growth on every plot; call about 10 times a second.
     * Returns the plots whose crop became ready during this call (usually none).
     */
    public List<Plot> tick(long nowMillis) {
        List<Plot> becameReady = new ArrayList<>();
        for (int row = 0; row < MAX_SIZE; row++) {
            for (int col = 0; col < MAX_SIZE; col++) {
                if (plots[row][col].update(nowMillis)) {
                    becameReady.add(plots[row][col]);
                }
            }
        }
        return becameReady;
    }

    public int getDroneRow() {
        return droneRow;
    }

    public int getDroneCol() {
        return droneCol;
    }

    public Plot getDronePlot() {
        return plots[droneRow][droneCol];
    }

    /** Moves the drone one plot. Directions: North, South, East, West. Stays inside the grid. */
    public void moveDrone(String direction) {
        int row = droneRow;
        int col = droneCol;
        switch (direction) {
            case "North":
                row--;
                break;
            case "South":
                row++;
                break;
            case "East":
                col++;
                break;
            case "West":
                col--;
                break;
            default:
                break;
        }
        if (row < 0 || row >= size || col < 0 || col >= size) {
            return;
        }
        droneRow = row;
        droneCol = col;
    }
}
