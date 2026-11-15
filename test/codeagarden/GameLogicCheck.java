package codeagarden;

/** A plain console check of the game rules (no window). Prints PASS or FAIL for each rule. */
public class GameLogicCheck {

    private static int failures = 0;

    public static void main(String[] args) {
        checkSeedFactory();
        checkSingletonAndMoney();
        checkInventory();
        checkPlotGrowth();
        checkFarm();
        checkDays();
        checkStory();
        checkSaveRoundTrip();
        checkPasswords();
        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void checkSeedFactory() {
        Seed pumpkin = SeedFactory.createSeed(SeedFactory.PUMPKIN);
        check("Pumpkin costs 50, grows 12s, sells 120",
                pumpkin.getPrice() == 50 && pumpkin.getGrowTime() == 12 && pumpkin.getSellPrice() == 120);
        check("Shop list has 4 seeds, cheapest first",
                SeedFactory.createAllSeeds().size() == 4
                        && SeedFactory.createAllSeeds().get(0).getName().equals("Carrot"));
        boolean threw = false;
        try {
            SeedFactory.createSeed("Banana");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("Unknown seed name is rejected", threw);
    }

    private static void checkSingletonAndMoney() {
        GameSession a = GameSession.getInstance();
        GameSession b = GameSession.getInstance();
        check("getInstance() always returns the same object", a == b);
        a.reset();
        check("New game starts with $100", a.getBalance() == 100);
        check("Cannot spend $150 with $100 (nothing charged)", !a.spend(150) && a.getBalance() == 100);
        check("Spending $10 leaves $90", a.spend(10) && a.getBalance() == 90);
        a.addBalance(10_000);
        check("Goal reached at $10,000 or more", a.isGoalReached());
        a.reset();
        check("reset() restores $100 and goal not reached", a.getBalance() == 100 && !a.isGoalReached());
    }

    private static void checkInventory() {
        PlayerInventory inv = new PlayerInventory();
        check("Inventory starts empty with nothing selected", inv.isEmpty() && inv.getSelectedName() == null);
        inv.addSeed(SeedFactory.createSeed("Carrot"), 2);
        check("First seed owned is selected automatically", "Carrot".equals(inv.getSelectedName()));
        inv.addSeed(SeedFactory.createSeed("Corn"), 1);
        inv.select("Corn");
        check("Can select another owned seed", "Corn".equals(inv.getSelectedName()));
        inv.select("Pumpkin");
        check("Cannot select a seed you don't own", "Corn".equals(inv.getSelectedName()));
        check("Taking the last Corn returns it", inv.takeSelectedSeed().getName().equals("Corn"));
        check("Selection moves to Carrot when Corn runs out", "Carrot".equals(inv.getSelectedName()));
        inv.takeSelectedSeed();
        inv.takeSelectedSeed();
        check("Empty again after using all seeds; takeSelectedSeed() gives null",
                inv.isEmpty() && inv.takeSelectedSeed() == null);
    }

    private static void checkPlotGrowth() {
        Plot plot = new Plot();
        plot.plant(SeedFactory.createSeed("Carrot"), 0L);   // grows in 3 seconds
        check("Just planted: GROWING, stage 1", plot.getState() == PlotState.GROWING && plot.getStage(0L) == 1);
        check("After 1.1s: stage 2", plot.getStage(1100L) == 2);
        check("After 2.1s: stage 3 but still GROWING", plot.getStage(2100L) == 3 && plot.getState() == PlotState.GROWING);
        check("update() before 3s does not make it ready", !plot.update(2900L));
        check("update() at 3s makes it READY (returns true once)", plot.update(3000L) && plot.isReady() && !plot.update(3100L));
        check("Harvest returns the carrot (sells for 20) and empties the plot",
                plot.harvest().getSellPrice() == 20 && plot.isEmpty());
        boolean threw = false;
        try {
            plot.harvest();
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("Cannot harvest an empty plot", threw);
    }

    private static void checkFarm() {
        Farm farm = new Farm();
        check("Farm starts 3x3 with drone at top-left", farm.getSize() == 3 && farm.getDroneRow() == 0 && farm.getDroneCol() == 0);
        farm.moveDrone("North");
        farm.moveDrone("West");
        check("Drone cannot leave the grid", farm.getDroneRow() == 0 && farm.getDroneCol() == 0);
        farm.moveDrone("East");
        farm.moveDrone("South");
        check("Drone moves East and South", farm.getDroneRow() == 1 && farm.getDroneCol() == 1);
        check("First upgrade costs $150", farm.getUpgradeCost() == 150);
        farm.expand();
        check("Second upgrade costs $400", farm.getSize() == 4 && farm.getUpgradeCost() == 400);
        farm.expand();
        check("Third upgrade costs $1000", farm.getSize() == 5 && farm.getUpgradeCost() == 1000);
        farm.expand();
        farm.expand();
        check("Farm stops at 6x6", farm.getSize() == 6 && !farm.canExpand());
        farm.getPlot(0, 0).plant(SeedFactory.createSeed("Carrot"), 0L);
        farm.tick(3000L);
        check("tick() grows plots to READY", farm.getPlot(0, 0).isReady());
    }

    private static void checkDays() {
        GameSession s = GameSession.getInstance();
        s.reset();
        for (int i = 0; i < 59; i++) {
            s.tickSecond();
        }
        check("After 59 seconds it is still day 1", s.getCurrentDay() == 1);
        s.tickSecond();
        check("After 60 seconds it is day 2", s.getCurrentDay() == 2);
        for (int i = 0; i < 1739; i++) {
            s.tickSecond();
        }
        check("Just before 30 days are over: day 30, season not over", s.getCurrentDay() == 30 && !s.isSeasonOver());
        s.tickSecond();
        check("After 30 full days the season is over (day stays 30)", s.isSeasonOver() && s.getCurrentDay() == 30);
        s.reset();
    }

    private static void checkStory() {
        GameSession s = GameSession.getInstance();
        s.reset();
        StoryModel story = s.getStory();
        check("Story starts at chapter 1 of 3", story.getSceneNumber() == 1 && story.getSceneCount() == 3);
        check("Cannot advance before choosing a reply", !story.advance());
        StoryModel.StoryReply reply = story.chooseReply(1);   // "Plant the quickest crop": +$5 and a Carrot
        s.applyConsequence(reply.consequence());
        check("Quickest crop gives +$5 and 1 Carrot", s.getBalance() == 105 && s.getInventory().getCounts().get("Carrot") == 1);
        boolean threw = false;
        try {
            story.chooseReply(0);
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("Only one reply per chapter", threw);
        check("Advance goes to chapter 2", story.advance() && story.getSceneNumber() == 2);
        story.chooseReply(0);
        check("Chapter 2 reply 1 costs $10", story.getFinalOutcome().contains("community table"));
        story.advance();
        story.chooseReply(2);
        check("After chapter 3 reply, advance() returns false and story is completed", !story.advance() && story.isCompleted());
        s.reset();
        check("reset() restarts the story at chapter 1", s.getStory().getSceneNumber() == 1 && !s.getStory().isCompleted());
    }

    // Save/load without a database: createSaveData() then restore() must give the same game back.
    private static void checkSaveRoundTrip() {
        GameSession s = GameSession.getInstance();
        s.reset();
        s.addBalance(400);
        s.getFarm().expand();
        s.getInventory().addSeed(SeedFactory.createSeed("Corn"), 3);
        s.getStory().chooseReply(0);
        s.getStory().advance();
        for (int i = 0; i < 125; i++) {
            s.tickSecond();   // 125 s = day 3
        }
        long t = 1_000_000L;
        s.getFarm().getPlot(0, 1).plant(SeedFactory.createSeed("Pumpkin"), t);          // 12 s crop
        s.getFarm().getPlot(3, 3).plant(SeedFactory.createSeed("Carrot"), t - 10_000L); // already ready
        s.getFarm().tick(t);
        SaveData data = s.createSaveData(t + 4_000L);   // pumpkin has grown 4 s
        check("Save has 2 tiles and the time played", data.getTiles().size() == 2 && data.getElapsedSeconds() == 125);

        long later = 9_000_000L;   // loading much later must not grow the crops while the game was closed
        s.reset();
        s.restore(data, later);
        check("Restore brings back balance, day, farm size", s.getBalance() == 500 && s.getCurrentDay() == 3
                && s.getFarm().getSize() == 4);
        check("Restore brings back seeds and story chapter",
                s.getInventory().getCounts().get("Corn") == 3 && s.getStory().getSceneNumber() == 2
                        && !s.getStory().isReplyChosen());
        Plot pumpkin = s.getFarm().getPlot(0, 1);
        check("Pumpkin tile is back, still growing, 4 s grown (stage 2 of 3)",
                "Pumpkin".equals(pumpkin.getSeed().getName()) && !pumpkin.isReady()
                        && pumpkin.getGrownMillis(later) == 4_000L && pumpkin.getStage(later) == 2);
        check("Ready carrot tile is back and ready to harvest", s.getFarm().getPlot(3, 3).isReady());

        java.util.List<SavedTile> badTiles = java.util.Arrays.asList(
                new SavedTile(5, 5, "Carrot", 0L), new SavedTile(0, 0, "Weed", 0L));
        s.restore(new SaveData(-5, 99_999, 99, 99, java.util.Collections.singletonMap("Weed", 4), badTiles), later);
        check("Bad save values are clamped and unknown seeds ignored",
                s.isSeasonOver() && s.getFarm().getSize() == Farm.MAX_SIZE && s.getInventory().isEmpty()
                        && s.getStory().isCompleted() && s.getFarm().getPlot(0, 0).isEmpty());
        s.restore(new SaveData(100, 0, 3, 0, java.util.Collections.<String, Integer>emptyMap(), badTiles), later);
        check("A tile outside a 3x3 farm is ignored", s.getFarm().getPlot(5, 5).isEmpty());
        s.reset();
        check("reset() keeps the logged-in player (Play Again)", keepsPlayer(s));
    }

    private static boolean keepsPlayer(GameSession s) {
        Player me = new Player(7, "Tester");
        s.setPlayer(me);
        s.reset();
        boolean kept = s.getPlayer() == me;
        s.setPlayer(null);
        return kept;
    }

    // Passwords are stored as salt + hash, never as the password itself.
    private static void checkPasswords() {
        String salt = Passwords.newSalt();
        String hash = Passwords.hash(salt, "demo1234".toCharArray());
        check("Hash is 64 hex characters and is not the password", hash.length() == 64 && !hash.contains("demo1234"));
        check("Right password matches, wrong one does not",
                Passwords.matches(salt, hash, "demo1234".toCharArray())
                        && !Passwords.matches(salt, hash, "demo1235".toCharArray()));
        check("Same password with another salt gives another hash",
                !Passwords.hash(Passwords.newSalt(), "demo1234".toCharArray()).equals(hash));
    }
}
