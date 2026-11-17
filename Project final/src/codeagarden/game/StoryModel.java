package codeagarden.game;

import java.util.List;

/**
 * The garden story: three chapters, each with a few pages to read and then a choice of
 * replies, plus the win and lose endings. It also remembers how far the player has got.
 *
 * A "record" (StoryScene, StoryReply, StoryConsequence) is a small class that only holds
 * data; Java writes its constructor and getters for us (for example scene.title()).
 *
 * Only the number of finished chapters is saved (SAVE.chapters_done, 0 to 3), so the
 * database did not change when pages were added.
 */
public final class StoryModel {

    private static final String DEFAULT_OUTCOME = "The community garden is ready for its next season.";

    private final List<StoryScene> scenes = List.of(
            new StoryScene("Chapter 1: Starting the Garden",
                    List.of(
                            "The gate to the old community garden creaks open. Weeds have swallowed the\n"
                                    + "paths, the fence leans, and the soil beds are hard and grey.\n\n"
                                    + "Old Mr. Santos, who kept this garden for forty years, leans on his cane.\n"
                                    + "\"The village festival is only a season away,\" he says. \"Thirty days.\n"
                                    + "If the garden can earn $10,000, we can pay for the festival and keep\n"
                                    + "these gates open for good.\"",
                            "Beside the toolshed, a dusty farming drone blinks awake.\n\n"
                                    + "\"It doesn't take orders by hand,\" Mr. Santos chuckles. \"It reads code.\n"
                                    + "Tell it where to move, what to plant and when to harvest, and it will\n"
                                    + "work all day without a break.\"\n\n"
                                    + "He points at the 4 by 6 field in front of you: twenty-four plots,\n"
                                    + "waiting for seeds.",
                            "\"Start small,\" he advises. \"Carrots grow in three seconds. Pumpkins\n"
                                    + "take longer but sell for much more. Buy seeds in the shop, then write\n"
                                    + "a program that plants them and harvests them when they're ready.\"\n\n"
                                    + "He hands you the toolshed key. \"How do you want to begin?\""),
                    List.of(
                            new StoryReply("Start with a careful plan", new StoryConsequence(0, null, "planner",
                                    "The garden begins with a careful, steady plan.")),
                            new StoryReply("Plant the quickest crop", new StoryConsequence(5, SeedFactory.createSeed(SeedFactory.CARROT), "quick-start",
                                    "A quick harvest gives the garden an encouraging start.")),
                            new StoryReply("Ask the drone to scout", new StoryConsequence(0, SeedFactory.createSeed(SeedFactory.CARROT), "drone-scout",
                                    "The drone maps the best beds, and a spare seed turns up."))
                    )),
            new StoryScene("Chapter 2: Helping the Community",
                    List.of(
                            "A week later, the first green rows catch the morning light. People walking\n"
                                    + "past the fence start to slow down and look.\n\n"
                                    + "Mina, a teacher from the nearby school, arrives with a basket and a\n"
                                    + "line of curious students behind her.",
                            "\"Our students want to help at the festival,\" she says. \"Could the garden\n"
                                    + "grow some food for the community table? Nothing fancy. Just enough so\n"
                                    + "every family gets a plate.\"\n\n"
                                    + "One of the students is staring at the drone. \"Does it really run on\n"
                                    + "code? Can we write some too?\"",
                            "Mr. Santos smiles from his bench. \"The garden is yours to run,\" he tells\n"
                                    + "you. \"But everyone is watching to see what it can become.\"\n\n"
                                    + "Mina waits for your answer."),
                    List.of(
                            new StoryReply("Share the next harvest", new StoryConsequence(-10, null, "community-table",
                                    "The school children join in, and the community table is promised.")),
                            new StoryReply("Teach the students about the drone", new StoryConsequence(0, SeedFactory.createSeed(SeedFactory.CORN), "teacher",
                                    "The students learn the basics, and Mina gives you a packet of corn seeds.")),
                            new StoryReply("Ask everyone to bring one seed", new StoryConsequence(10, null, "seed-swap",
                                    "A friendly seed swap gives the whole neighborhood a stake in the garden."))
                    )),
            new StoryScene("Chapter 3: Preparing for the Festival",
                    List.of(
                            "A storm rolls through in the middle of the season. In the morning, the fence\n"
                                    + "is down in two places and a few plots are full of mud.\n\n"
                                    + "Before you can even start cleaning, the students arrive with shovels.\n"
                                    + "Mina is behind them, and behind her, half the village.",
                            "By evening the fence stands straight again. Mr. Santos looks over the\n"
                                    + "field, the drone humming above the rows.\n\n"
                                    + "\"Forty years I kept this place,\" he says quietly, \"and I never saw this\n"
                                    + "many people in it.\"",
                            "Festival week is almost here. The paths are clear, the beds are green, and\n"
                                    + "the money box is filling up. One final choice will shape how the garden\n"
                                    + "welcomes the village when the gates open.\n\n"
                                    + "Keep farming: the festival only happens if the garden reaches $10,000\n"
                                    + "before the thirty days are over."),
                    List.of(
                            new StoryReply("Decorate the garden with care", new StoryConsequence(-15, null, "decorated",
                                    "The garden shines with a warm welcome for every visitor.")),
                            new StoryReply("Save the best harvest for the feast", new StoryConsequence(25, null, "feast",
                                    "The festival feast becomes a celebration of the garden's recovery.")),
                            new StoryReply("Invite the students to lead the opening", new StoryConsequence(0, null, "student-opening",
                                    "The students open the gates, proud to share the garden they helped restore."))
                    ))
    );

