import java.io.PrintStream;
import java.util.Scanner;

/**
 *
 * @author Syed Rishad
 * @version 2026.11.18
 */
public class OrderUpController {

    /** How many bad entries a prompt accepts before giving up. */
    static final int MAX_TRIES = 3;

    /** Shown when typed text is blank or not a whole number. */
    static final String NOT_A_NUMBER = "Please enter a whole number.";

    /** Shown when the item number is not on the menu. */
    static final String NO_SUCH_ITEM = "That item number is not on the menu.";

    /** Shown when the quantity is outside the allowed range. */
    static final String BAD_QUANTITY = "The quantity must be from "
        + Order.MIN_QTY + " to " + Order.MAX_QTY + ".";

    /** Shown when the model refuses an item that passed the other checks. */
    static final String OUT_OF_STOCK = "That item is out of stock.";

    /** Shown when the cashier submits an order with nothing in it. */
    static final String EMPTY_ORDER =
        "Add at least one item before submitting the order.";

    /** Shown when a prompt gives up after too many bad entries. */
    static final String TOO_MANY_TRIES =
        "Too many invalid entries. Going back to the main menu.";

    /** Shown when the main menu choice is not one of the options. */
    static final String BAD_OPTION =
        "Error: Please choose one of the numbered options.";

    /** Shown if something fails that the handlers did not expect. */
    static final String UNEXPECTED =
        "Error: Something went wrong. Please try again.";

    /** Shown when a ticket number to cancel or remake was never issued. */
    static final String NO_SUCH_ORDER =
        "There is no order with that ticket number.";

    /** Shown when the order to cancel is no longer waiting. */
    static final String NOT_WAITING =
        "Only an order that is still waiting can be cancelled.";

    /** Shown when the order to remake has not been completed. */
    static final String NOT_READY =
        "Only an order that is ready can be sent back to be remade.";

    /**
     * Shown after a rush order is placed. The controller prints this itself
     * for now, because CashierView has no method for it yet.
     */
    static final String RUSH_NOTE =
        "Rush order: it goes ahead of the regular orders.";

    /**
     * Shown, followed by the ticket number, after an order is cancelled. The
     * controller prints this itself for now, because CashierView has no
     * method for it yet.
     */
    static final String ORDER_CANCELLED = "Order cancelled. Ticket #";

    /** Shown when the program ends. */
    static final String GOODBYE = "Goodbye!";

    private final OrderUpModel model;
    private final CashierView cashierView;
    private final KitchenView kitchenView;
    private final CustomerStatusView customerView;
    private final Scanner in;
    private final PrintStream out;

    /**
     * Creates a controller for one model and the three views.
     *
     * @param model
     *            the model every handler reads and changes
     * @param cashierView
     *            the cashier's view
     * @param kitchenView
     *            the cook's view
     * @param customerView
     *            the customer status view
     * @param in
     *            the one input source the whole program shares, used here
     *            for the main menu choice
     * @param out
     *            where the main menu is printed
     * @throws IllegalArgumentException
     *             if any argument is null
     */
    public OrderUpController(
        OrderUpModel model,
        CashierView cashierView,
        KitchenView kitchenView,
        CustomerStatusView customerView,
        Scanner in,
        PrintStream out) {
        if (model == null || cashierView == null || kitchenView == null
            || customerView == null || in == null || out == null) {
            throw new IllegalArgumentException(
                "The model, the three views, the input, and the output "
                    + "are all required.");
        }
        this.model = model;
        this.cashierView = cashierView;
        this.kitchenView = kitchenView;
        this.customerView = customerView;
        this.in = in;
        this.out = out;
    }


    /**
     * The main loop. Shows the main menu, runs the handler for the option
     * chosen, and repeats until the user quits or the input runs out. No
     * exception gets out of the loop.
     */
    public void run() {
        boolean running = true;
        while (running) {
            showMainMenu();
            if (in.hasNextLine()) {
                String choice = in.nextLine().trim();
                try {
                    running = dispatch(choice);
                }
                catch (RuntimeException e) {
                    out.println(UNEXPECTED);
                }
            }
            else {
                running = false;
            }
        }
        out.println(GOODBYE);
    }


    // ----------------------------------------------------------
    // Cashier
    // ----------------------------------------------------------

    /**
     * Throws away the order being built and starts a fresh one.
     */
    public void handleNewOrder() {
        model.startNewOrder();
        cashierView.showNewOrderStarted();
    }


    /**
     * Adds an item to the order being built. An item number that is not on
     * the menu, a quantity outside the allowed range, or an out-of-stock
     * item is reported to the cashier and nothing is added.
     *
     * @param choice
     *            the item number as shown on the menu, where the first item
     *            is 1
     * @param qty
     *            how many of the item to add
     */
    public void handleAddItem(int choice, int qty) {
        if (!checkChoice(choice) || !checkQuantity(qty)) {
            return;
        }
        // Hand the model its own MenuItem object. addToOrder looks the item
        // up by identity, so a copy would be rejected.
        MenuItem item = model.getMenu().get(choice - 1);
        try {
            model.addToOrder(item, qty);
        }
        catch (IllegalArgumentException e) {
            // The item number and quantity were already checked, so the
            // only rule left for the model to refuse on is stock.
            cashierView.showError(OUT_OF_STOCK);
            return;
        }
        cashierView.showItemAdded(item, qty);
    }


