import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One customer order: a ticket number, the items ordered, and a status.
 * <p>
 * The ticket number is set once at construction. The status starts as WAITING
 * and moves to READY when the cook finishes the order.
 */
public class Order {

    private final int ticketNumber;
    private final List<OrderItem> items;
    private OrderStatus status;

    /**
     * Creates an empty order in the WAITING state.
     *
     * @param ticketNumber this order's ticket number
     */
    public Order(int ticketNumber) {
        this.ticketNumber = ticketNumber;
        this.items = new ArrayList<>();
        this.status = OrderStatus.WAITING;
    }

    /**
     * Appends an item to the order.
     *
     * @param item the menu item; must not be null
     * @param qty  the quantity; must be at least 1
     * @throws IllegalArgumentException if item is null or qty is below 1
     */
    public void addItem(MenuItem item, int qty) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        if (qty < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        items.add(new OrderItem(item, qty));
    }

    /** @return this order's ticket number */
    public int getTicketNumber() {
        return ticketNumber;
    }

    /** @return a read-only view of the items on this order */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** @return true if no items have been added yet */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** @return this order's current status */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Changes this order's status.
     *
     * @param status the new status; must not be null
     * @throws IllegalArgumentException if status is null
     */
    public void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }

    /** @return total price of all items on the order */
    public double getTotal() {
        double total = 0;
        for (OrderItem oi : items) {
            total += oi.getLineTotal();
        }
        return total;
    }

    @Override
    public String toString() {
        return "Order #" + ticketNumber + " " + items + " [" + status + "]";
    }
}
