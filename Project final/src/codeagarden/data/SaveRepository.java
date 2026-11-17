package codeagarden.data;

import codeagarden.game.Farm;
import codeagarden.game.GameSession;
import codeagarden.game.SeedFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The only class that talks to the MySQL database, through JDBC (java.sql). ALL the SQL in the
 * game is in this file; the screens only call methods like logIn(), save(), load(), deleteSave(),
 * getLeaderboard() and runReport().
 *
 * Tables (see sql/schema.sql): PLAYER, FARM, CROP, PLANTED_CROP, INVENTORY, INVENTORY_CROP, SAVE.
 * Every player has exactly one FARM, one INVENTORY and at most one SAVE.
 *
 * Settings (host, port, database, user, password) are read from db.properties, never typed in code.
 * Every problem becomes a SaveException with a message the player can understand, so a missing
 * or unreachable database never crashes the game. Every method that writes several rows uses a
 * transaction: either all of its changes are stored, or none are (the old save stays as it was).
 */
public class SaveRepository {

    private static final String CONFIG_FILE = "db.properties";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int SOCKET_TIMEOUT_MILLIS = 5000;
    private static final int LEADERBOARD_SIZE = 50;

    // The farm is always 4 rows x 6 columns. FARM.farm_size is kept in the database
    // (so the tables did not change) and always stores the number of rows.
    private static final int FARM_SIZE_IN_DATABASE = Farm.ROWS;

    // MySQL error numbers we explain in plain words
    private static final int ERROR_DUPLICATE_ENTRY = 1062;
    private static final int ERROR_ACCESS_DENIED = 1045;
    private static final int ERROR_DATABASE_ACCESS_DENIED = 1044;
    private static final int ERROR_UNKNOWN_DATABASE = 1049;
    private static final int ERROR_TABLE_MISSING = 1146;
    private static final int ERROR_UNKNOWN_COLUMN = 1054;

    // =========================== ALL THE SQL ===========================
    // ---- players ----
    private static final String FIND_PLAYER =
            "SELECT player_id, player_name, password_salt, password_hash FROM PLAYER WHERE player_name = ?";
    private static final String INSERT_PLAYER =
            "INSERT INTO PLAYER (player_name, password_salt, password_hash, currency) VALUES (?, ?, ?, ?)";
    private static final String INSERT_FARM = "INSERT INTO FARM (player_id, farm_size) VALUES (?, ?)";
    private static final String INSERT_INVENTORY = "INSERT INTO INVENTORY (player_id) VALUES (?)";

    // ---- saving (the old tiles and seeds are deleted, then the current ones inserted) ----
    private static final String UPDATE_CURRENCY = "UPDATE PLAYER SET currency = ? WHERE player_id = ?";
    private static final String DELETE_TILES =
            "DELETE pc FROM PLANTED_CROP pc JOIN FARM f ON pc.farm_id = f.farm_id WHERE f.player_id = ?";
    private static final String INSERT_TILE =
            "INSERT INTO PLANTED_CROP (farm_id, crop_id, row_num, col_num, grown_ms, state) "
                    + "SELECT f.farm_id, c.crop_id, ?, ?, ?, ? FROM FARM f, CROP c "
                    + "WHERE f.player_id = ? AND c.crop_name = ?";
    private static final String DELETE_SEEDS =
            "DELETE ic FROM INVENTORY_CROP ic JOIN INVENTORY i ON ic.inventory_id = i.inventory_id "
                    + "WHERE i.player_id = ?";
    private static final String INSERT_SEED =
            "INSERT INTO INVENTORY_CROP (inventory_id, crop_id, quantity) "
                    + "SELECT i.inventory_id, c.crop_id, ? FROM INVENTORY i, CROP c "
                    + "WHERE i.player_id = ? AND c.crop_name = ?";
    private static final String UPDATE_SAVE =
            "UPDATE `SAVE` SET saved_at = NOW(), elapsed_seconds = ?, chapters_done = ?, finished = ? "
                    + "WHERE player_id = ?";
    private static final String INSERT_SAVE =
            "INSERT INTO `SAVE` (player_id, inventory_id, farm_id, saved_at, elapsed_seconds, chapters_done, finished) "
                    + "SELECT p.player_id, i.inventory_id, f.farm_id, NOW(), ?, ?, ? FROM PLAYER p "
                    + "JOIN INVENTORY i ON i.player_id = p.player_id JOIN FARM f ON f.player_id = p.player_id "
                    + "WHERE p.player_id = ?";

