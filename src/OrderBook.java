import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hands out ticket numbers and keeps every submitted order so it can be
 * looked up by its ticket number. Orders are never removed, so the book is
 * also the order history, kept in the order the orders were registered.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderBook {

    /** The ticket number given to the first order. */
    public static final int FIRST_TICKET = 1;

    private final Map<Integer, Order> orders;
    private int nextTicket;

    /**
     * Creates an empty order book whose first ticket is FIRST_TICKET.
     */
    public OrderBook() {
        orders = new LinkedHashMap<Integer, Order>();
        nextTicket = FIRST_TICKET;
    }


    /**
     * Hands out the next unused ticket number. Each call uses the number
     * up, so the same number is never returned twice.
     *
     * @return the next unused ticket number
     */
    public int nextTicket() {
        int ticket = nextTicket;
        nextTicket++;
        return ticket;
    }


    /**
     * Adds an order to the book under its own ticket number.
     *
     * @param order
     *            the order to keep
     * @throws IllegalArgumentException
     *             if the order is null or its ticket number is already in
     *             the book
     */
    public void register(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("The order can't be null.");
        }
        int ticket = order.getTicketNumber();
        if (orders.containsKey(ticket)) {
            throw new IllegalArgumentException("Ticket " + ticket
                + " is already registered.");
        }
        orders.put(ticket, order);
    }


    /**
     * Looks up an order by its ticket number.
     *
     * @param ticket
     *            the ticket number to look up
     * @return the order with that ticket number, or null if there is none
     */
    public Order find(int ticket) {
        return orders.get(ticket);
    }
}
