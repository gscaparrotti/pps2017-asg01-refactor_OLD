package model;

/**
 * Created by gscap_000 on 25/04/2016.
 */
public class Order {

    private final IDish dish;
    private final Pair<Integer, Integer> amounts;

    public Order(final IDish dish, final Pair<Integer, Integer> amounts) {
        this.dish = dish;
        this.amounts = amounts;
    }

    public IDish getDish() {
        return dish;
    }

    public Pair<Integer, Integer> getAmounts() {
        return amounts;
    }
}
