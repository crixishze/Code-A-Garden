package codeagarden;

/**
 * One square of soil: empty, growing a crop, or ready to harvest.
 * Time is passed in (in milliseconds) so the rules can be tested without waiting.
 */
public class Plot {

    private static final int MILLIS_PER_SECOND = 1000;
    private static final double STAGE_TWO_PROGRESS = 1.0 / 3.0;
    private static final double STAGE_THREE_PROGRESS = 2.0 / 3.0;

    private PlotState state = PlotState.EMPTY;
    private Seed seed;
    private long plantedAtMillis;

    public PlotState getState() {
        return state;
    }

    public Seed getSeed() {
        return seed;
    }

    public boolean isEmpty() {
        return state == PlotState.EMPTY;
    }

    public boolean isReady() {
        return state == PlotState.READY;
    }

    public void plant(Seed newSeed, long nowMillis) {
        if (state != PlotState.EMPTY) {
            throw new IllegalStateException("Plot is not empty");
        }
        seed = newSeed;
        plantedAtMillis = nowMillis;
        state = PlotState.GROWING;
    }

    /** Which of the 3 pictures to show: 1 = just planted, 3 = nearly or fully grown. */
    public int getStage(long nowMillis) {
        if (seed == null) {
            return 1;
        }
        if (state == PlotState.READY) {
            return 3;
        }
        double progress = getProgress(nowMillis);
        if (progress >= STAGE_THREE_PROGRESS) {
            return 3;
        }
        if (progress >= STAGE_TWO_PROGRESS) {
            return 2;
        }
        return 1;
    }

    /** Call regularly. Returns true on the moment the crop becomes ready. */
    public boolean update(long nowMillis) {
        if (state == PlotState.GROWING && getProgress(nowMillis) >= 1.0) {
            state = PlotState.READY;
            return true;
        }
        return false;
    }

    /** Removes the ready crop and returns its seed (its sell price is the reward). */
    public Seed harvest() {
        if (state != PlotState.READY) {
            throw new IllegalStateException("Nothing to harvest");
        }
        Seed harvested = seed;
        seed = null;
        plantedAtMillis = 0L;
        state = PlotState.EMPTY;
        return harvested;
    }

    /** How long the crop has grown, in milliseconds (used for saving). 0 for an empty plot. */
    public long getGrownMillis(long nowMillis) {
        if (seed == null) {
            return 0L;
        }
        long growMillis = (long) seed.getGrowTime() * MILLIS_PER_SECOND;
        if (state == PlotState.READY) {
            return growMillis;
        }
        return Math.max(0L, Math.min(growMillis, nowMillis - plantedAtMillis));
    }

    /** Puts back a crop from a save: as if it was planted grownMillis ago. */
    public void restore(Seed savedSeed, long grownMillis, long nowMillis) {
        plant(savedSeed, nowMillis - Math.max(0L, grownMillis));
        update(nowMillis);   // a fully grown crop becomes READY straight away
    }

    // 0.0 = just planted, 1.0 = fully grown
    private double getProgress(long nowMillis) {
        long growMillis = Math.max(1, (long) seed.getGrowTime() * MILLIS_PER_SECOND);
        return Math.min(1.0, (nowMillis - plantedAtMillis) / (double) growMillis);
    }
}
