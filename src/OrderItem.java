/**
 * Pairs a menu item with the quantity ordered. Immutable.
 * <p>
 * This class only checks that the data is usable (non-null item, quantity of
 * at least 1). The 1-99 limit and the in-stock rule are business rules that
 * live in OrderUpModel.
 */
public class OrderItem {

    private final MenuItem item;
    private final int quantity;

    /**
     * @param item     the menu item ordered; must not be null
     * @param quantity how many were ordered; must be at least 1
     * @throws IllegalArgumentException if item is null or quantity is below 1
     */
    public OrderItem(MenuItem item, int quantity) {
        if (item == null) {
            throw new IllegalArgumentException("Order item must have a menu item");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.item = item;
        this.quantity = quantity;
    }

    /** @return the menu item ordered */
    public MenuItem getItem() {
        return item;
    }

    /** @return the quantity ordered */
    public int getQuantity() {
        return quantity;
    }

    /** @return price of this line: item price times quantity */
    public double getLineTotal() {
        return item.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return quantity + " x " + item.getName();
    }
}
