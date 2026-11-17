package codeagarden.data;

/** One planted square in a save: where it is, which crop, and how long it has grown. */
public class SavedTile {

    private final int row;
    private final int col;
    private final String seedName;
    private final long grownMillis;

    /** Remembers one planted plot. row and col are counted from 0. */
    public SavedTile(int row, int col, String seedName, long grownMillis) {
        this.row = row;
        this.col = col;
        this.seedName = seedName;
        this.grownMillis = grownMillis;
    }

    /** 0-based, like the Farm grid (the database stores row + 1). */
    public int getRow() {
        return row;
    }

    /** 0-based column. */
    public int getCol() {
        return col;
    }

    /** Which crop is planted, e.g. "Carrot". */
    public String getSeedName() {
        return seedName;
    }

    /** How many milliseconds the crop had grown. */
    public long getGrownMillis() {
        return grownMillis;
    }
}
