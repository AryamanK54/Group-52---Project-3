import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import student.TestCase;

/**
 * Tests for the OrderUpController class. The controller runs against the
 * real model and the real views, with typed input replaced by a string and
 * the console replaced by a buffer the tests can read.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderUpControllerTest extends TestCase {

    private OrderUpModel model;
    private OrderUpController controller;
    private ByteArrayOutputStream screen;

    // A second set of views that print into their own buffer. A test calls
    // one of them to get the exact text a view prints, then checks that the
    // same text reached the screen. That way the tests don't copy the
    // wording that belongs to the views.
    private ByteArrayOutputStream expected;
    private CashierView cashierSays;
    private KitchenView kitchenSays;
    private CustomerStatusView customerSays;

    /**
     * Creates a fresh model and a controller with no typed input.
     */
    public void setUp() {
        model = new OrderUpModel();
        expected = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(expected);
        Scanner none = new Scanner("");
        cashierSays = new CashierView(none, printer);
        kitchenSays = new KitchenView(printer);
        customerSays = new CustomerStatusView(none, printer);
        connect("");
    }


    // ----------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------

    /**
     * The constructor rejects a missing model, view, input, or output.
     */
    public void testConstructorRejectsNull() {
        Scanner in = new Scanner("");
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        CashierView cashier = new CashierView(in, out);
        KitchenView kitchen = new KitchenView(out);
        CustomerStatusView customer = new CustomerStatusView(in, out);
        assertRejected(null, cashier, kitchen, customer, in, out);
        assertRejected(model, null, kitchen, customer, in, out);
        assertRejected(model, cashier, null, customer, in, out);
        assertRejected(model, cashier, kitchen, null, in, out);
        assertRejected(model, cashier, kitchen, customer, null, out);
        assertRejected(model, cashier, kitchen, customer, in, null);
    }


    // ----------------------------------------------------------
    // validateQuantity
    // ----------------------------------------------------------

    /**
     * Quantities 1 through 99 are valid, including both ends.
     */
    public void testValidateQuantity() {
        assertFalse(controller.validateQuantity(0));
        assertTrue(controller.validateQuantity(1));
        assertTrue(controller.validateQuantity(50));
        assertTrue(controller.validateQuantity(99));
        assertFalse(controller.validateQuantity(100));
        assertFalse(controller.validateQuantity(-5));
    }


    // ----------------------------------------------------------
    // handleAddItem
    // ----------------------------------------------------------

    /**
     * A valid item number and quantity adds that item to the order.
     */
    public void testAddItem() {
        controller.handleAddItem(1, 2);
        cashierSays.showItemAdded(model.getMenu().get(0), 2);
        assertTrue(shown(said()));
        controller.handleSubmitOrder();
        assertEquals(1, model.waitingCount());
    }


    /**
     * The last item number and the largest quantity are accepted.
     */
    public void testAddItemUpperBounds() {
        int last = model.getMenu().size();
        controller.handleAddItem(last, Order.MAX_QTY);
        cashierSays.showItemAdded(model.getMenu().get(last - 1),
            Order.MAX_QTY);
        assertTrue(shown(said()));
    }


    /**
     * An item number that is not on the menu is an error and adds nothing.
     */
    public void testAddItemBadChoice() {
        controller.handleAddItem(0, 1);
        controller.handleAddItem(-1, 1);
        controller.handleAddItem(model.getMenu().size() + 1, 1);
        assertEquals(3, timesShown(error(OrderUpController.NO_SUCH_ITEM)));
        assertNothingAdded();
    }


    /**
     * A quantity outside 1 through 99 is an error and adds nothing.
     */
    public void testAddItemBadQuantity() {
        controller.handleAddItem(1, 0);
        controller.handleAddItem(1, 100);
        assertEquals(2, timesShown(error(OrderUpController.BAD_QUANTITY)));
        assertNothingAdded();
    }


    /**
     * When both values are bad, only the item number is reported.
     */
    public void testAddItemBadChoiceAndQuantity() {
        controller.handleAddItem(0, 0);
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ITEM)));
        assertEquals(0, timesShown(error(OrderUpController.BAD_QUANTITY)));
    }


    /**
     * An out-of-stock item is an error and adds nothing.
     */
    public void testAddItemOutOfStock() {
        MenuItem item = model.getMenu().get(0);
        item.setInStock(false);
        controller.handleAddItem(1, 1);
        assertEquals(1, timesShown(error(OrderUpController.OUT_OF_STOCK)));
        assertNothingAdded();
        // Put the item back in case the menu is shared between models.
        item.setInStock(true);
    }


    // ----------------------------------------------------------
    // handleNewOrder and handleSubmitOrder
    // ----------------------------------------------------------

    /**
     * Starting a new order throws away the items already added.
     */
    public void testNewOrderDiscardsItems() {
        controller.handleAddItem(1, 1);
        controller.handleNewOrder();
        cashierSays.showNewOrderStarted();
        assertTrue(shown(said()));
        assertNothingAdded();
    }


    /**
     * Submitting an empty order is an error and no order is created.
     */
    public void testSubmitEmptyOrder() {
        controller.handleSubmitOrder();
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(0, model.waitingCount());
        assertNull(model.peekNext());
    }


    /**
     * Submitting shows the ticket number and puts the order in line.
     */
    public void testSubmitOrder() {
        int ticket = placeOrder();
        cashierSays.showOrderPlaced(ticket);
        assertTrue(shown(said()));
        assertEquals(0, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(1, model.waitingCount());
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
    }


    /**
     * A second order gets its own ticket and waits behind the first.
     */
    public void testSubmitSecondOrder() {
        int first = placeOrder();
        int second = placeOrder();
        assertTrue(first != second);
        assertEquals(2, model.waitingCount());
        assertEquals(first, model.peekNext().getTicketNumber());
        assertEquals(2, model.getStatus(second).getPosition());
    }


    /**
     * After a submit the next order starts empty.
     */
    public void testSubmitClearsOrder() {
        placeOrder();
        controller.handleSubmitOrder();
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(1, model.waitingCount());
    }


    // ----------------------------------------------------------
    // handleShowNext and handleCompleteNext
    // ----------------------------------------------------------

    /**
     * Showing the next order with nobody waiting shows the empty line.
     */
    public void testShowNextEmptyLine() {
        controller.handleShowNext();
        kitchenSays.showEmptyLine();
        assertEquals(1, timesShown(said()));
    }


    /**
     * Showing the next order shows the front of the line and the number
     * waiting, and changes nothing.
     */
    public void testShowNext() {
        placeOrder();
        placeOrder();
        Order front = model.peekNext();
        controller.handleShowNext();
        kitchenSays.showNextOrder(front);
        assertTrue(shown(said()));
        kitchenSays.showWaitingCount(2);
        assertTrue(shown(said()));
        assertSame(front, model.peekNext());
        assertEquals(2, model.waitingCount());
    }


    /**
     * Completing with nobody waiting shows the empty line and does not
     * crash.
     */
    public void testCompleteNextEmptyLine() {
        controller.handleCompleteNext();
        kitchenSays.showEmptyLine();
        assertEquals(1, timesShown(said()));
        assertEquals(0, model.waitingCount());
    }


    /**
     * Completing marks the front order ready and shows the one behind it.
     */
    public void testCompleteNext() {
        int first = placeOrder();
        int second = placeOrder();
        Order front = model.peekNext();
        controller.handleCompleteNext();
        assertEquals(OrderStatus.READY, model.getStatus(first).getStatus());
        assertEquals(OrderStatus.WAITING, model.getStatus(second).getStatus());
        kitchenSays.showOrderCompleted(front);
        assertTrue(shown(said()));
        kitchenSays.showNextOrder(model.peekNext());
        assertTrue(shown(said()));
        kitchenSays.showWaitingCount(1);
        assertTrue(shown(said()));
        assertEquals(second, model.peekNext().getTicketNumber());
    }


    /**
     * Completing the last order shows that it is done, then the empty line.
     */
    public void testCompleteLastOrder() {
        int ticket = placeOrder();
        Order front = model.peekNext();
        controller.handleCompleteNext();
        assertEquals(OrderStatus.READY, model.getStatus(ticket).getStatus());
        kitchenSays.showOrderCompleted(front);
        assertTrue(shown(said()));
        kitchenSays.showEmptyLine();
        assertEquals(1, timesShown(said()));
    }


    // ----------------------------------------------------------
    // handleStatusLookup
    // ----------------------------------------------------------

    /**
     * A waiting order shows its place in line.
     */
    public void testStatusLookupWaiting() {
        int first = placeOrder();
        int second = placeOrder();
        controller.handleStatusLookup(first);
        customerSays.showStatus(first, OrderStatus.WAITING, 1);
        assertTrue(shown(said()));
        controller.handleStatusLookup(second);
        customerSays.showStatus(second, OrderStatus.WAITING, 2);
        assertTrue(shown(said()));
    }


    /**
     * A completed order shows as ready.
     */
    public void testStatusLookupReady() {
        int ticket = placeOrder();
        controller.handleCompleteNext();
        controller.handleStatusLookup(ticket);
        customerSays.showStatus(ticket, OrderStatus.READY, model.getStatus(
            ticket).getPosition());
        assertTrue(shown(said()));
    }


    /**
     * A ticket number that was never issued shows as not found.
     */
    public void testStatusLookupNotFound() {
        int ticket = placeOrder();
        controller.handleStatusLookup(ticket + 1000);
        customerSays.showNotFound(ticket + 1000);
        assertEquals(1, timesShown(said()));
        controller.handleStatusLookup(-3);
        customerSays.showNotFound(-3);
        assertEquals(1, timesShown(said()));
    }


    // ----------------------------------------------------------
    // run: main menu
    // ----------------------------------------------------------

    /**
     * Choosing quit ends the loop.
     */
    public void testRunQuit() {
        connect("7\n");
        controller.run();
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * The letter q also quits, in either case.
     */
    public void testRunQuitWithLetter() {
        connect("Q\n1\n");
        controller.run();
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
        cashierSays.showNewOrderStarted();
        assertFalse(shown(said()));
    }


    /**
     * The loop ends by itself when the input runs out.
     */
    public void testRunEndsWhenInputEnds() {
        connect("");
        controller.run();
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * A main menu choice that is not an option is an error, and the menu
     * comes back.
     */
    public void testRunBadOption() {
        connect("9\nabc\n\n7\n");
        controller.run();
        assertEquals(3, timesShown(OrderUpController.BAD_OPTION));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * Spaces around a main menu choice are ignored.
     */
    public void testRunTrimsChoice() {
        connect("  3  \n 7 \n");
        controller.run();
        assertEquals(0, timesShown(OrderUpController.BAD_OPTION));
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
    }


    /**
     * A failure the handlers did not expect is reported and the loop keeps
     * going.
     */
    public void testRunSurvivesFailure() {
        screen = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(screen);
        Scanner in = new Scanner("5\n3\n7\n");
        KitchenView broken = new KitchenView(printer) {
            @Override
            public void showEmptyLine() {
                throw new UnsupportedOperationException("broken view");
            }
        };
        controller = new OrderUpController(model, new CashierView(in,
            printer), broken, new CustomerStatusView(in, printer), in,
            printer);
        controller.run();
        assertEquals(1, timesShown(OrderUpController.UNEXPECTED));
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    // ----------------------------------------------------------
    // run: cashier
    // ----------------------------------------------------------

    /**
     * A cashier can start an order, add an item, and submit it.
     */
    public void testRunPlaceOrder() {
        connect("1\n2\n1\n2\n3\n7\n");
        controller.run();
        cashierSays.showNewOrderStarted();
        assertTrue(shown(said()));
        cashierSays.showMenu(model.getMenu());
        assertTrue(shown(said()));
        cashierSays.showItemAdded(model.getMenu().get(0), 2);
        assertTrue(shown(said()));
        assertEquals(1, model.waitingCount());
        cashierSays.showOrderPlaced(model.peekNext().getTicketNumber());
        assertTrue(shown(said()));
    }


    /**
     * A blank or non-numeric item number is an error and is asked for
     * again.
     */
    public void testRunRepromptsItemNumber() {
        connect("2\nabc\n\n1\n2\n3\n7\n");
        controller.run();
        assertEquals(2, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        cashierSays.showItemAdded(model.getMenu().get(0), 2);
        assertTrue(shown(said()));
        assertEquals(1, model.waitingCount());
    }


    /**
     * An item number that is not on the menu is an error and is asked for
     * again.
     */
    public void testRunRepromptsMissingItem() {
        connect("2\n0\n1\n1\n3\n7\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ITEM)));
        assertEquals(1, model.waitingCount());
    }


    /**
     * A blank, non-numeric, or out-of-range quantity is an error and is
     * asked for again.
     */
    public void testRunRepromptsQuantity() {
        connect("2\n1\ntwo\n0\n5\n3\n7\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(error(OrderUpController.BAD_QUANTITY)));
        cashierSays.showItemAdded(model.getMenu().get(0), 5);
        assertTrue(shown(said()));
        assertEquals(1, model.waitingCount());
    }


    /**
     * After too many bad item numbers the cashier goes back to the main
     * menu with nothing added.
     */
    public void testRunItemNumberGivesUp() {
        connect("2\nx\ny\nz\n3\n7\n");
        controller.run();
        assertEquals(OrderUpController.MAX_TRIES, timesShown(error(
            OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(0, model.waitingCount());
    }


    /**
     * After too many bad quantities the cashier goes back to the main menu
     * with nothing added.
     */
    public void testRunQuantityGivesUp() {
        connect("2\n1\nx\n0\n100\n3\n7\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(2, timesShown(error(OrderUpController.BAD_QUANTITY)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(0, model.waitingCount());
    }


    /**
     * The loop still ends if the input runs out in the middle of a prompt.
     */
    public void testRunInputEndsDuringPrompt() {
        connect("2\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    // ----------------------------------------------------------
    // run: kitchen and customer
    // ----------------------------------------------------------

    /**
     * A cook can look at the next order and complete it from the main
     * menu.
     */
    public void testRunKitchen() {
        int ticket = placeOrder();
        Order front = model.peekNext();
        connect("4\n5\n5\n7\n");
        controller.run();
        kitchenSays.showNextOrder(front);
        assertTrue(shown(said()));
        kitchenSays.showOrderCompleted(front);
        assertTrue(shown(said()));
        kitchenSays.showEmptyLine();
        assertEquals(2, timesShown(said()));
        assertEquals(OrderStatus.READY, model.getStatus(ticket).getStatus());
    }


    /**
     * A blank or non-numeric ticket number is an error and is asked for
     * again.
     */
    public void testRunStatusLookup() {
        int ticket = placeOrder();
        connect("6\nabc\n\n " + ticket + " \n7\n");
        controller.run();
        assertEquals(2, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        customerSays.showStatus(ticket, OrderStatus.WAITING, 1);
        assertEquals(1, timesShown(said()));
    }


    /**
     * A numeric ticket that doesn't exist shows as not found and is not
     * asked for again.
     */
    public void testRunStatusNotFound() {
        connect("6\n999\n7\n");
        controller.run();
        customerSays.showNotFound(999);
        assertEquals(1, timesShown(said()));
        assertEquals(0, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * After too many bad ticket numbers the customer goes back to the main
     * menu.
     */
    public void testRunStatusGivesUp() {
        connect("6\na\nb\nc\n7\n");
        controller.run();
        assertEquals(OrderUpController.MAX_TRIES, timesShown(error(
            OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /**
     * Replaces the controller with one that reads the given typed input
     * and prints to a new, empty screen. The model is kept.
     *
     * @param input
     *            everything the user will type, one entry per line
     */
    private void connect(String input) {
        screen = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(screen);
        Scanner in = new Scanner(input);
        controller = new OrderUpController(model, new CashierView(in,
            printer), new KitchenView(printer), new CustomerStatusView(in,
                printer), in, printer);
    }


    /**
     * Adds one item and submits the order through the controller.
     *
     * @return the new order's ticket number
     */
    private int placeOrder() {
        int before = model.waitingCount();
        controller.handleAddItem(1, 1);
        controller.handleSubmitOrder();
        assertEquals(before + 1, model.waitingCount());
        // A new order is last in line, so find the ticket at that place.
        int ticket = model.peekNext().getTicketNumber();
        while (model.getStatus(ticket).getPosition() != before + 1) {
            ticket++;
        }
        return ticket;
    }


    /**
     * Checks that the order being built is still empty, by showing that it
     * can't be submitted.
     */
    private void assertNothingAdded() {
        int before = timesShown(error(OrderUpController.EMPTY_ORDER));
        controller.handleSubmitOrder();
        assertEquals(before + 1, timesShown(error(
            OrderUpController.EMPTY_ORDER)));
        assertEquals(0, model.waitingCount());
    }


    /**
     * Gets the text a view prints for an error message.
     *
     * @param message
     *            the message handed to the view
     * @return what the view prints for it
     */
    private String error(String message) {
        cashierSays.showError(message);
        return said();
    }


    /**
     * Gets what the second set of views printed since the last call.
     *
     * @return the text a view printed
     */
    private String said() {
        String text = expected.toString();
        expected.reset();
        return text;
    }


    /**
     * Checks whether some text reached the screen.
     *
     * @param text
     *            the text to look for
     * @return true if the text was printed at least once
     */
    private boolean shown(String text) {
        return timesShown(text) > 0;
    }


    /**
     * Counts how many times some text reached the screen.
     *
     * @param text
     *            the text to look for
     * @return how many times the text was printed
     */
    private int timesShown(String text) {
        String all = screen.toString();
        int count = 0;
        int at = all.indexOf(text);
        while (at >= 0) {
            count++;
            at = all.indexOf(text, at + text.length());
        }
        return count;
    }


    /**
     * Checks that the constructor throws for the given arguments.
     *
     * @param m
     *            the model argument
     * @param cashier
     *            the cashier view argument
     * @param kitchen
     *            the kitchen view argument
     * @param customer
     *            the customer status view argument
     * @param in
     *            the input argument
     * @param out
     *            the output argument
     */
    private void assertRejected(
        OrderUpModel m,
        CashierView cashier,
        KitchenView kitchen,
        CustomerStatusView customer,
        Scanner in,
        PrintStream out) {
        Exception thrown = null;
        try {
            new OrderUpController(m, cashier, kitchen, customer, in, out);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
    }
}
