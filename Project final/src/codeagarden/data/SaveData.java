package codeagarden.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything that goes into a save: currency, time played, story chapters finished,
 * seeds owned and every planted tile. A plain data holder; it knows nothing about the database.
 */
public class SaveData {

    private final int balance;
    private final int elapsedSeconds;
    private final int chaptersDone;
    private final Map<String, Integer> seedCounts;
    private final List<SavedTile> tiles;

    /** Copies the values; the seed map and tile list can't be changed afterwards. */
    public SaveData(int balance, int elapsedSeconds, int chaptersDone,
                    Map<String, Integer> seedCounts, List<SavedTile> tiles) {
        this.balance = balance;
        this.elapsedSeconds = elapsedSeconds;
        this.chaptersDone = chaptersDone;
        this.seedCounts = Collections.unmodifiableMap(new LinkedHashMap<>(seedCounts));
        this.tiles = Collections.unmodifiableList(new ArrayList<>(tiles));
    }

    /** The player's money when the game was saved. */
    public int getBalance() {
        return balance;
    }

    /** Seconds played this season; the time remaining is worked out from this. */
    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    /** How many story chapters were finished (0 to 3). */
    public int getChaptersDone() {
        return chaptersDone;
    }

    /** Seed name to amount owned. */
    public Map<String, Integer> getSeedCounts() {
        return seedCounts;
    }

    /** Every planted plot. */
    public List<SavedTile> getTiles() {
        return tiles;
    }
}
