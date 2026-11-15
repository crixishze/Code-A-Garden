package codeagarden;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DESIGN PATTERN: Singleton.
 * There is exactly ONE GameSession in the whole program. It holds everything that
 * changes during a game: balance, day, farm, inventory and story progress. Every
 * screen calls GameSession.getInstance() and gets the same object, so they can never
 * disagree (for example the shop and the bottom bar always show the same balance).
 * Play Again calls reset(); saving and loading read and write this one object.
 * It also remembers which Player is logged in (null = playing without saving).
 */
public final class GameSession {

    public static final int STARTING_BALANCE = 100;
    public static final int GOAL = 10_000;
    public static final int SECONDS_PER_DAY = 60;   // real seconds per in-game day
    public static final int TOTAL_DAYS = 30;

    // Created once, when the class is first used. This is what makes it a Singleton.
    private static final GameSession INSTANCE = new GameSession();

    private int balance;
    private int elapsedSeconds;
    private Farm farm;
    private PlayerInventory inventory;
    private StoryModel story;
    private Player player;   // kept by reset(): Play Again is still the same player

    // private: nobody else may create a GameSession
    private GameSession() {
        reset();
    }

    public static GameSession getInstance() {
        return INSTANCE;
    }

    /** Starts a brand-new game: starting money, day 1, small empty farm, empty inventory, story at chapter 1. */
    public void reset() {
        balance = STARTING_BALANCE;
        elapsedSeconds = 0;
        farm = new Farm();
        inventory = new PlayerInventory();
        story = new StoryModel();
    }

    // ---- money ----
    public int getBalance() {
        return balance;
    }

    public void addBalance(int amount) {
        balance += amount;
    }

    /** Returns false (and charges nothing) if the player can't afford it. */
    public boolean spend(int amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public boolean isGoalReached() {
        return balance >= GOAL;
    }

    // ---- days ----
    /** Call once per real second while the game is running. */
    public void tickSecond() {
        elapsedSeconds++;
    }

    public int getCurrentDay() {
        return Math.min(TOTAL_DAYS, elapsedSeconds / SECONDS_PER_DAY + 1);
    }

    /** True once the full duration of the last day has passed. */
    public boolean isSeasonOver() {
        return elapsedSeconds >= SECONDS_PER_DAY * TOTAL_DAYS;
    }

    // ---- the rest of the game state ----
    public Farm getFarm() {
        return farm;
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public StoryModel getStory() {
        return story;
    }

    // ---- the player ----
    public Player getPlayer() {
        return player;
    }

    /** null means "no database": the game can be played but not saved. */
    public void setPlayer(Player newPlayer) {
        player = newPlayer;
    }

    // ---- saving and loading ----
    public SaveData createSaveData() {
        return createSaveData(System.currentTimeMillis());
    }

    /** Copies everything that is saved into a SaveData, including every planted tile. */
    public SaveData createSaveData(long nowMillis) {
        List<SavedTile> tiles = new ArrayList<>();
        for (int row = 0; row < farm.getSize(); row++) {
            for (int col = 0; col < farm.getSize(); col++) {
                Plot plot = farm.getPlot(row, col);
                if (!plot.isEmpty()) {
                    tiles.add(new SavedTile(row, col, plot.getSeed().getName(), plot.getGrownMillis(nowMillis)));
                }
            }
        }
        return new SaveData(balance, elapsedSeconds, farm.getSize(), story.getChaptersDone(),
                inventory.getCounts(), tiles);
    }

    public void restore(SaveData data) {
        restore(data, System.currentTimeMillis());
    }

    /** Replaces the whole game with a saved one. Out-of-range numbers are fixed and unknown seeds ignored. */
    public void restore(SaveData data, long nowMillis) {
        reset();
        balance = data.getBalance();
        elapsedSeconds = Math.max(0, Math.min(SECONDS_PER_DAY * TOTAL_DAYS, data.getElapsedSeconds()));
        int size = Math.max(Farm.START_SIZE, Math.min(Farm.MAX_SIZE, data.getFarmSize()));
        while (farm.getSize() < size) {
            farm.expand();
        }
        for (Map.Entry<String, Integer> seed : data.getSeedCounts().entrySet()) {
            if (seed.getValue() > 0 && SeedFactory.isKnownSeed(seed.getKey())) {
                inventory.addSeed(SeedFactory.createSeed(seed.getKey()), seed.getValue());
            }
        }
        for (SavedTile tile : data.getTiles()) {
            restoreTile(tile, size, nowMillis);
        }
        story.restoreChaptersDone(data.getChaptersDone());
    }

    private void restoreTile(SavedTile tile, int size, long nowMillis) {
        boolean inside = tile.getRow() >= 0 && tile.getRow() < size && tile.getCol() >= 0 && tile.getCol() < size;
        if (!inside || !SeedFactory.isKnownSeed(tile.getSeedName())) {
            return;   // bad data in the save: skip this tile rather than crash
        }
        Plot plot = farm.getPlot(tile.getRow(), tile.getCol());
        if (plot.isEmpty()) {
            plot.restore(SeedFactory.createSeed(tile.getSeedName()), tile.getGrownMillis(), nowMillis);
        }
    }

    /** Applies the money and seed reward of a story reply. */
    public void applyConsequence(StoryModel.StoryConsequence consequence) {
        if (consequence.balanceDelta() != 0) {
            addBalance(consequence.balanceDelta());
        }
        if (consequence.seed() != null) {
            inventory.addSeed(consequence.seed(), 1);
        }
    }
}