    /**
     * Submits the order being built as a regular order and shows the
     * cashier the ticket number. An empty order is reported to the cashier
     * and nothing is submitted.
     */
    public void handleSubmitOrder() {
        handleSubmitOrder(false);
    }


    /**
     * Submits the order being built and shows the cashier the ticket
     * number. A rush order is made before the regular orders. An empty
     * order is reported to the cashier and nothing is submitted.
     *
     * @param rush
     *            true to submit the order as a rush order
     */
    public void handleSubmitOrder(boolean rush) {
        int ticket;
        try {
            ticket = model.submitOrder(rush);
        }
        catch (IllegalStateException e) {
            cashierView.showError(EMPTY_ORDER);
            return;
        }
        cashierView.showOrderPlaced(ticket);
        if (rush) {
            out.println(RUSH_NOTE);
        }
    }


    /**
     * Cancels an order that is still waiting. A ticket number that was
     * never issued, or an order that is already ready or cancelled, is
     * reported to the cashier and nothing changes.
     *
     * @param ticket
     *            the ticket number of the order to cancel
     */
    public void handleCancelOrder(int ticket) {
        try {
            model.cancelOrder(ticket);
        }
        catch (IllegalArgumentException e) {
            cashierView.showError(NO_SUCH_ORDER);
            return;
        }
        catch (IllegalStateException e) {
            cashierView.showError(NOT_WAITING);
            return;
        }
        out.println(ORDER_CANCELLED + ticket);
    }


    // ----------------------------------------------------------
    // Kitchen
    // ----------------------------------------------------------

    /**
     * Shows the cook the order at the front of the line and how many orders
     * are waiting, without changing anything.
     */
    public void handleShowNext() {
        showLine();
    }


    /**
     * Marks the order at the front of the line as ready, then shows the cook
     * what is next. If nobody is waiting, the cook is told the line is empty
     * and nothing changes.
     */
    public void handleCompleteNext() {
        if (model.waitingCount() == 0) {
            kitchenView.showEmptyLine();
            return;
        }
        Order done = model.completeNext();
        kitchenView.showOrderCompleted(done);
        showLine();
    }


    /**
     * Sends a finished order back to the front of the line to be made
     * again, then shows the cook the line. A ticket number that was never
     * issued, or an order that is not ready, is reported to the cook and
     * nothing changes.
     *
     * @param ticket
     *            the ticket number of the order to remake
     */
    public void handleRemakeOrder(int ticket) {
        try {
            model.remakeOrder(ticket);
        }
        catch (IllegalArgumentException e) {
            kitchenView.showError(NO_SUCH_ORDER);
            return;
        }
        catch (IllegalStateException e) {
            kitchenView.showError(NOT_READY);
            return;
        }
        showLine();
    }


    // ----------------------------------------------------------
    // Customer
    // ----------------------------------------------------------

    /**
     * Looks up an order and shows the customer its status. A ticket number
     * that was never issued is a normal result, not an error, and is shown
     * as not found.
     *
     * @param ticket
     *            the ticket number to look up
     */
    public void handleStatusLookup(int ticket) {
        StatusInfo info = model.getStatus(ticket);
        if (info.getStatus() == OrderStatus.NOT_FOUND) {
            customerView.showNotFound(ticket);
        }
        else {
            customerView.showStatus(ticket, info.getStatus(), info
                .getPosition());
        }
    }


    // ----------------------------------------------------------
    // Validation
    // ----------------------------------------------------------

    /**
     * Checks a quantity against the range the model allows.
     *
     * @param q
     *            the quantity to check
     * @return true if the quantity is within the allowed range, including
     *         both ends
     */
    public boolean validateQuantity(int q) {
        return q >= Order.MIN_QTY && q <= Order.MAX_QTY;
    }


    // ----------------------------------------------------------
    // Main menu
    // ----------------------------------------------------------

    /**
     * Prints the main menu and the prompt for a choice.
     */
    private void showMainMenu() {
        out.println();
        out.println("=== Order Up! ===");
        out.println("  1) Cashier: start a new order");
        out.println("  2) Cashier: add an item");
        out.println("  3) Cashier: submit the order");
        out.println("  4) Kitchen: show the next order");
        out.println("  5) Kitchen: complete the next order");
        out.println("  6) Customer: check an order");
        out.println("  7) Cashier: submit the order as a rush order");
        out.println("  8) Cashier: cancel an order");
        out.println("  9) Kitchen: remake a finished order");
        out.println("  0) Quit");
        out.print("Choose an option: ");
        out.flush();
    }


