package codeagarden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything that goes into a save: currency, time played, farm size, story chapters finished,
 * seeds owned and every planted tile. A plain data holder; it knows nothing about the database.
 */
public class SaveData {

    private final int balance;
    private final int elapsedSeconds;
    private final int farmSize;
    private final int chaptersDone;
    private final Map<String, Integer> seedCounts;
    private final List<SavedTile> tiles;

    public SaveData(int balance, int elapsedSeconds, int farmSize, int chaptersDone,
                    Map<String, Integer> seedCounts, List<SavedTile> tiles) {
        this.balance = balance;
        this.elapsedSeconds = elapsedSeconds;
        this.farmSize = farmSize;
        this.chaptersDone = chaptersDone;
        this.seedCounts = Collections.unmodifiableMap(new LinkedHashMap<>(seedCounts));
        this.tiles = Collections.unmodifiableList(new ArrayList<>(tiles));
    }

    public int getBalance() {
        return balance;
    }

    /** Seconds played this season; the time remaining is worked out from this. */
    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getFarmSize() {
        return farmSize;
    }

    public int getChaptersDone() {
        return chaptersDone;
    }

    public Map<String, Integer> getSeedCounts() {
        return seedCounts;
    }

    public List<SavedTile> getTiles() {
        return tiles;
    }
}
