import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One customer order: a ticket number, the items ordered, and a status.
 * <p>
 * The ticket number is set once at construction. The status starts as WAITING
 * and moves to READY when the cook finishes the order.
 */
public class Order
{

    /** The smallest quantity allowed for one line of an order. */
    public static final int MIN_QTY = 1;

    /** The largest quantity allowed for one line of an order. */
    public static final int MAX_QTY = 99;

    private final int ticketNumber;
    private final boolean rush;
    private final List<OrderItem> items;
    private OrderStatus status;

    /**
     * Creates a new Order object with the specified ticket number. By default,
     * the order is not marked as a rush or priority order.
     * 
     * @param ticketNumber
     *            The unique number identifying this order.
     */
    public Order(int ticketNumber)
    {
        this(ticketNumber, false);
    }


    /**
     * Creates an empty order in the WAITING state.
     *
     * @param ticketNumber
     *            this order's ticket number
     * @param rush
     *            true if this is a rush order
     */
    public Order(int ticketNumber, boolean rush)
    {
        this.ticketNumber = ticketNumber;
        this.rush = rush;
        this.items = new ArrayList<>();
        this.status = OrderStatus.WAITING;
    }


    /**
     * Appends an item to the order.
     *
     * @param item
     *            the menu item; must not be null
     * @param qty
     *            the quantity; must be from MIN_QTY to MAX_QTY
     * @throws IllegalArgumentException
     *             if item is null or qty is outside MIN_QTY to MAX_QTY
     */
    public void addItem(MenuItem item, int qty)
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Item must not be null");
        }
        if (qty < 1)
        {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        items.add(new OrderItem(item, qty));
    }


    /** @return this order's ticket number */
    public int getTicketNumber()
    {
        return ticketNumber;
    }


    /** @return true if this is a rush order */
    public boolean isRush()
    {
        return rush;
    }


    /** @return a read-only view of the items on this order */
    public List<OrderItem> getItems()
    {
        return Collections.unmodifiableList(items);
    }


    /** @return true if no items have been added yet */
    public boolean isEmpty()
    {
        return items.isEmpty();
    }


    /** @return this order's current status */
    public OrderStatus getStatus()
    {
        return status;
    }


    /**
     * Changes this order's status.
     *
     * @param status
     *            the new status; must not be null
     * @throws IllegalArgumentException
     *             if status is null
     */
    public void setStatus(OrderStatus status)
    {
        if (status == null)
        {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }


    /** @return total price of all items on the order */
    public double getTotal()
    {
        double total = 0;
        for (OrderItem oi : items)
        {
            total += oi.getLineTotal();
        }
        return total;
    }


    @Override
    public String toString()
    {
        return "Order #" + ticketNumber + " " + items + " [" + status + "]";
    }
}