    // ---- loading ----
    private static final String SELECT_SAVE =
            "SELECT s.elapsed_seconds, s.chapters_done, s.finished, p.currency FROM `SAVE` s "
                    + "JOIN PLAYER p ON p.player_id = s.player_id WHERE s.player_id = ?";
    private static final String SELECT_SEEDS =
            "SELECT c.crop_name, ic.quantity FROM INVENTORY_CROP ic "
                    + "JOIN INVENTORY i ON i.inventory_id = ic.inventory_id JOIN CROP c ON c.crop_id = ic.crop_id "
                    + "WHERE i.player_id = ? ORDER BY c.cost";
    private static final String SELECT_TILES =
            "SELECT c.crop_name, pc.row_num, pc.col_num, pc.grown_ms FROM PLANTED_CROP pc "
                    + "JOIN FARM f ON f.farm_id = pc.farm_id JOIN CROP c ON c.crop_id = pc.crop_id "
                    + "WHERE f.player_id = ?";

    // ---- deleting a player (ON DELETE CASCADE in sql/schema.sql removes everything that belongs to them) ----
    private static final String DELETE_PLAYER = "DELETE FROM PLAYER WHERE player_id = ?";

    // ---- leaderboard: richest first ----
    private static final String SELECT_LEADERBOARD =
            "SELECT player_name, currency FROM PLAYER ORDER BY currency DESC, player_name LIMIT " + LEADERBOARD_SIZE;

    // ---- the REPORTS screen: 10 business questions ----
    private static final List<Report> REPORTS = Arrays.asList(
            new Report("Leaderboard (richest first)", "ORDER BY", null,
                    "SELECT player_name AS Player, currency AS Money FROM PLAYER "
                            + "ORDER BY currency DESC, player_name"),
            new Report("Search players by name", "WHERE + LIKE, typed value", "Show players whose name starts with:",
                    "SELECT player_name AS Player, currency AS Money FROM PLAYER "
                            + "WHERE player_name LIKE CONCAT(?, '%') ORDER BY player_name"),
            new Report("Crops ready to harvest", "JOIN of 4 tables, WHERE", null,
                    "SELECT p.player_name AS Player, c.crop_name AS Crop, pc.row_num AS `Row`, pc.col_num AS `Column` "
                            + "FROM PLANTED_CROP pc JOIN FARM f ON pc.farm_id = f.farm_id "
                            + "JOIN PLAYER p ON f.player_id = p.player_id JOIN CROP c ON pc.crop_id = c.crop_id "
                            + "WHERE pc.state = 'Matured' ORDER BY p.player_name"),
            new Report("Planted plots per player", "LEFT JOIN, COUNT, GROUP BY", null,
                    "SELECT p.player_name AS Player, COUNT(pc.crop_id) AS `Planted plots` FROM PLAYER p "
                            + "JOIN FARM f ON f.player_id = p.player_id "
                            + "LEFT JOIN PLANTED_CROP pc ON pc.farm_id = f.farm_id "
                            + "GROUP BY p.player_id, p.player_name ORDER BY `Planted plots` DESC, p.player_name"),
            new Report("Players with 3 or more planted plots", "JOIN, GROUP BY + HAVING", null,
                    "SELECT p.player_name AS Player, COUNT(*) AS `Planted plots` FROM PLAYER p "
                            + "JOIN FARM f ON f.player_id = p.player_id JOIN PLANTED_CROP pc ON pc.farm_id = f.farm_id "
                            + "GROUP BY p.player_id, p.player_name HAVING COUNT(*) >= 3 ORDER BY p.player_name"),
            new Report("Total seeds owned per player", "JOIN, SUM, GROUP BY", null,
                    "SELECT p.player_name AS Player, SUM(ic.quantity) AS Seeds FROM PLAYER p "
                            + "JOIN INVENTORY i ON i.player_id = p.player_id "
                            + "JOIN INVENTORY_CROP ic ON ic.inventory_id = i.inventory_id "
                            + "GROUP BY p.player_id, p.player_name ORDER BY Seeds DESC, p.player_name"),
            new Report("Players richer than the average", "Subquery, AVG", null,
                    "SELECT player_name AS Player, currency AS Money FROM PLAYER "
                            + "WHERE currency > (SELECT AVG(currency) FROM PLAYER) ORDER BY currency DESC"),
            new Report("Most profitable crop per second", "Calculated column, ORDER BY", null,
                    "SELECT crop_name AS Crop, reward - cost AS Profit, "
                            + "ROUND((reward - cost) / growth_time, 2) AS `Profit per second` "
                            + "FROM CROP ORDER BY `Profit per second` DESC"),
            new Report("Games in progress vs finished", "JOIN, COUNT, AVG, GROUP BY", null,
                    "SELECT CASE WHEN s.finished = 1 THEN 'Finished' ELSE 'In progress' END AS Status, "
                            + "COUNT(*) AS Players, ROUND(AVG(p.currency), 1) AS `Average money` "
                            + "FROM `SAVE` s JOIN PLAYER p ON p.player_id = s.player_id "
                            + "GROUP BY s.finished ORDER BY s.finished"),
            new Report("Players with nothing ready to harvest", "Subquery, NOT IN", null,
                    "SELECT player_name AS Player FROM PLAYER WHERE player_id NOT IN ("
                            + "SELECT f.player_id FROM FARM f JOIN PLANTED_CROP pc ON pc.farm_id = f.farm_id "
                            + "WHERE pc.state = 'Matured') ORDER BY player_name")
    );
    // ====================================================================

