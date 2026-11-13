import student.TestCase;

/**
 * Tests for the OrderBook class.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderBookTest extends TestCase {

    private OrderBook book;

    /**
     * Creates a brand new, empty order book for each test method.
     */
    public void setUp() {
        book = new OrderBook();
    }


    // ----------------------------------------------------------
    // nextTicket
    // ----------------------------------------------------------

    /**
     * The first ticket handed out is FIRST_TICKET.
     */
    public void testFirstTicket() {
        assertEquals(OrderBook.FIRST_TICKET, book.nextTicket());
    }


    /**
     * Tickets go up by one each time and never repeat.
     */
    public void testTicketsAreSequential() {
        int first = book.nextTicket();
        assertEquals(first + 1, book.nextTicket());
        assertEquals(first + 2, book.nextTicket());
        assertEquals(first + 3, book.nextTicket());
    }


    /**
     * Each book counts its own tickets.
     */
    public void testBooksCountSeparately() {
        book.nextTicket();
        book.nextTicket();
        OrderBook other = new OrderBook();
        assertEquals(OrderBook.FIRST_TICKET, other.nextTicket());
    }


    /**
     * Handing out a ticket number does not register an order for it.
     */
    public void testNextTicketDoesNotRegister() {
        int ticket = book.nextTicket();
        assertNull(book.find(ticket));
    }


    // ----------------------------------------------------------
    // register and find
    // ----------------------------------------------------------

    /**
     * A registered order is found by its ticket number, and it is the same
     * object that was registered.
     */
    public void testRegisterThenFind() {
        Order order = new Order(book.nextTicket());
        book.register(order);
        assertSame(order, book.find(order.getTicketNumber()));
    }


    /**
     * Several registered orders are each found under their own ticket.
     */
    public void testFindAmongSeveral() {
        Order first = new Order(book.nextTicket());
        Order second = new Order(book.nextTicket());
        Order third = new Order(book.nextTicket());
        book.register(first);
        book.register(second);
        book.register(third);
        assertSame(first, book.find(first.getTicketNumber()));
        assertSame(second, book.find(second.getTicketNumber()));
        assertSame(third, book.find(third.getTicketNumber()));
    }


    /**
     * Registering an order does not use up a ticket number.
     */
    public void testRegisterDoesNotUseTicket() {
        int first = book.nextTicket();
        book.register(new Order(first));
        assertEquals(first + 1, book.nextTicket());
    }


    /**
     * find returns null in an empty book.
     */
    public void testFindInEmptyBook() {
        assertNull(book.find(OrderBook.FIRST_TICKET));
    }


    /**
     * find returns null for a ticket number that was never registered.
     */
    public void testFindUnknownTicket() {
        Order order = new Order(book.nextTicket());
        book.register(order);
        assertNull(book.find(order.getTicketNumber() + 1));
        assertNull(book.find(0));
        assertNull(book.find(-1));
    }


    /**
     * Registering null throws and leaves the book working.
     */
    public void testRegisterNull() {
        Exception thrown = null;
        try {
            book.register(null);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertNull(book.find(OrderBook.FIRST_TICKET));
    }


    /**
     * Registering a second order with a ticket number already in the book
     * throws and keeps the first order.
     */
    public void testRegisterDuplicateTicket() {
        int ticket = book.nextTicket();
        Order original = new Order(ticket);
        Order copy = new Order(ticket);
        book.register(original);
        Exception thrown = null;
        try {
            book.register(copy);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertSame(original, book.find(ticket));
    }


    /**
     * Registering the same order twice throws.
     */
    public void testRegisterSameOrderTwice() {
        Order order = new Order(book.nextTicket());
        book.register(order);
        Exception thrown = null;
        try {
            book.register(order);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertSame(order, book.find(order.getTicketNumber()));
    }
}
