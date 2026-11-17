package codeagarden.game;

import codeagarden.data.Player;
import codeagarden.data.SaveData;
import codeagarden.data.SavedTile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DESIGN PATTERN: Singleton.
 * There is exactly ONE GameSession in the whole program. It holds everything that
 * changes during a game: balance, time played, farm, inventory and story progress. Every
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
    public static final int SEASON_SECONDS = SECONDS_PER_DAY * TOTAL_DAYS;   // 1800

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

    /** The one and only GameSession. */
    public static GameSession getInstance() {
        return INSTANCE;
    }

    /** Starts a brand-new game: starting money, day 1, empty farm, empty inventory, story at chapter 1. */
    public void reset() {
        balance = STARTING_BALANCE;
        elapsedSeconds = 0;
        farm = new Farm();
        inventory = new PlayerInventory();
        story = new StoryModel();
    }

    // ---- money ----

    /** The player's money right now. */
    public int getBalance() {
        return balance;
    }

    /** Adds money (a harvest or a story reward). A negative amount takes money away. */
    public void addBalance(int amount) {
        balance += amount;
    }

    /** Pays for something. Returns false (and charges nothing) if the player can't afford it. */
    public boolean spend(int amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    /** True once the player has $10,000 or more: the game is won. */
    public boolean isGoalReached() {
        return balance >= GOAL;
    }

    // ---- time ----

    /** Call once per real second while the game is running. */
    public void tickSecond() {
        elapsedSeconds++;
    }

    /** Seconds played this season (0 to 1800). */
    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    /** The in-game day, 1 to 30 (60 real seconds = 1 day). */
    public int getCurrentDay() {
        return Math.min(TOTAL_DAYS, elapsedSeconds / SECONDS_PER_DAY + 1);
    }

    /** True once all 30 days have fully passed: the game is lost if the goal was not reached. */
    public boolean isSeasonOver() {
        return elapsedSeconds >= SEASON_SECONDS;
    }

    // ---- the rest of the game state ----

    /** The 4 x 6 farm of this game. */
    public Farm getFarm() {
        return farm;
    }

    /** The seeds the player owns. */
    public PlayerInventory getInventory() {
        return inventory;
    }

    /** The story chapters and how far the player has got. */
    public StoryModel getStory() {
        return story;
    }

    // ---- the player ----

    /** The logged-in player, or null when playing without a database. */
    public Player getPlayer() {
        return player;
    }

    /** null means "no database": the game can be played but not saved. */
    public void setPlayer(Player newPlayer) {
        player = newPlayer;
    }

    // ---- saving and loading ----

    /** Copies everything that is saved into a SaveData, using the current time. */
    public SaveData createSaveData() {
        return createSaveData(System.currentTimeMillis());
    }

    /** Copies everything that is saved into a SaveData, including every planted plot. */
    public SaveData createSaveData(long nowMillis) {
        List<SavedTile> tiles = new ArrayList<>();
        for (int row = 0; row < Farm.ROWS; row++) {
            for (int col = 0; col < Farm.COLS; col++) {
                Plot plot = farm.getPlot(row, col);
                if (!plot.isEmpty()) {
                    tiles.add(new SavedTile(row, col, plot.getSeed().getName(), plot.getGrownMillis(nowMillis)));
                }
            }
        }
        return new SaveData(balance, elapsedSeconds, story.getChaptersDone(), inventory.getCounts(), tiles);
    }

    /** Replaces the whole game with a saved one, using the current time. */
    public void restore(SaveData data) {
        restore(data, System.currentTimeMillis());
    }

    /** Replaces the whole game with a saved one. Out-of-range numbers are fixed and unknown seeds ignored. */
    public void restore(SaveData data, long nowMillis) {
        reset();
        balance = data.getBalance();
        elapsedSeconds = Math.max(0, Math.min(SEASON_SECONDS, data.getElapsedSeconds()));
        for (Map.Entry<String, Integer> seed : data.getSeedCounts().entrySet()) {
            if (seed.getValue() > 0 && SeedFactory.isKnownSeed(seed.getKey())) {
                inventory.addSeed(SeedFactory.createSeed(seed.getKey()), seed.getValue());
            }
        }
        for (SavedTile tile : data.getTiles()) {
            restoreTile(tile, nowMillis);
        }
        story.restoreChaptersDone(data.getChaptersDone());
    }

    // Puts one saved crop back on the farm. Bad data (outside the farm, unknown seed) is skipped.
    private void restoreTile(SavedTile tile, long nowMillis) {
        if (!Farm.isInside(tile.getRow(), tile.getCol()) || !SeedFactory.isKnownSeed(tile.getSeedName())) {
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
