package codeagarden;

import java.util.ArrayList;
import java.util.List;

/**
 * DESIGN PATTERN: Factory Method.
 * The only place that knows how to build each Seed (its price, grow time and sell price).
 * The shop, the story and the inventory ask this factory for a seed by name instead of
 * writing "new Seed(...)" with the numbers themselves. To change a crop or add one,
 * edit this one class.
 */
public final class SeedFactory {

    public static final String CARROT = "Carrot";
    public static final String CORN = "Corn";
    public static final String TOMATO = "Tomato";
    public static final String PUMPKIN = "Pumpkin";

    private SeedFactory() {
        // Only static methods; nobody should create a SeedFactory object.
    }

    /** The factory method: builds the seed with the given name. */
    public static Seed createSeed(String name) {
        switch (name) {
            case CARROT:
                return new Seed(CARROT, 10, 3, 20);
            case CORN:
                return new Seed(CORN, 20, 5, 45);
            case TOMATO:
                return new Seed(TOMATO, 30, 8, 70);
            case PUMPKIN:
                return new Seed(PUMPKIN, 50, 12, 120);
            default:
                throw new IllegalArgumentException("Unknown seed: " + name);
        }
    }

    /** True if this name is one of our seeds (used to ignore bad data in a save). */
    public static boolean isKnownSeed(String name) {
        return CARROT.equals(name) || CORN.equals(name) || TOMATO.equals(name) || PUMPKIN.equals(name);
    }

    /** Every seed the shop sells, cheapest first (the order the shop shows them). */
    public static List<Seed> createAllSeeds() {
        List<Seed> seeds = new ArrayList<>();
        seeds.add(createSeed(CARROT));
        seeds.add(createSeed(CORN));
        seeds.add(createSeed(TOMATO));
        seeds.add(createSeed(PUMPKIN));
        return seeds;
    }
}
