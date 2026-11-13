import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The single entry point to Order Up!'s data and rules, and the only model
 * class the controller talks to. It owns the menu, the kitchen line, the
 * order book, and the order the cashier is building.
 *
 * The model never prints and never reads input. It reports a broken rule by
 * throwing, and the controller decides what to show.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderUpModel {

    private final List<MenuItem> menu;
    private final KitchenQueue kitchenQueue;
    private final OrderBook orderBook;
    private final List<OrderItem> draft;

    /**
     * Creates a model with the standard food truck menu.
     */
    public OrderUpModel() {
        this(standardMenu());
    }


    /**
     * Creates a model with the given menu.
     *
     * @param menu
     *            the items for sale, in the order they should be listed
     * @throws IllegalArgumentException
     *             if the menu is null, is empty, or contains null
     */
    public OrderUpModel(List<MenuItem> menu) {
        if (menu == null || menu.isEmpty() || menu.contains(null)) {
            throw new IllegalArgumentException(
                "The menu needs at least one item and no missing items.");
        }
        this.menu = Collections.unmodifiableList(new ArrayList<MenuItem>(
            menu));
        this.kitchenQueue = new KitchenQueue();
        this.orderBook = new OrderBook();
        this.draft = new ArrayList<OrderItem>();
    }


    // ----------------------------------------------------------
    // Cashier
    // ----------------------------------------------------------

    /**
     * Gets the menu. The list can be read but not changed.
     *
     * @return a read-only view of the menu
     */
    public List<MenuItem> getMenu() {
        return menu;
    }


    /**
     * Throws away the order being built and starts an empty one.
     */
    public void startNewOrder() {
        draft.clear();
    }


    /**
     * Adds an item to the order being built.
     *
     * @param item
     *            an item from this model's menu
     * @param qty
     *            how many of the item
     * @throws IllegalArgumentException
     *             if the item is null, is not on this model's menu, or is
     *             out of stock, or the quantity is outside Order.MIN_QTY to
     *             Order.MAX_QTY
     */
    public void addToOrder(MenuItem item, int qty) {
        if (item == null) {
            throw new IllegalArgumentException("The item can't be null.");
        }
        if (!menu.contains(item)) {
            throw new IllegalArgumentException(
                "That item is not on the menu.");
        }
        if (!item.isInStock()) {
            throw new IllegalArgumentException(item.getName()
                + " is out of stock.");
        }
        // OrderItem checks the quantity.
        draft.add(new OrderItem(item, qty));
    }


    /**
     * Turns the order being built into a real order: gives it a ticket,
     * records it in the order book, and puts it at the back of the kitchen
     * line. The next order then starts empty.
     *
     * @return the new order's ticket number
     * @throws IllegalStateException
     *             if the order being built has no items
     */
    public int submitOrder() {
        if (draft.isEmpty()) {
            throw new IllegalStateException(
                "An order needs at least one item.");
        }
        // The ticket is only taken here, so an order that is started and
        // then abandoned never uses one up.
        Order order = new Order(orderBook.nextTicket());
        for (OrderItem line : draft) {
            order.addItem(line.getMenuItem(), line.getQuantity());
        }
        orderBook.register(order);
        kitchenQueue.enqueue(order);
        draft.clear();
        return order.getTicketNumber();
    }


    /**
     * Cancels an order that is still waiting. The order stays in the order
     * book and is skipped by the kitchen line.
     *
     * @param ticket
     *            the ticket number of the order to cancel
     * @throws IllegalArgumentException
     *             if no order has that ticket number
     * @throws IllegalStateException
     *             if the order is not waiting
     */
    public void cancelOrder(int ticket) {
        Order order = requireOrder(ticket);
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new IllegalStateException(
                "Only a waiting order can be cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);
    }


    /**
     * Sends a finished order back to the front of the kitchen line to be
     * made again.
     *
     * @param ticket
     *            the ticket number of the order to remake
     * @throws IllegalArgumentException
     *             if no order has that ticket number
     * @throws IllegalStateException
     *             if the order is not ready
     */
    public void remakeOrder(int ticket) {
        Order order = requireOrder(ticket);
        if (order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException(
                "Only a ready order can be remade.");
        }
        order.setStatus(OrderStatus.WAITING);
        kitchenQueue.addRemakeToFront(order);
    }


    // ----------------------------------------------------------
    // Kitchen
    // ----------------------------------------------------------

    /**
     * Takes the order at the front of the kitchen line and marks it ready.
     *
     * @return the order that was completed
     * @throws IllegalStateException
     *             if nobody is waiting
     */
    public Order completeNext() {
        if (kitchenQueue.isEmpty()) {
            throw new IllegalStateException("Nobody is waiting.");
        }
        Order order = kitchenQueue.dequeueFront();
        order.setStatus(OrderStatus.READY);
        return order;
    }


    /**
     * Returns the order at the front of the kitchen line without removing
     * it.
     *
     * @return the order the cook should make next, or null if nobody is
     *         waiting
     */
    public Order peekNext() {
        return kitchenQueue.peekFront();
    }


    /**
     * Counts the orders waiting in the kitchen line.
     *
     * @return the number of waiting orders
     */
    public int waitingCount() {
        return kitchenQueue.size();
    }


    // ----------------------------------------------------------
    // Customer
    // ----------------------------------------------------------

    /**
     * Looks up an order's status and place in line. A ticket number that
     * was never issued is a normal result, not an error.
     *
     * @param ticket
     *            the ticket number to look up
     * @return the order's status and position, or a NOT_FOUND status if no
     *         order has that ticket number
     */
    public StatusInfo getStatus(int ticket) {
        Order order = orderBook.find(ticket);
        if (order == null) {
            return new StatusInfo(ticket, OrderStatus.NOT_FOUND,
                StatusInfo.NOT_IN_LINE);
        }
        return new StatusInfo(ticket, order.getStatus(), kitchenQueue
            .positionOf(ticket));
    }


    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /**
     * Finds an order that has to exist.
     *
     * @param ticket
     *            the ticket number to look up
     * @return the order with that ticket number
     * @throws IllegalArgumentException
     *             if no order has that ticket number
     */
    private Order requireOrder(int ticket) {
        Order order = orderBook.find(ticket);
        if (order == null) {
            throw new IllegalArgumentException("There is no order with "
                + "ticket " + ticket + ".");
        }
        return order;
    }


    /**
     * Builds the menu used when none is given.
     *
     * @return the standard food truck menu
     */
    private static List<MenuItem> standardMenu() {
        List<MenuItem> items = new ArrayList<MenuItem>();
        items.add(new MenuItem("Cheeseburger", 8.99));
        items.add(new MenuItem("Chicken Tacos", 7.49));
        items.add(new MenuItem("Veggie Wrap", 6.99));
        items.add(new MenuItem("Fries", 3.49));
        items.add(new MenuItem("Lemonade", 2.49));
        return items;
    }
}