    /**
     * Runs the handler for one main menu choice.
     *
     * @param choice
     *            the text typed at the main menu, already trimmed
     * @return false if the user chose to quit, true to keep going
     */
    private boolean dispatch(String choice) {
        boolean keepGoing = true;
        switch (choice.toLowerCase()) {
            case "1":
                handleNewOrder();
                break;
            case "2":
                addItemFromPrompts();
                break;
            case "3":
                handleSubmitOrder();
                break;
            case "4":
                handleShowNext();
                break;
            case "5":
                handleCompleteNext();
                break;
            case "6":
                lookUpFromPrompt();
                break;
            case "7":
                handleSubmitOrder(true);
                break;
            case "8":
                cancelFromPrompt();
                break;
            case "9":
                remakeFromPrompt();
                break;
            case "0":
            case "q":
                keepGoing = false;
                break;
            default:
                out.println(BAD_OPTION);
                break;
        }
        return keepGoing;
    }


    /**
     * Shows the menu, asks the cashier for an item number and a quantity,
     * and adds the item. Each prompt is repeated after a bad entry. If
     * either prompt gives up, nothing is added.
     */
    private void addItemFromPrompts() {
        cashierView.showMenu(model.getMenu());
        Integer choice = promptForChoice();
        if (choice == null) {
            return;
        }
        Integer qty = promptForQuantity();
        if (qty == null) {
            return;
        }
        handleAddItem(choice.intValue(), qty.intValue());
    }


    /**
     * Asks the customer for a ticket number and shows that order's status.
     */
    private void lookUpFromPrompt() {
        Integer ticket = promptForTicket();
        if (ticket != null) {
            handleStatusLookup(ticket.intValue());
        }
    }


    /**
     * Asks the cashier for a ticket number and cancels that order.
     */
    private void cancelFromPrompt() {
        Integer ticket = promptForTicket();
        if (ticket != null) {
            handleCancelOrder(ticket.intValue());
        }
    }


    /**
     * Asks the cook for a ticket number and sends that order back to be
     * made again.
     */
    private void remakeFromPrompt() {
        Integer ticket = promptForTicket();
        if (ticket != null) {
            handleRemakeOrder(ticket.intValue());
        }
    }


    /**
     * Asks for a ticket number until a whole number is entered. The prompt
     * is the customer view's, which is the only view that asks for a
     * ticket, so the cashier and the cook share it for now.
     *
     * @return the ticket number, or null if the prompt gave up
     */
    private Integer promptForTicket() {
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            Integer ticket = parseWholeNumber(customerView.promptTicket());
            if (ticket != null) {
                return ticket;
            }
            customerView.showError(NOT_A_NUMBER);
        }
        customerView.showError(TOO_MANY_TRIES);
        return null;
    }


    /**
     * Asks the cashier for an item number until one on the menu is entered.
     *
     * @return the item number, or null if the prompt gave up
     */
    private Integer promptForChoice() {
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            Integer choice = parseWholeNumber(cashierView.promptItemChoice());
            if (choice == null) {
                cashierView.showError(NOT_A_NUMBER);
            }
            else if (checkChoice(choice.intValue())) {
                return choice;
            }
        }
        cashierView.showError(TOO_MANY_TRIES);
        return null;
    }


    /**
     * Asks the cashier for a quantity until one in the allowed range is
     * entered.
     *
     * @return the quantity, or null if the prompt gave up
     */
    private Integer promptForQuantity() {
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            Integer qty = parseWholeNumber(cashierView.promptQuantity());
            if (qty == null) {
                cashierView.showError(NOT_A_NUMBER);
            }
            else if (checkQuantity(qty.intValue())) {
                return qty;
            }
        }
        cashierView.showError(TOO_MANY_TRIES);
        return null;
    }


    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /**
     * Checks that an item number is on the menu, telling the cashier if it
     * is not.
     *
     * @param choice
     *            the item number, where the first item is 1
     * @return true if the item number is on the menu
     */
    private boolean checkChoice(int choice) {
        boolean onMenu = choice >= 1 && choice <= model.getMenu().size();
        if (!onMenu) {
            cashierView.showError(NO_SUCH_ITEM);
        }
        return onMenu;
    }


    /**
     * Checks that a quantity is in the allowed range, telling the cashier
     * if it is not.
     *
     * @param qty
     *            the quantity to check
     * @return true if the quantity is allowed
     */
    private boolean checkQuantity(int qty) {
        boolean allowed = validateQuantity(qty);
        if (!allowed) {
            cashierView.showError(BAD_QUANTITY);
        }
        return allowed;
    }


    /**
     * Shows the cook the front of the line and the number waiting, or the
     * empty line if nobody is waiting.
     */
    private void showLine() {
        Order next = model.peekNext();
        if (next == null) {
            kitchenView.showEmptyLine();
        }
        else {
            kitchenView.showNextOrder(next);
            kitchenView.showWaitingCount(model.waitingCount());
        }
    }


    /**
     * Reads a whole number from typed text, ignoring spaces around it.
     *
     * @param text
     *            the text to read
     * @return the number, or null if the text is blank or is not a whole
     *         number that fits in an int
     */
    private static Integer parseWholeNumber(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
