package codeagarden;

public class Seed {
    private final String name;
    private final int price;
    private final int growTime;
    private final int sellPrice;

    public Seed(String name, int price, int growTime, int sellPrice) {
        this.name = name;
        this.price = price;
        this.growTime = growTime;
        this.sellPrice = sellPrice;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getGrowTime() {
        return growTime;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    @Override
    public String toString() {
        return String.format("%s Cost: %d  Grows: %ds  Sells: %d%n", name, price, growTime, sellPrice);
    }
}
