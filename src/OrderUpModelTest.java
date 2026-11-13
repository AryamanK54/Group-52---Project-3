import java.util.ArrayList;
import java.util.List;
import student.TestCase;

/**
 * Tests for the OrderUpModel class. Nothing here uses a view or the
 * controller, so the model is shown to work on its own.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderUpModelTest extends TestCase {

    private OrderUpModel model;
    private MenuItem burger;
    private MenuItem fries;
    private MenuItem soda;

    /**
     * Creates a model with a three-item menu for each test method.
     */
    public void setUp() {
        burger = new MenuItem("Burger", 8.99);
        fries = new MenuItem("Fries", 3.49);
        soda = new MenuItem("Soda", 1.99);
        List<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(burger);
        menu.add(fries);
        menu.add(soda);
        model = new OrderUpModel(menu);
    }


    // ----------------------------------------------------------
    // Construction and menu
    // ----------------------------------------------------------

    /**
     * A new model has its menu, nobody waiting, and no orders.
     */
    public void testNewModel() {
        assertEquals(3, model.getMenu().size());
        assertSame(burger, model.getMenu().get(0));
        assertSame(soda, model.getMenu().get(2));
        assertEquals(0, model.waitingCount());
        assertNull(model.peekNext());
    }


    /**
     * The model with no menu given has the standard menu, all in stock.
     */
    public void testStandardMenu() {
        OrderUpModel standard = new OrderUpModel();
        assertFalse(standard.getMenu().isEmpty());
        for (MenuItem item : standard.getMenu()) {
            assertTrue(item.isInStock());
        }
        assertEquals(0, standard.waitingCount());
    }


    /**
     * The menu can't be changed through getMenu.
     */
    public void testMenuIsReadOnly() {
        Exception thrown = null;
        try {
            model.getMenu().add(new MenuItem("Pie", 4.00));
        }
        catch (UnsupportedOperationException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(3, model.getMenu().size());
    }


    /**
     * Changing the list the model was built from does not change the menu.
     */
    public void testMenuIsCopied() {
        List<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(burger);
        OrderUpModel small = new OrderUpModel(menu);
        menu.add(fries);
        assertEquals(1, small.getMenu().size());
    }


    /**
     * A missing menu, an empty menu, or a menu with a gap is rejected.
     */
    public void testBadMenu() {
        List<MenuItem> empty = new ArrayList<MenuItem>();
        List<MenuItem> gap = new ArrayList<MenuItem>();
        gap.add(burger);
        gap.add(null);
        assertFalse(canBuild(null));
        assertFalse(canBuild(empty));
        assertFalse(canBuild(gap));
    }


    // ----------------------------------------------------------
    // addToOrder
    // ----------------------------------------------------------

    /**
     * Items added to the order show up on the submitted order.
     */
    public void testAddToOrder() {
        model.addToOrder(burger, 2);
        model.addToOrder(fries, 1);
        model.submitOrder();
        List<OrderItem> items = model.peekNext().getItems();
        assertEquals(2, items.size());
        assertSame(burger, items.get(0).getMenuItem());
        assertEquals(2, items.get(0).getQuantity());
        assertSame(fries, items.get(1).getMenuItem());
        assertEquals(2 * 8.99 + 3.49, model.peekNext().getTotal(), 0.001);
    }


    /**
     * Quantities 1 and 99 are accepted, and 0 and 100 are rejected.
     */
    public void testAddToOrderQuantityBounds() {
        assertFalse(canAdd(burger, 0));
        assertTrue(canAdd(burger, 1));
        assertTrue(canAdd(burger, 99));
        assertFalse(canAdd(burger, 100));
        model.submitOrder();
        assertEquals(2, model.peekNext().getItems().size());
    }


    /**
     * A null item is rejected and nothing is added.
     */
    public void testAddToOrderNull() {
        assertFalse(canAdd(null, 1));
        assertFalse(canSubmit());
    }


    /**
     * An item that is not on this model's menu is rejected, even if it
     * looks the same as one that is.
     */
    public void testAddToOrderNotOnMenu() {
        assertFalse(canAdd(new MenuItem("Pie", 4.00), 1));
        assertFalse(canAdd(new MenuItem("Burger", 8.99), 1));
        assertFalse(canSubmit());
    }


    /**
     * An out-of-stock item is rejected until it is back in stock.
     */
    public void testAddToOrderOutOfStock() {
        burger.setInStock(false);
        assertFalse(canAdd(burger, 1));
        assertFalse(canSubmit());
        burger.setInStock(true);
        assertTrue(canAdd(burger, 1));
        assertTrue(canSubmit());
    }


    /**
     * Starting a new order throws away the items already added.
     */
    public void testStartNewOrder() {
        model.addToOrder(burger, 1);
        model.startNewOrder();
        assertFalse(canSubmit());
        model.addToOrder(fries, 1);
        model.submitOrder();
        assertEquals(1, model.peekNext().getItems().size());
        assertSame(fries, model.peekNext().getItems().get(0).getMenuItem());
    }


    // ----------------------------------------------------------
    // submitOrder
    // ----------------------------------------------------------

    /**
     * Submitting an empty order throws and creates nothing.
     */
    public void testSubmitEmptyOrder() {
        Exception thrown = null;
        try {
            model.submitOrder();
        }
        catch (IllegalStateException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(0, model.waitingCount());
    }


    /**
     * A submitted order is waiting at the back of the line under its
     * ticket.
     */
    public void testSubmitOrder() {
        int ticket = place(burger);
        assertEquals(OrderBook.FIRST_TICKET, ticket);
        assertEquals(1, model.waitingCount());
        assertEquals(ticket, model.peekNext().getTicketNumber());
        assertEquals(OrderStatus.WAITING, model.peekNext().getStatus());
    }


    /**
     * Tickets go up by one, and a failed submit does not use one up.
     */
    public void testTicketsAreSequential() {
        int first = place(burger);
        assertFalse(canSubmit());
        model.addToOrder(fries, 1);
        model.startNewOrder();
        int second = place(fries);
        int third = place(soda);
        assertEquals(first + 1, second);
        assertEquals(first + 2, third);
        assertEquals(3, model.waitingCount());
    }


    /**
     * After a submit the next order starts empty.
     */
    public void testSubmitClearsOrder() {
        place(burger);
        assertFalse(canSubmit());
        assertEquals(1, model.waitingCount());
    }


    // ----------------------------------------------------------
    // completeNext and peekNext
    // ----------------------------------------------------------

    /**
     * Completing from an empty line throws.
     */
    public void testCompleteNextEmptyLine() {
        Exception thrown = null;
        try {
            model.completeNext();
        }
        catch (IllegalStateException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(0, model.waitingCount());
    }


    /**
     * Orders are completed oldest first and come back marked ready.
     */
    public void testCompleteNext() {
        int first = place(burger);
        int second = place(fries);
        Order done = model.completeNext();
        assertEquals(first, done.getTicketNumber());
        assertEquals(OrderStatus.READY, done.getStatus());
        assertEquals(1, model.waitingCount());
        assertEquals(second, model.peekNext().getTicketNumber());
        assertEquals(second, model.completeNext().getTicketNumber());
        assertNull(model.peekNext());
    }


    /**
     * Looking at the next order does not remove it.
     */
    public void testPeekNext() {
        int ticket = place(burger);
        assertEquals(ticket, model.peekNext().getTicketNumber());
        assertEquals(ticket, model.peekNext().getTicketNumber());
        assertEquals(1, model.waitingCount());
    }


    // ----------------------------------------------------------
    // getStatus
    // ----------------------------------------------------------

    /**
     * A waiting order reports its place in line.
     */
    public void testGetStatusWaiting() {
        int first = place(burger);
        int second = place(fries);
        StatusInfo info = model.getStatus(second);
        assertEquals(second, info.getTicket());
        assertEquals(OrderStatus.WAITING, info.getStatus());
        assertEquals(2, info.getPosition());
        assertEquals(1, model.getStatus(first).getPosition());
    }


    /**
     * A completed order is ready and has no place in line, and the order
     * behind it moves up.
     */
    public void testGetStatusReady() {
        int first = place(burger);
        int second = place(fries);
        model.completeNext();
        assertEquals(OrderStatus.READY, model.getStatus(first).getStatus());
        assertEquals(StatusInfo.NOT_IN_LINE, model.getStatus(first)
            .getPosition());
        assertEquals(1, model.getStatus(second).getPosition());
    }


    /**
     * An unknown ticket is reported as not found without throwing.
     */
    public void testGetStatusNotFound() {
        int ticket = place(burger);
        StatusInfo info = model.getStatus(ticket + 1);
        assertEquals(ticket + 1, info.getTicket());
        assertEquals(OrderStatus.NOT_FOUND, info.getStatus());
        assertEquals(StatusInfo.NOT_IN_LINE, info.getPosition());
        assertEquals(OrderStatus.NOT_FOUND, model.getStatus(0).getStatus());
        assertEquals(OrderStatus.NOT_FOUND, model.getStatus(-4).getStatus());
    }


    // ----------------------------------------------------------
    // cancelOrder
    // ----------------------------------------------------------

    /**
     * A cancelled order is flagged, kept in the history, and skipped by
     * the line.
     */
    public void testCancelOrder() {
        int first = place(burger);
        int second = place(fries);
        int third = place(soda);
        model.cancelOrder(second);
        StatusInfo info = model.getStatus(second);
        assertEquals(OrderStatus.CANCELLED, info.getStatus());
        assertEquals(StatusInfo.NOT_IN_LINE, info.getPosition());
        assertEquals(2, model.waitingCount());
        assertEquals(2, model.getStatus(third).getPosition());
        assertEquals(first, model.completeNext().getTicketNumber());
        assertEquals(third, model.completeNext().getTicketNumber());
        assertEquals(0, model.waitingCount());
    }


    /**
     * Cancelling the front order makes the one behind it next.
     */
    public void testCancelFrontOrder() {
        int first = place(burger);
        int second = place(fries);
        model.cancelOrder(first);
        assertEquals(second, model.peekNext().getTicketNumber());
        assertEquals(1, model.getStatus(second).getPosition());
        assertEquals(second, model.completeNext().getTicketNumber());
    }


    /**
     * Cancelling the only order leaves an empty line.
     */
    public void testCancelOnlyOrder() {
        int ticket = place(burger);
        model.cancelOrder(ticket);
        assertEquals(0, model.waitingCount());
        assertNull(model.peekNext());
        Exception thrown = null;
        try {
            model.completeNext();
        }
        catch (IllegalStateException e) {
            thrown = e;
        }
        assertNotNull(thrown);
    }


    /**
     * Cancelling an unknown ticket throws and changes nothing.
     */
    public void testCancelUnknownTicket() {
        int ticket = place(burger);
        Exception thrown = null;
        try {
            model.cancelOrder(ticket + 1);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(1, model.waitingCount());
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket)
            .getStatus());
    }


    /**
     * An order that is ready or already cancelled can't be cancelled.
     */
    public void testCancelNotWaiting() {
        int ready = place(burger);
        int cancelled = place(fries);
        model.completeNext();
        model.cancelOrder(cancelled);
        assertFalse(canCancel(ready));
        assertFalse(canCancel(cancelled));
        assertEquals(OrderStatus.READY, model.getStatus(ready).getStatus());
        assertEquals(OrderStatus.CANCELLED, model.getStatus(cancelled)
            .getStatus());
    }


    // ----------------------------------------------------------
    // remakeOrder
    // ----------------------------------------------------------

    /**
     * A remade order is waiting again at the front, ahead of the orders
     * already in line.
     */
    public void testRemakeOrder() {
        int first = place(burger);
        int second = place(fries);
        int third = place(soda);
        model.completeNext();
        model.remakeOrder(first);
        assertEquals(OrderStatus.WAITING, model.getStatus(first)
            .getStatus());
        assertEquals(1, model.getStatus(first).getPosition());
        assertEquals(2, model.getStatus(second).getPosition());
        assertEquals(3, model.getStatus(third).getPosition());
        assertEquals(3, model.waitingCount());
        assertEquals(first, model.completeNext().getTicketNumber());
        assertEquals(second, model.completeNext().getTicketNumber());
    }


    /**
     * A remade order keeps its ticket number and its items.
     */
    public void testRemakeKeepsOrder() {
        int ticket = place(burger);
        Order done = model.completeNext();
        model.remakeOrder(ticket);
        assertSame(done, model.peekNext());
        assertEquals(1, model.peekNext().getItems().size());
    }


    /**
     * Remaking an unknown ticket throws and changes nothing.
     */
    public void testRemakeUnknownTicket() {
        int ticket = place(burger);
        model.completeNext();
        Exception thrown = null;
        try {
            model.remakeOrder(ticket + 1);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(0, model.waitingCount());
    }


    /**
     * An order that is still waiting or was cancelled can't be remade.
     */
    public void testRemakeNotReady() {
        int waiting = place(burger);
        int cancelled = place(fries);
        model.cancelOrder(cancelled);
        assertFalse(canRemake(waiting));
        assertFalse(canRemake(cancelled));
        assertEquals(1, model.waitingCount());
        assertEquals(1, model.getStatus(waiting).getPosition());
    }


    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /**
     * Submits an order for one of an item.
     *
     * @param item
     *            the item to order
     * @return the new order's ticket number
     */
    private int place(MenuItem item) {
        model.addToOrder(item, 1);
        return model.submitOrder();
    }


    /**
     * Tries to build a model with a menu.
     *
     * @param menu
     *            the menu to use
     * @return false if the menu was rejected
     */
    private boolean canBuild(List<MenuItem> menu) {
        try {
            new OrderUpModel(menu);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }


    /**
     * Tries to add an item to the order being built.
     *
     * @param item
     *            the item to add
     * @param qty
     *            how many of the item
     * @return false if the model rejected the item
     */
    private boolean canAdd(MenuItem item, int qty) {
        try {
            model.addToOrder(item, qty);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }


    /**
     * Tries to submit the order being built.
     *
     * @return false if the model rejected the order as empty
     */
    private boolean canSubmit() {
        try {
            model.submitOrder();
        }
        catch (IllegalStateException e) {
            return false;
        }
        return true;
    }


    /**
     * Tries to cancel an order that exists.
     *
     * @param ticket
     *            the ticket number to cancel
     * @return false if the model rejected the cancel
     */
    private boolean canCancel(int ticket) {
        try {
            model.cancelOrder(ticket);
        }
        catch (IllegalStateException e) {
            return false;
        }
        return true;
    }


    /**
     * Tries to remake an order that exists.
     *
     * @param ticket
     *            the ticket number to remake
     * @return false if the model rejected the remake
     */
    private boolean canRemake(int ticket) {
        try {
            model.remakeOrder(ticket);
        }
        catch (IllegalStateException e) {
            return false;
        }
        return true;
    }
}
