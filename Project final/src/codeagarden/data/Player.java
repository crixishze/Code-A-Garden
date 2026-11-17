package codeagarden.data;

/** A logged-in player: the database id and the name shown on the leaderboard. */
public class Player {

    private final int id;
    private final String name;

    /** A player as read from the PLAYER table. */
    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /** PLAYER.player_id. */
    public int getId() {
        return id;
    }

    /** PLAYER.player_name. */
    public String getName() {
        return name;
    }
}
