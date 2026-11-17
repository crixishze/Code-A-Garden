package codeagarden.game;

/** One kind of seed/crop. Immutable: once created, its numbers never change. */
public class Seed {
    private final String name;
    private final int price;
    private final int growTime;   // seconds
    private final int sellPrice;

    /** Creates a seed. Only SeedFactory calls this. */
    public Seed(String name, int price, int growTime, int sellPrice) {
        this.name = name;
        this.price = price;
        this.growTime = growTime;
        this.sellPrice = sellPrice;
    }

    /** "Carrot", "Corn", "Tomato" or "Pumpkin". */
    public String getName() {
        return name;
    }

    /** What one seed costs in the shop. */
    public int getPrice() {
        return price;
    }

    /** Seconds the crop needs to grow. */
    public int getGrowTime() {
        return growTime;
    }

    /** Money earned when the crop is harvested. */
    public int getSellPrice() {
        return sellPrice;
    }

    /** A one-line description, handy when debugging. */
    @Override
    public String toString() {
        return String.format("%s Cost: %d  Grows: %ds  Sells: %d%n", name, price, growTime, sellPrice);
    }
}
