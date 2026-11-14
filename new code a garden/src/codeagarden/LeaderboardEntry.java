package codeagarden;

/** One line of the leaderboard. */
public class LeaderboardEntry {

    private final String playerName;
    private final int currency;

    public LeaderboardEntry(String playerName, int currency) {
        this.playerName = playerName;
        this.currency = currency;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getCurrency() {
        return currency;
    }
}
