import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

/**
 * The kitchen line: a Dequeue of orders. New orders go to the back (FIFO); a
 * remade order is pushed to the FRONT so a botched order is not sent to the
 * back of the line. Cancelled orders are never spliced out of the middle. They
 * stay in the dequeue flagged CANCELLED and are skipped: peekFront, size, and
 * positionOf ignore them, and dequeueFront discards any that reach the front.
 * (They remain in OrderBook as history.)
 */
public class KitchenQueue
{

    private final Deque<Order> orders = new ArrayDeque<>();

    /**
     * Adds a WAITING order to the back of the line.
     * 
     * @param o
     *            the WAITING order to add to the back of the queue
     * @throws IllegalArgumentException
     *             if o is null or not WAITING
     */
    public void enqueue(Order o)
    {
        requireWaiting(o);
        orders.addLast(o);
    }


    /**
     * Adds a remade WAITING order to the front of the line.
     * 
     * @param o
     *            the remade WAITING order to place at the front of the queue
     * @throws IllegalArgumentException
     *             if o is null or not WAITING
     */
    public void addRemakeToFront(Order o)
    {
        requireWaiting(o);
        orders.addFirst(o);
    }


    /**
     * Removes and returns the next active (non-cancelled) order.
     * 
     * @return the next active order in the queue
     * @throws NoSuchElementException
     *             if there is no active order
     */
    public Order dequeueFront()
    {
        while (!orders.isEmpty())
        {
            Order front = orders.pollFirst();
            if (front.getStatus() != OrderStatus.CANCELLED)
            {
                return front;
            }
        }
        throw new NoSuchElementException("kitchen queue is empty");
    }


    /** @return the next active order without removing it, or null if none */
    public Order peekFront()
    {
        for (Order o : orders)
        {
            if (o.getStatus() != OrderStatus.CANCELLED)
            {
                return o;
            }
        }
        return null;
    }


    /**
     * Number of active (non-cancelled) orders waiting.
     * 
     * @return the number of active orders
     */
    public int size()
    {
        int count = 0;
        for (Order o : orders)
        {
            if (o.getStatus() != OrderStatus.CANCELLED)
            {
                count++;
            }
        }
        return count;
    }


    // ----------------------------------------------------------
    /**
     * Place a description of your method here.
     * 
     * @return true if there are no active orders, false otherwise
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }


    /**
     * @param ticket
     *            the ticket number of the order to locate
     * @return 1-based place among active orders (1 = next up), or -1 if the
     *             ticket is not actively waiting in the queue
     */
    public int positionOf(int ticket)
    {
        int position = 0;
        for (Order o : orders)
        {
            if (o.getStatus() == OrderStatus.CANCELLED)
            {
                continue;
            }
            position++;
            if (o.getTicketNumber() == ticket)
            {
                return position;
            }
        }
        return -1;
    }


    private static void requireWaiting(Order o)
    {
        if (o == null)
        {
            throw new IllegalArgumentException("order must not be null");
        }
        if (o.getStatus() != OrderStatus.WAITING)
        {
            throw new IllegalArgumentException(
                "only WAITING orders can be queued, but was " + o.getStatus());
        }
    }
}