    private int sceneIndex;
    private boolean replyChosen;
    private boolean completed;
    private String finalOutcome = DEFAULT_OUTCOME;

    /** The chapter the player is on now. */
    public StoryScene currentScene() {
        return scenes.get(sceneIndex);
    }

    /** The current chapter's number, 1 to 3. */
    public int getSceneNumber() {
        return sceneIndex + 1;
    }

    /** How many chapters the story has (3). */
    public int getSceneCount() {
        return scenes.size();
    }

    /** True once a reply has been picked in the current chapter. */
    public boolean isReplyChosen() {
        return replyChosen;
    }

    /** True once every chapter is finished. */
    public boolean isCompleted() {
        return completed;
    }

    /** The outcome sentence of the last reply chosen; used in the endings. */
    public String getFinalOutcome() {
        return finalOutcome;
    }

    /**
     * How many chapters are finished (0 to 3). A chapter counts once its reply is chosen.
     * This is what gets saved.
     */
    public int getChaptersDone() {
        return sceneIndex + (replyChosen ? 1 : 0);
    }

    /** Puts the story back where a save left it (at the first page of the next chapter). */
    public void restoreChaptersDone(int done) {
        int clamped = Math.max(0, Math.min(scenes.size(), done));
        if (clamped >= scenes.size()) {
            sceneIndex = scenes.size() - 1;
            replyChosen = true;
            completed = true;
        } else {
            sceneIndex = clamped;
            replyChosen = false;
            completed = false;
        }
    }

    /** The player picks one reply in the current chapter (only once per chapter). */
    public StoryReply chooseReply(int replyIndex) {
        if (replyChosen) {
            throw new IllegalStateException("A reply has already been chosen for this scene");
        }
        StoryReply reply = currentScene().replies().get(replyIndex);
        replyChosen = true;
        finalOutcome = reply.consequence().outcomeMessage();
        return reply;
    }

    /** Moves to the next chapter. Returns false if there is none (or no reply was chosen yet). */
    public boolean advance() {
        if (!replyChosen) {
            return false;
        }
        if (sceneIndex == scenes.size() - 1) {
            completed = true;
            return false;
        }
        sceneIndex++;
        replyChosen = false;
        return true;
    }

    /** The text shown when the player reaches $10,000 (the game is won). */
    public String getWinEnding(int day, int balance) {
        return "The garden reached its goal on day " + day + " with $" + String.format("%,d", balance) + "!\n\n"
                + "The festival opens under strings of lights. Families fill the long tables,\n"
                + "the students show their parents the drone, and Mr. Santos cuts the ribbon\n"
                + "on the gate with shaking hands.\n\n"
                + finalOutcome + "\n\n"
                + "Congratulations! The community garden is saved, and its gates will stay open\n"
                + "for many seasons to come.";
    }

    /** The text shown when the 30 days end before $10,000 (the game is lost). */
    public String getLoseEnding(int balance) {
        return "The thirty days are over. The garden earned $" + String.format("%,d", balance)
                + " of the $" + String.format("%,d", GameSession.GOAL) + " it needed.\n\n"
                + "The festival is held in the school hall instead, and the garden gate is\n"
                + "locked again for the winter. Mr. Santos pats the drone on its way back into\n"
                + "the toolshed. \"Next season,\" he says. \"Now we know what it can do.\"\n\n"
                + finalOutcome + "\n\n"
                + "Better luck next time: plan your code, keep every plot busy, and try again!";
    }

    /** One chapter: a title, the pages read in order, then the replies to choose from. */
    public record StoryScene(String title, List<String> pages, List<StoryReply> replies) {
        public StoryScene {
            pages = List.copyOf(pages);
            replies = List.copyOf(replies);
        }
    }

    /** One reply button: its text and what happens when it is picked. */
    public record StoryReply(String text, StoryConsequence consequence) { }

    /** What a reply does: money change, a free seed (or null), a short key, and the outcome sentence. */
    public record StoryConsequence(int balanceDelta, Seed seed, String bonusKey, String outcomeMessage) { }
}
