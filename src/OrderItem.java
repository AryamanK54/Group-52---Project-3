/**
 * Pairs a menu item with the quantity ordered. Immutable.
 * <p>
 * The quantity must be from Order.MIN_QTY to Order.MAX_QTY (1 to 99). This is
 * the one place that range is enforced, so both Order.addItem and
 * OrderUpModel.addToOrder reject a bad quantity through this constructor. The
 * in-stock rule lives in OrderUpModel.
 */
public class OrderItem {

    private final MenuItem item;
    private final int quantity;

    /**
     * @param item     the menu item ordered; must not be null
     * @param quantity how many were ordered; must be from Order.MIN_QTY to
     *                 Order.MAX_QTY
     * @throws IllegalArgumentException if item is null or quantity is outside
     *                                  Order.MIN_QTY to Order.MAX_QTY
     */
    public OrderItem(MenuItem item, int quantity) {
        if (item == null) {
            throw new IllegalArgumentException("Order item must have a menu item");
        }
        if (quantity < Order.MIN_QTY || quantity > Order.MAX_QTY) {
            throw new IllegalArgumentException("Quantity must be from "
                    + Order.MIN_QTY + " to " + Order.MAX_QTY);
        }
        this.item = item;
        this.quantity = quantity;
    }

    /** @return the menu item ordered */
    public MenuItem getMenuItem() {
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