    // ---------------------------- players ----------------------------

    /** True if someone already uses this name. */
    public boolean playerExists(String name) throws SaveException {
        try (Connection connection = connect();
             PreparedStatement statement = prepare(connection, FIND_PLAYER, name);
             ResultSet row = statement.executeQuery()) {
            return row.next();
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    /** Makes a new player with an empty farm and inventory. Fails if the name is taken. */
    public Player createPlayer(String name, char[] password) throws SaveException {
        String salt = Passwords.newSalt();
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                int id = insertPlayer(connection, name, salt, Passwords.hash(salt, password));
                update(connection, INSERT_FARM, id, FARM_SIZE_IN_DATABASE);
                update(connection, INSERT_INVENTORY, id);
                connection.commit();
                return new Player(id, name);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ERROR_DUPLICATE_ENTRY) {
                throw new SaveException("The name \"" + name + "\" is already taken. Pick another name.");
            }
            throw explain(e);
        }
    }

    /** Checks the name and password. Returns the player, or fails with "wrong name or password". */
    public Player logIn(String name, char[] password) throws SaveException {
        try (Connection connection = connect();
             PreparedStatement statement = prepare(connection, FIND_PLAYER, name);
             ResultSet row = statement.executeQuery()) {
            if (row.next() && Passwords.matches(row.getString("password_salt"),
                    row.getString("password_hash"), password)) {
                return new Player(row.getInt("player_id"), row.getString("player_name"));
            }
            // same message whether the name or the password is wrong, so names can't be guessed this way
            throw new SaveException("Wrong player name or password.");
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // ----------------------------- saving -----------------------------

    /** Saves a game in progress (CONTINUE can load it). */
    public void save(Player player, SaveData data) throws SaveException {
        write(player, data, false);
    }

    /** Saves a game that was won or lost: its currency counts on the leaderboard, but it can't be continued. */
    public void saveFinishedGame(Player player, SaveData data) throws SaveException {
        write(player, data, true);
    }

    /** True if this player has a saved game that is not finished. */
    public boolean hasGameInProgress(Player player) throws SaveException {
        return load(player) != null;
    }

    /** Reads this player's save. Returns null if there is nothing to continue. */
    public SaveData load(Player player) throws SaveException {
        try (Connection connection = connect();
             PreparedStatement statement = prepare(connection, SELECT_SAVE, player.getId());
             ResultSet row = statement.executeQuery()) {
            if (!row.next() || row.getInt("finished") != 0) {
                return null;
            }
            return new SaveData(row.getInt("currency"), row.getInt("elapsed_seconds"),
                    row.getInt("chapters_done"), readSeeds(connection, player), readTiles(connection, player));
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    /**
     * Deletes the player completely: save, farm, planted crops, inventory, seeds, and their
     * leaderboard entry. One DELETE is enough because the foreign keys use ON DELETE CASCADE.
     */
    public void deleteSave(Player player) throws SaveException {
        try (Connection connection = connect()) {
            // One DELETE is enough: the database cascades to the farm, planted crops,
            // inventory, seeds and save, and a single statement is already all-or-nothing.
            update(connection, DELETE_PLAYER, player.getId());
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // --------------------------- leaderboard ---------------------------

    /** Every player's currency, highest first. */
    public List<LeaderboardEntry> getLeaderboard() throws SaveException {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(SELECT_LEADERBOARD);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                entries.add(new LeaderboardEntry(rows.getString("player_name"), rows.getInt("currency")));
            }
            return entries;
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // ----------------------------- reports -----------------------------

    /** The 10 reports shown on the REPORTS screen, in menu order. */
    public List<Report> getReports() {
        return REPORTS;
    }

    /**
     * Runs one report and returns its columns and rows. input is the typed value for a
     * report that needs one (it goes into the ? of a PreparedStatement); otherwise it is ignored.
     */
    public ReportResult runReport(Report report, String input) throws SaveException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(report.sql)) {
            if (report.needsInput()) {
                statement.setString(1, input == null ? "" : input.trim());
            }
            try (ResultSet rows = statement.executeQuery()) {
                return readResult(rows);
            }
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // Copies any query result into a ReportResult: column names first, then every row.
    private ReportResult readResult(ResultSet rows) throws SQLException {
        ResultSetMetaData meta = rows.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.add(meta.getColumnLabel(i));   // the "AS" name, e.g. "Player"
        }
        List<List<Object>> table = new ArrayList<>();
        while (rows.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columns.size(); i++) {
                row.add(rows.getObject(i));
            }
            table.add(row);
        }
        return new ReportResult(columns, table);
    }

    // ------------------------- the steps of saving -------------------------

    // One transaction: money, planted crops, seeds and the SAVE row are stored together or not at all.
    private void write(Player player, SaveData data, boolean finished) throws SaveException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);   // one transaction: everything is saved or nothing
            try {
                int id = player.getId();
                update(connection, UPDATE_CURRENCY, data.getBalance(), id);
                writeTiles(connection, id, data);
                writeSeeds(connection, id, data);
                writeSaveRow(connection, id, data, finished);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // Replaces the player's PLANTED_CROP rows with the plots planted right now.
    private void writeTiles(Connection connection, int playerId, SaveData data) throws SQLException {
        update(connection, DELETE_TILES, playerId);
        try (PreparedStatement statement = connection.prepareStatement(INSERT_TILE)) {
            for (SavedTile tile : data.getTiles()) {
                statement.setInt(1, tile.getRow() + 1);   // the database counts rows from 1
                statement.setInt(2, tile.getCol() + 1);
                statement.setLong(3, tile.getGrownMillis());
                statement.setString(4, stateName(tile));
                statement.setInt(5, playerId);
                statement.setString(6, tile.getSeedName());
                statement.executeUpdate();
            }
        }
    }

    // Replaces the player's INVENTORY_CROP rows with the seeds owned right now.
    private void writeSeeds(Connection connection, int playerId, SaveData data) throws SQLException {
        update(connection, DELETE_SEEDS, playerId);
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SEED)) {
            for (Map.Entry<String, Integer> seed : data.getSeedCounts().entrySet()) {
                statement.setInt(1, seed.getValue());
                statement.setInt(2, playerId);
                statement.setString(3, seed.getKey());
                statement.executeUpdate();
            }
        }
    }

    // Updates the player's SAVE row, or inserts it the first time this player saves.
    private void writeSaveRow(Connection connection, int playerId, SaveData data, boolean finished)
            throws SQLException {
        int finishedFlag = finished ? 1 : 0;
        int changed = update(connection, UPDATE_SAVE, data.getElapsedSeconds(), data.getChaptersDone(),
                finishedFlag, playerId);
        if (changed == 0) {   // first save for this player
            update(connection, INSERT_SAVE, data.getElapsedSeconds(), data.getChaptersDone(), finishedFlag, playerId);
        }
    }

    // The words used in PLANTED_CROP.state: Seed (first third of growing), Growing, Matured (ready to harvest).
    private static String stateName(SavedTile tile) {
        long growMillis = SeedFactory.createSeed(tile.getSeedName()).getGrowTime() * 1000L;
        if (tile.getGrownMillis() >= growMillis) {
            return "Matured";
        }
        return tile.getGrownMillis() * 3 < growMillis ? "Seed" : "Growing";
    }

    // ------------------------- the steps of loading -------------------------

    // Reads the player's seed counts (INVENTORY_CROP joined to CROP for the names).
    private Map<String, Integer> readSeeds(Connection connection, Player player) throws SQLException {
        Map<String, Integer> seeds = new LinkedHashMap<>();
        try (PreparedStatement statement = prepare(connection, SELECT_SEEDS, player.getId());
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                seeds.put(rows.getString("crop_name"), rows.getInt("quantity"));
            }
        }
        return seeds;
    }

    // Reads the player's planted plots (PLANTED_CROP joined to FARM and CROP).
    private List<SavedTile> readTiles(Connection connection, Player player) throws SQLException {
        List<SavedTile> tiles = new ArrayList<>();
        try (PreparedStatement statement = prepare(connection, SELECT_TILES, player.getId());
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                tiles.add(new SavedTile(rows.getInt("row_num") - 1, rows.getInt("col_num") - 1,
                        rows.getString("crop_name"), rows.getLong("grown_ms")));
            }
        }
        return tiles;
    }

