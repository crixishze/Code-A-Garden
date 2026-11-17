package codeagarden.data;

/** One line of the leaderboard. */
public class LeaderboardEntry {

    private final String playerName;
    private final int currency;

    /** One row of the leaderboard query. */
    public LeaderboardEntry(String playerName, int currency) {
        this.playerName = playerName;
        this.currency = currency;
    }

    /** The player's name. */
    public String getPlayerName() {
        return playerName;
    }

    /** The player's money. */
    public int getCurrency() {
        return currency;
    }
}
