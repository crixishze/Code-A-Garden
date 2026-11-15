package codeagarden;

/** A logged-in player: the database id and the name shown on the leaderboard. */
public class Player {

    private final int id;
    private final String name;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
