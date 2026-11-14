package codeagarden;

import java.util.List;

/**
 * The garden story: the chapters, the replies you can pick, and how far the player has got.
 * A "record" is a small class that only holds data; Java writes its getters for us
 * (for example scene.title()).
 */
public final class StoryModel {

    private static final String DEFAULT_OUTCOME = "The community garden is ready for its next season.";

    private final List<StoryScene> scenes = List.of(
            new StoryScene("Chapter 1: Starting the Garden",
                    "The gate to the old community garden creaks open. The plots are overgrown,\n"
                            + "but the village festival is only a season away. A small drone powers on\n"
                            + "beside the toolshed, ready to help you bring the garden back to life.\n\n"
                            + "Your first objective is simple: learn the drone, plant carefully, and\n"
                            + "harvest enough crops to make the first steps toward $10,000.",
                    List.of(
                            new StoryReply("Start with a careful plan", new StoryConsequence(0, null, "planner", "The garden begins with a careful, steady plan.")),
                            new StoryReply("Plant the quickest crop", new StoryConsequence(5, SeedFactory.createSeed(SeedFactory.CARROT), "quick-start", "A quick harvest gives the garden an encouraging start.")),
                            new StoryReply("Ask the drone to scout", new StoryConsequence(0, SeedFactory.createSeed(SeedFactory.CARROT), "drone-scout", "The drone maps the best beds, and a spare seed turns up."))
                    )),
            new StoryScene("Chapter 2: Helping the Community",
                    "Mina, a teacher from the nearby school, arrives with a basket.\n\n"
                            + "\"Our students want to help at the festival,\" she says. \"Could you grow\n"
                            + "some food for the community table?\" The garden is still yours to run,\n"
                            + "but everyone is watching to see what it can become.",
                    List.of(
                            new StoryReply("Share the next harvest", new StoryConsequence(-10, null, "community-table", "The school children join in, and the community table is promised.")),
                            new StoryReply("Teach the students about the drone", new StoryConsequence(0, SeedFactory.createSeed(SeedFactory.CORN), "teacher", "The students learn the basics, and Mina gives you a packet of corn seeds.")),
                            new StoryReply("Ask everyone to bring one seed", new StoryConsequence(10, null, "seed-swap", "A friendly seed swap gives the whole neighborhood a stake in the garden."))
                    )),
            new StoryScene("Chapter 3: Preparing for the Festival",
                    "Festival week is here. The paths are clear, the beds are green, and the drone\n"
                            + "hums over the last rows of crops. One final choice will shape the garden's\n"
                            + "welcome for the village when the gates open.",
                    List.of(
                            new StoryReply("Decorate the garden with care", new StoryConsequence(-15, null, "decorated", "The garden shines with a warm welcome for every visitor.")),
                            new StoryReply("Save the best harvest for the feast", new StoryConsequence(25, null, "feast", "The festival feast becomes a celebration of the garden's recovery.")),
                            new StoryReply("Invite the students to lead the opening", new StoryConsequence(0, null, "student-opening", "The students open the gates, proud to share the garden they helped restore."))
                    ))
    );

    private int sceneIndex;
    private boolean replyChosen;
    private boolean completed;
    private String finalOutcome = DEFAULT_OUTCOME;

    public StoryScene currentScene() {
        return scenes.get(sceneIndex);
    }

    public int getSceneNumber() {
        return sceneIndex + 1;
    }

    public int getSceneCount() {
        return scenes.size();
    }

    public boolean isReplyChosen() {
        return replyChosen;
    }

    public boolean isCompleted() {
        return completed;
    }

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

    /** Puts the story back where a save left it. */
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

    public record StoryScene(String title, String dialogue, List<StoryReply> replies) {
        public StoryScene {
            replies = List.copyOf(replies);
        }
    }

    public record StoryReply(String text, StoryConsequence consequence) { }

    public record StoryConsequence(int balanceDelta, Seed seed, String bonusKey, String outcomeMessage) { }
}