    // ------------------------------ small helpers ------------------------------

    // Inserts the PLAYER row and returns the new player_id that MySQL picked (AUTO_INCREMENT).
    private int insertPlayer(Connection connection, String name, String salt, String hash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PLAYER, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, salt);
            statement.setString(3, hash);
            statement.setInt(4, GameSession.STARTING_BALANCE);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);   // the new player_id MySQL picked
            }
        }
    }

    // Runs an INSERT/UPDATE/DELETE whose ? are all numbers. Returns how many rows it changed.
    private int update(Connection connection, String sql, int... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setInt(i + 1, values[i]);
            }
            return statement.executeUpdate();
        }
    }

    // A PreparedStatement whose one ? is a text value.
    private PreparedStatement prepare(Connection connection, String sql, String value) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, value);   // a ? value is never pasted into the SQL text, so names can't inject SQL
        return statement;
    }

    // A PreparedStatement whose one ? is a number.
    private PreparedStatement prepare(Connection connection, String sql, int value) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, value);
        return statement;
    }

    // ------------------------------- connecting -------------------------------

    // Opens a JDBC connection to MySQL using the settings in db.properties.
    private Connection connect() throws SaveException {
        Properties settings = readSettings();
        String url = "jdbc:mysql://" + settings.getProperty("host", "localhost")
                + ":" + settings.getProperty("port", "3306")
                + "/" + settings.getProperty("database", "code_a_garden")
                // useSSL=false / allowPublicKeyRetrieval=true let a plain local MySQL 8 accept the login
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&connectTimeout=" + CONNECT_TIMEOUT_MILLIS
                + "&socketTimeout=" + SOCKET_TIMEOUT_MILLIS;
        try {
            return DriverManager.getConnection(url,
                    settings.getProperty("user", "root"), settings.getProperty("password", ""));
        } catch (SQLException e) {
            throw explain(e);
        }
    }

    // Reads db.properties from the folder the game was started in.
    private Properties readSettings() throws SaveException {
        Properties settings = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            settings.load(in);
        } catch (IOException e) {
            throw new SaveException("No database settings found. Copy db.properties.example to "
                    + "db.properties (in the project folder) and fill it in.", true, e);
        }
        return settings;
    }

    // Turns a technical database error into a sentence the player can act on.
    private SaveException explain(SQLException e) {
        String technical = String.valueOf(e.getMessage());
        if (technical.contains("No suitable driver")) {
            return new SaveException("The MySQL driver is missing. Put the mysql-connector-j .jar file "
                    + "in the lib folder.", true, e);
        }
        switch (e.getErrorCode()) {
            case ERROR_ACCESS_DENIED:
                return new SaveException("The database refused the user name or password in db.properties.", true, e);
            case ERROR_DATABASE_ACCESS_DENIED:
                return new SaveException("The user in db.properties may not use that database (or it does not "
                        + "exist yet). Run sql/schema.sql and check the database name.", true, e);
            case ERROR_UNKNOWN_DATABASE:
            case ERROR_TABLE_MISSING:
            case ERROR_UNKNOWN_COLUMN:
                return new SaveException("The game's database tables were not found or are out of date. "
                        + "Run sql/schema.sql first.", true, e);
            default:
                break;
        }
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {
            return new SaveException("Could not reach the database. Check that MySQL is running and that "
                    + "db.properties is correct.", true, e);
        }
        return new SaveException("The database reported a problem: " + technical, true, e);
    }
}
