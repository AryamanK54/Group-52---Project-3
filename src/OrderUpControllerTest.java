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
 * @author Aryaman Kapoor (rush prompt, view current order, typed-text
 *         handlers, MainMenuView wiring)
 * @version 2026.11.18
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
     * The constructor rejects a missing model or view.
     */
    public void testConstructorRejectsNull() {
        Scanner in = new Scanner("");
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        CashierView cashier = new CashierView(in, out);
        KitchenView kitchen = new KitchenView(out);
        CustomerStatusView customer = new CustomerStatusView(in, out);
        MainMenuView menu = new MainMenuView(in, out);
        assertRejected(null, cashier, kitchen, customer, menu);
        assertRejected(model, null, kitchen, customer, menu);
        assertRejected(model, cashier, null, customer, menu);
        assertRejected(model, cashier, kitchen, null, menu);
        assertRejected(model, cashier, kitchen, customer, null);
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
    // handleSubmitOrder: rush orders
    // ----------------------------------------------------------

    /**
     * A rush order shows its ticket and the rush note, and goes ahead of
     * the regular orders already waiting.
     */
    public void testSubmitRushOrder() {
        int first = placeOrder();
        int second = placeOrder();
        int rush = placeRushOrder();
        assertEquals(1, timesShown(rushPlaced(rush)));
        cashierSays.showOrderPlaced(rush);
        assertFalse(shown(said()));
        assertEquals(3, model.waitingCount());
        assertEquals(rush, model.peekNext().getTicketNumber());
        assertTrue(model.peekNext().isRush());
        assertEquals(2, model.getStatus(first).getPosition());
        assertEquals(3, model.getStatus(second).getPosition());
    }


    /**
     * A submit with rush set to false is a regular order with no rush
     * note.
     */
    public void testSubmitNotRush() {
        int first = placeOrder();
        controller.handleAddItem(1, 1);
        controller.handleSubmitOrder(false);
        cashierSays.showOrderPlaced(first + 1);
        assertTrue(shown(said()));
        assertEquals(0, timesShown(rushPlaced(first + 1)));
        assertEquals(2, model.waitingCount());
        assertEquals(first, model.peekNext().getTicketNumber());
        assertFalse(model.peekNext().isRush());
    }


    /**
     * Submitting an empty rush order is an error and no order is created.
     */
    public void testSubmitEmptyRushOrder() {
        controller.handleSubmitOrder(true);
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(0, model.waitingCount());
    }


    // ----------------------------------------------------------
    // handleCancelOrder
    // ----------------------------------------------------------

    /**
     * Cancelling a waiting order confirms it and takes it out of the line.
     */
    public void testCancelOrder() {
        int first = placeOrder();
        int second = placeOrder();
        controller.handleCancelOrder(first);
        assertEquals(1, timesShown(cancelled(first)));
        assertEquals(OrderStatus.CANCELLED, model.getStatus(first)
            .getStatus());
        assertEquals(1, model.waitingCount());
        assertEquals(second, model.peekNext().getTicketNumber());
    }


    /**
     * Cancelling a ticket that was never issued is an error and changes
     * nothing.
     */
    public void testCancelUnknownTicket() {
        int ticket = placeOrder();
        controller.handleCancelOrder(ticket + 1000);
        controller.handleCancelOrder(-3);
        assertEquals(2, timesShown(error(OrderUpController.NO_SUCH_ORDER)));
        assertEquals(0, timesShown(cancelled(ticket + 1000)));
        assertEquals(0, timesShown(cancelled(-3)));
        assertEquals(1, model.waitingCount());
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
    }


    /**
     * An order that is already ready can't be cancelled.
     */
    public void testCancelReadyOrder() {
        int ticket = placeOrder();
        controller.handleCompleteNext();
        controller.handleCancelOrder(ticket);
        assertEquals(1, timesShown(error(OrderUpController.NOT_WAITING)));
        assertEquals(0, timesShown(cancelled(ticket)));
        assertEquals(OrderStatus.READY, model.getStatus(ticket).getStatus());
    }


    /**
     * An order can only be cancelled once.
     */
    public void testCancelTwice() {
        int ticket = placeOrder();
        controller.handleCancelOrder(ticket);
        controller.handleCancelOrder(ticket);
        assertEquals(1, timesShown(cancelled(ticket)));
        assertEquals(1, timesShown(error(OrderUpController.NOT_WAITING)));
        assertEquals(OrderStatus.CANCELLED, model.getStatus(ticket)
            .getStatus());
    }


    /**
     * The cook is never shown a cancelled order.
     */
    public void testCancelledOrderIsSkipped() {
        int first = placeOrder();
        int second = placeOrder();
        controller.handleCancelOrder(first);
        controller.handleCompleteNext();
        assertEquals(OrderStatus.READY, model.getStatus(second).getStatus());
        assertEquals(OrderStatus.CANCELLED, model.getStatus(first)
            .getStatus());
        kitchenSays.showEmptyLine();
        assertEquals(1, timesShown(said()));
    }


    // ----------------------------------------------------------
    // handleRemakeOrder
    // ----------------------------------------------------------

    /**
     * Remaking a finished order puts it back at the front of the line and
     * shows the cook the line.
     */
    public void testRemakeOrder() {
        int first = placeOrder();
        int second = placeOrder();
        Order remade = model.peekNext();
        controller.handleCompleteNext();
        controller.handleRemakeOrder(first);
        assertEquals(OrderStatus.WAITING, model.getStatus(first).getStatus());
        assertEquals(1, model.getStatus(first).getPosition());
        assertEquals(2, model.getStatus(second).getPosition());
        kitchenSays.showNextOrder(remade);
        assertTrue(shown(said()));
        kitchenSays.showWaitingCount(2);
        assertTrue(shown(said()));
    }


    /**
     * Remaking a ticket that was never issued is an error and changes
     * nothing.
     */
    public void testRemakeUnknownTicket() {
        int ticket = placeOrder();
        controller.handleCompleteNext();
        controller.handleRemakeOrder(ticket + 1000);
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ORDER)));
        assertEquals(0, model.waitingCount());
        assertEquals(OrderStatus.READY, model.getStatus(ticket).getStatus());
    }


    /**
     * An order that is still waiting or was cancelled can't be remade.
     */
    public void testRemakeNotReady() {
        int waiting = placeOrder();
        int cancelled = placeOrder();
        controller.handleCancelOrder(cancelled);
        controller.handleRemakeOrder(waiting);
        controller.handleRemakeOrder(cancelled);
        assertEquals(2, timesShown(error(OrderUpController.NOT_READY)));
        assertEquals(1, model.waitingCount());
        assertEquals(1, model.getStatus(waiting).getPosition());
        assertEquals(OrderStatus.CANCELLED, model.getStatus(cancelled)
            .getStatus());
    }


    /**
     * A rush order waits behind a remake but ahead of the regular orders.
     */
    public void testRushWaitsBehindRemake() {
        int remade = placeOrder();
        int regular = placeOrder();
        controller.handleCompleteNext();
        controller.handleRemakeOrder(remade);
        int rush = placeRushOrder();
        assertEquals(1, model.getStatus(remade).getPosition());
        assertEquals(2, model.getStatus(rush).getPosition());
        assertEquals(3, model.getStatus(regular).getPosition());
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
     * A cancelled order shows as cancelled, not as not found.
     */
    public void testStatusLookupCancelled() {
        int ticket = placeOrder();
        controller.handleCancelOrder(ticket);
        controller.handleStatusLookup(ticket);
        customerSays.showStatus(ticket, OrderStatus.CANCELLED,
            StatusInfo.NOT_IN_LINE);
        assertEquals(1, timesShown(said()));
        customerSays.showNotFound(ticket);
        assertEquals(0, timesShown(said()));
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
        connect("0\n1\n");
        controller.run();
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
        cashierSays.showNewOrderStarted();
        assertFalse(shown(said()));
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
        connect("10\nabc\n\n0\n");
        controller.run();
        assertEquals(3, timesShown(OrderUpController.BAD_OPTION));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * Spaces around a main menu choice are ignored.
     */
    public void testRunTrimsChoice() {
        connect("  3  \n 0 \n");
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
        Scanner in = new Scanner("5\n3\n0\n");
        KitchenView broken = new KitchenView(printer) {
            @Override
            public void showEmptyLine() {
                throw new UnsupportedOperationException("broken view");
            }
        };
        controller = new OrderUpController(model, new CashierView(in,
            printer), broken, new CustomerStatusView(in, printer),
            new MainMenuView(in, printer));
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
        connect("1\n2\n1\n2\n3\nn\n0\n");
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
        connect("2\nabc\n\n1\n2\n3\nn\n0\n");
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
        connect("2\n0\n1\n1\n3\nn\n0\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ITEM)));
        assertEquals(1, model.waitingCount());
    }


    /**
     * A blank, non-numeric, or out-of-range quantity is an error and is
     * asked for again.
     */
    public void testRunRepromptsQuantity() {
        connect("2\n1\ntwo\n0\n5\n3\nn\n0\n");
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
        connect("2\nx\ny\nz\n3\n0\n");
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
        connect("2\n1\nx\n0\n100\n3\n0\n");
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
        connect("4\n5\n5\n0\n");
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
        connect("6\nabc\n\n " + ticket + " \n0\n");
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
        connect("6\n999\n0\n");
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
        connect("6\na\nb\nc\n0\n");
        controller.run();
        assertEquals(OrderUpController.MAX_TRIES, timesShown(error(
            OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    // ----------------------------------------------------------
    // run: rush, cancel, and remake
    // ----------------------------------------------------------

    /**
     * Answering y to the rush question submits a rush order, which goes
     * ahead of the regular order already waiting.
     */
    public void testRunRushOrder() {
        int regular = placeOrder();
        connect("2\n1\n1\n3\ny\n0\n");
        controller.run();
        assertEquals(2, model.waitingCount());
        assertTrue(model.peekNext().isRush());
        assertEquals(1, timesShown(rushPlaced(model.peekNext()
            .getTicketNumber())));
        assertEquals(2, model.getStatus(regular).getPosition());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * "yes" in any case and with spaces around it also means rush.
     */
    public void testRunRushOrderYesWord() {
        connect("2\n1\n1\n3\n  YeS  \n0\n");
        controller.run();
        assertEquals(1, model.waitingCount());
        assertTrue(model.peekNext().isRush());
        assertEquals(0, timesShown(error(OrderUpController.NOT_YES_NO)));
    }


    /**
     * Answering n submits a regular order that waits behind the orders
     * already in line.
     */
    public void testRunRushAnswerNo() {
        int regular = placeOrder();
        connect("2\n1\n1\n3\nn\n0\n");
        controller.run();
        assertEquals(2, model.waitingCount());
        assertEquals(regular, model.peekNext().getTicketNumber());
        assertFalse(model.peekNext().isRush());
        cashierSays.showOrderPlaced(regular + 1);
        assertTrue(shown(said()));
        assertEquals(2, model.getStatus(regular + 1).getPosition());
    }


    /**
     * "no" in capitals also means a regular order.
     */
    public void testRunRushAnswerNoWord() {
        connect("2\n1\n1\n3\nNO\n0\n");
        controller.run();
        assertEquals(1, model.waitingCount());
        assertFalse(model.peekNext().isRush());
    }


    /**
     * A blank answer or "maybe" is an error and the question is asked
     * again; a later y still submits a rush order.
     */
    public void testRunRushRepromptsBlankAndMaybe() {
        connect("2\n1\n1\n3\n\nmaybe\ny\n0\n");
        controller.run();
        assertEquals(2, timesShown(error(OrderUpController.NOT_YES_NO)));
        assertEquals(1, model.waitingCount());
        assertTrue(model.peekNext().isRush());
    }


    /**
     * After three bad answers nothing is submitted, but the items stay on
     * the order so it can be submitted later.
     */
    public void testRunRushGivesUp() {
        connect("2\n1\n1\n3\nmaybe\n\nsure\n3\nn\n0\n");
        controller.run();
        assertEquals(OrderUpController.MAX_TRIES, timesShown(error(
            OrderUpController.NOT_YES_NO)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(1, model.waitingCount());
        assertFalse(model.peekNext().isRush());
    }


    /**
     * If the input runs out at the rush question, nothing is submitted and
     * the program ends without crashing.
     */
    public void testRunInputEndsAtRushPrompt() {
        connect("2\n1\n1\n3\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(0, model.waitingCount());
        assertEquals(1, model.getCurrentItems().size());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * Submitting with nothing in the order is an error and the rush
     * question is never asked.
     */
    public void testRunEmptyOrderSkipsRushQuestion() {
        connect("3\ny\n0\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.EMPTY_ORDER)));
        assertEquals(0, timesShown(error(OrderUpController.NOT_YES_NO)));
        // The "y" is read as a main menu choice, which is not an option.
        assertEquals(1, timesShown(OrderUpController.BAD_OPTION));
        assertEquals(0, model.waitingCount());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * Option 7 shows the order being built, and it is empty again after
     * the order is submitted.
     */
    public void testRunViewCurrentOrder() {
        connect("7\n2\n1\n2\n7\n3\nn\n7\n0\n");
        controller.run();
        cashierSays.showCurrentOrder(new java.util.ArrayList<OrderItem>());
        assertEquals(2, timesShown(said()));
        java.util.List<OrderItem> one = new java.util.ArrayList<OrderItem>();
        one.add(new OrderItem(model.getMenu().get(0), 2));
        cashierSays.showCurrentOrder(one);
        assertEquals(1, timesShown(said()));
    }


    /**
     * run() shows the main menu through MainMenuView.
     */
    public void testRunUsesMainMenuView() {
        connect("0\n");
        controller.run();
        ByteArrayOutputStream menuText = new ByteArrayOutputStream();
        new MainMenuView(new Scanner(""), new PrintStream(menuText))
            .showMainMenu();
        assertEquals(1, timesShown(menuText.toString()));
    }


    /**
     * A cashier can cancel an order from the main menu. A blank or
     * non-numeric ticket number is an error and is asked for again.
     */
    public void testRunCancel() {
        int ticket = placeOrder();
        connect("8\nabc\n\n " + ticket + " \n0\n");
        controller.run();
        assertEquals(2, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(cancelled(ticket)));
        assertEquals(OrderStatus.CANCELLED, model.getStatus(ticket)
            .getStatus());
        assertEquals(0, model.waitingCount());
    }


    /**
     * A numeric ticket that doesn't exist is an error and is not asked for
     * again.
     */
    public void testRunCancelUnknownTicket() {
        int ticket = placeOrder();
        connect("8\n999\n0\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ORDER)));
        assertEquals(0, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * After too many bad ticket numbers the cashier goes back to the main
     * menu and nothing is cancelled.
     */
    public void testRunCancelGivesUp() {
        int ticket = placeOrder();
        connect("8\na\nb\nc\n0\n");
        controller.run();
        assertEquals(OrderUpController.MAX_TRIES, timesShown(error(
            OrderUpController.NOT_A_NUMBER)));
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * A cook can complete an order and then send it back to be remade from
     * the main menu.
     */
    public void testRunRemake() {
        int first = placeOrder();
        int second = placeOrder();
        connect("5\n9\n" + first + "\n0\n");
        controller.run();
        assertEquals(OrderStatus.WAITING, model.getStatus(first).getStatus());
        assertEquals(1, model.getStatus(first).getPosition());
        assertEquals(2, model.getStatus(second).getPosition());
        kitchenSays.showWaitingCount(2);
        assertTrue(shown(said()));
    }


    /**
     * Remaking an order that is still waiting is an error from the main
     * menu too, and the loop keeps going.
     */
    public void testRunRemakeNotReady() {
        int ticket = placeOrder();
        connect("9\n" + ticket + "\n9\n999\n0\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.NOT_READY)));
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ORDER)));
        assertEquals(1, model.getStatus(ticket).getPosition());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    /**
     * The input can run out in the middle of the cancel prompt without
     * hanging or crashing.
     */
    public void testRunInputEndsDuringCancel() {
        int ticket = placeOrder();
        connect("8\n");
        controller.run();
        assertEquals(1, timesShown(error(OrderUpController.TOO_MANY_TRIES)));
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
        assertEquals(1, timesShown(OrderUpController.GOODBYE));
    }


    // ----------------------------------------------------------
    // handleShowMenu and handleViewCurrentOrder
    // ----------------------------------------------------------

    /**
     * Showing the menu shows every item on it.
     */
    public void testShowMenu() {
        controller.handleShowMenu();
        cashierSays.showMenu(model.getMenu());
        assertEquals(1, timesShown(said()));
    }


    /**
     * The current order starts empty.
     */
    public void testViewCurrentOrderEmpty() {
        controller.handleViewCurrentOrder();
        cashierSays.showCurrentOrder(model.getCurrentItems());
        assertEquals(1, timesShown(said()));
        assertTrue(model.getCurrentItems().isEmpty());
    }


    /**
     * The current order shows the items added so far, in order.
     */
    public void testViewCurrentOrderWithItems() {
        controller.handleAddItem(1, 2);
        controller.handleAddItem(2, 1);
        controller.handleViewCurrentOrder();
        cashierSays.showCurrentOrder(model.getCurrentItems());
        assertEquals(1, timesShown(said()));
        assertEquals(2, model.getCurrentItems().size());
    }


    // ----------------------------------------------------------
    // Handlers that take typed text (used by the JavaFX screens)
    // ----------------------------------------------------------

    /**
     * Typed numbers, with spaces around them, add the item.
     */
    public void testAddItemText() {
        controller.handleAddItem(" 1 ", "2");
        cashierSays.showItemAdded(model.getMenu().get(0), 2);
        assertTrue(shown(said()));
        assertEquals(1, model.getCurrentItems().size());
    }


    /**
     * Blank, missing, or non-numeric text is an error and adds nothing.
     */
    public void testAddItemTextNotANumber() {
        controller.handleAddItem("", "1");
        controller.handleAddItem("1", "two");
        controller.handleAddItem(null, "1");
        controller.handleAddItem("1", null);
        assertEquals(4, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertTrue(model.getCurrentItems().isEmpty());
    }


    /**
     * Typed text that is a number still gets the item and quantity checks.
     */
    public void testAddItemTextOutOfRange() {
        controller.handleAddItem("99", "1");
        controller.handleAddItem("1", "100");
        assertEquals(1, timesShown(error(OrderUpController.NO_SUCH_ITEM)));
        assertEquals(1, timesShown(error(OrderUpController.BAD_QUANTITY)));
        assertTrue(model.getCurrentItems().isEmpty());
    }


    /**
     * A typed ticket number shows that order's status.
     */
    public void testStatusLookupText() {
        int ticket = placeOrder();
        controller.handleStatusLookup(" " + ticket + " ");
        customerSays.showStatus(ticket, OrderStatus.WAITING, 1);
        assertEquals(1, timesShown(said()));
    }


    /**
     * A blank or non-numeric ticket is an error for the customer.
     */
    public void testStatusLookupTextNotANumber() {
        controller.handleStatusLookup("");
        controller.handleStatusLookup("abc");
        customerSays.showError(OrderUpController.NOT_A_NUMBER);
        assertEquals(2, timesShown(said()));
    }


    /**
     * A typed ticket number cancels that order; bad text changes nothing.
     */
    public void testCancelOrderText() {
        int ticket = placeOrder();
        controller.handleCancelOrder("x");
        assertEquals(1, timesShown(error(OrderUpController.NOT_A_NUMBER)));
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
        controller.handleCancelOrder(String.valueOf(ticket));
        assertEquals(1, timesShown(cancelled(ticket)));
        assertEquals(OrderStatus.CANCELLED, model.getStatus(ticket)
            .getStatus());
    }


    /**
     * A typed ticket number remakes that order; bad text is an error for
     * the cook and changes nothing.
     */
    public void testRemakeOrderText() {
        int ticket = placeOrder();
        controller.handleCompleteNext();
        controller.handleRemakeOrder("");
        kitchenSays.showError(OrderUpController.NOT_A_NUMBER);
        assertEquals(1, timesShown(said()));
        assertEquals(OrderStatus.READY, model.getStatus(ticket).getStatus());
        controller.handleRemakeOrder(String.valueOf(ticket));
        assertEquals(OrderStatus.WAITING, model.getStatus(ticket).getStatus());
        assertEquals(1, model.getStatus(ticket).getPosition());
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
                printer), new MainMenuView(in, printer));
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
     * Adds one item and submits the order as a rush order through the
     * controller.
     *
     * @return the new order's ticket number
     */
    private int placeRushOrder() {
        controller.handleAddItem(1, 1);
        controller.handleSubmitOrder(true);
        // Tickets go up by one, so the newest order has the highest one.
        int ticket = OrderBook.FIRST_TICKET;
        while (model.getStatus(ticket + 1)
            .getStatus() != OrderStatus.NOT_FOUND) {
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
     * Gets the text the cashier view prints when a rush order is placed.
     *
     * @param ticket
     *            the rush order's ticket number
     * @return what the view prints for it
     */
    private String rushPlaced(int ticket) {
        cashierSays.showRushOrderPlaced(ticket);
        return said();
    }


    /**
     * Gets the text the cashier view prints when an order is cancelled.
     *
     * @param ticket
     *            the cancelled order's ticket number
     * @return what the view prints for it
     */
    private String cancelled(int ticket) {
        cashierSays.showOrderCancelled(ticket);
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
     * @param menu
     *            the main menu view argument
     */
    private void assertRejected(
        OrderUpModel m,
        CashierView cashier,
        KitchenView kitchen,
        CustomerStatusView customer,
        MainMenuView menu) {
        Exception thrown = null;
        try {
            new OrderUpController(m, cashier, kitchen, customer, menu);
        }
        catch (IllegalArgumentException e) {
            thrown = e;
        }
        assertNotNull(thrown);
    }
}
