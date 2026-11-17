package codeagarden.game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The seeds the player owns: seed name to amount. No screen code here.
 * The shop adds seeds; plant(Carrot) in the player's code takes one out.
 */
public class PlayerInventory {

    private final Map<String, Integer> counts = new LinkedHashMap<>();

    /** Adds seeds of this kind (buying in the shop, or a story reward). */
    public void addSeed(Seed seed, int amount) {
        counts.merge(seed.getName(), amount, Integer::sum);
    }

    /** How many seeds of this kind the player owns (0 if none). */
    public int getCount(String seedName) {
        return counts.getOrDefault(seedName, 0);
    }

    /**
     * Uses up one seed of this kind and returns it, ready to plant.
     * Returns null if the player owns none of that seed.
     */
    public Seed takeSeed(String seedName) {
        int owned = getCount(seedName);
        if (owned <= 0) {
            return null;
        }
        if (owned == 1) {
            counts.remove(seedName);   // the last one: the seed disappears from the list
        } else {
            counts.put(seedName, owned - 1);
        }
        return SeedFactory.createSeed(seedName);
    }

    /** True if the player owns no seeds at all. */
    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /** Seed name to amount, in the order the seeds were first obtained. Read-only. */
    public Map<String, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }
}
