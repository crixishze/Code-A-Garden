package codeagarden;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The seeds the player owns and which one is selected for planting. No screen code here. */
public class PlayerInventory {

    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private String selected;   // name of the seed chosen for planting, null = none

    /** Adds seeds. The first seed you ever own is selected automatically. */
    public void addSeed(Seed seed, int amount) {
        counts.merge(seed.getName(), amount, Integer::sum);
        if (selected == null) {
            selected = seed.getName();
        }
    }

    /** Returns the selected seed and uses up one of it, or null if nothing is selected. */
    public Seed takeSelectedSeed() {
        if (selected == null) {
            return null;
        }
        Seed seed = SeedFactory.createSeed(selected);
        int left = counts.get(selected) - 1;
        if (left <= 0) {
            counts.remove(selected);
            // move the selection to another seed that is still owned, if any
            selected = counts.isEmpty() ? null : counts.keySet().iterator().next();
        } else {
            counts.put(selected, left);
        }
        return seed;
    }

    /** Chooses which owned seed to plant. Ignored if you don't own it. */
    public void select(String seedName) {
        if (counts.containsKey(seedName)) {
            selected = seedName;
        }
    }

    public String getSelectedName() {
        return selected;
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /** Seed name to amount, in the order the seeds were first obtained. Read-only. */
    public Map<String, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }
}
