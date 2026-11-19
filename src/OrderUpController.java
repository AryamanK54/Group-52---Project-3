/**
 * The only controller in Order Up!. It takes what the user typed or clicked,
 * checks that it is well formed, calls the model, and tells the right view
 * what to show. It never prints or reads anything itself: the console main
 * menu goes through MainMenuView, and every other message goes through the
 * cashier, kitchen, or customer view. The JavaFX screens call the same
 * handle... methods as the console menu.
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
        "Please choose one of the numbered options.";

    /** Shown if something fails that the handlers did not expect. */
    static final String UNEXPECTED =
        "Something went wrong. Please try again.";

    /** Shown when the answer to the rush question is not yes or no. */
    static final String NOT_YES_NO = "Please answer y or n.";

    /** Shown when a ticket number to cancel or remake was never issued. */
    static final String NO_SUCH_ORDER =
        "There is no order with that ticket number.";

    /** Shown when the order to cancel is no longer waiting. */
    static final String NOT_WAITING =
        "Only an order that is still waiting can be cancelled.";

    /** Shown when the order to remake has not been completed. */
    static final String NOT_READY =
        "Only an order that is ready can be sent back to be remade.";

    /** Shown when the program ends. */
    static final String GOODBYE = "Goodbye!";

    private final OrderUpModel model;
    private final CashierView cashierView;
    private final KitchenView kitchenView;
    private final CustomerStatusView customerView;
    private final MainMenuView mainMenuView;

    /**
     * Creates a controller for one model and the four views.
     *
     * @param model
     *            the model every handler reads and changes
     * @param cashierView
     *            the cashier's view
     * @param kitchenView
     *            the cook's view
     * @param customerView
     *            the customer status view
     * @param mainMenuView
     *            the console main menu, used only by run()
     * @throws IllegalArgumentException
     *             if any argument is null
     */
    public OrderUpController(
        OrderUpModel model,
        CashierView cashierView,
        KitchenView kitchenView,
        CustomerStatusView customerView,
        MainMenuView mainMenuView) {
        if (model == null || cashierView == null || kitchenView == null
            || customerView == null || mainMenuView == null) {
            throw new IllegalArgumentException(
                "The model and all four views are required.");
        }
        this.model = model;
        this.cashierView = cashierView;
        this.kitchenView = kitchenView;
        this.customerView = customerView;
        this.mainMenuView = mainMenuView;
    }


    /**
     * The main loop. Shows the main menu, runs the handler for the option
     * chosen, and repeats until the user quits or the input runs out. No
     * exception gets out of the loop.
     */
    public void run() {
        boolean running = true;
        while (running) {
            mainMenuView.showMainMenu();
            // promptChoice returns "0" (quit) once the input runs out.
            String choice = mainMenuView.promptChoice();
            try {
                running = dispatch(choice);
            }
            catch (RuntimeException e) {
                mainMenuView.showError(UNEXPECTED);
            }
        }
        mainMenuView.showGoodbye();
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
     * Shows the cashier the menu.
     */
    public void handleShowMenu() {
        cashierView.showMenu(model.getMenu());
    }


    /**
     * Shows the cashier the items on the order being built so far.
     */
    public void handleViewCurrentOrder() {
        cashierView.showCurrentOrder(model.getCurrentItems());
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
     * Adds an item to the order being built from typed text, as the JavaFX
     * cashier screen sends it. Text that is blank or not a whole number is
     * reported to the cashier and nothing is added; otherwise this works like
     * handleAddItem(int, int).
     *
     * @param choiceText
     *            the item number as typed, where the first item is 1
     * @param qtyText
     *            the quantity as typed
     */
    public void handleAddItem(String choiceText, String qtyText) {
        Integer choice = parseWholeNumber(choiceText);
        Integer qty = parseWholeNumber(qtyText);
        if (choice == null || qty == null) {
            cashierView.showError(NOT_A_NUMBER);
            return;
        }
        handleAddItem(choice.intValue(), qty.intValue());
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
        if (rush) {
            cashierView.showRushOrderPlaced(ticket);
        }
        else {
            cashierView.showOrderPlaced(ticket);
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
        cashierView.showOrderCancelled(ticket);
    }


    /**
     * Cancels an order from a typed ticket number, as the JavaFX cashier
     * screen sends it. Text that is blank or not a whole number is reported
     * to the cashier; otherwise this works like handleCancelOrder(int).
     *
     * @param ticketText
     *            the ticket number as typed
     */
    public void handleCancelOrder(String ticketText) {
        Integer ticket = parseWholeNumber(ticketText);
        if (ticket == null) {
            cashierView.showError(NOT_A_NUMBER);
            return;
        }
        handleCancelOrder(ticket.intValue());
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


    /**
     * Sends a finished order back to be remade from a typed ticket number,
     * as the JavaFX kitchen screen sends it. Text that is blank or not a
     * whole number is reported to the cook; otherwise this works like
     * handleRemakeOrder(int).
     *
     * @param ticketText
     *            the ticket number as typed
     */
    public void handleRemakeOrder(String ticketText) {
        Integer ticket = parseWholeNumber(ticketText);
        if (ticket == null) {
            kitchenView.showError(NOT_A_NUMBER);
            return;
        }
        handleRemakeOrder(ticket.intValue());
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


    /**
     * Looks up an order from a typed ticket number, as the JavaFX customer
     * screen sends it. Text that is blank or not a whole number is reported
     * to the customer; otherwise this works like handleStatusLookup(int).
     *
     * @param ticketText
     *            the ticket number as typed
     */
    public void handleStatusLookup(String ticketText) {
        Integer ticket = parseWholeNumber(ticketText);
        if (ticket == null) {
            customerView.showError(NOT_A_NUMBER);
            return;
        }
        handleStatusLookup(ticket.intValue());
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
                submitFromPrompt();
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
                handleViewCurrentOrder();
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
                mainMenuView.showError(BAD_OPTION);
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
     * Asks the cashier whether the order being built is a rush order, then
     * submits it. An empty order is reported without asking. If the rush
     * prompt gives up, nothing is submitted and the items stay on the order.
     */
    private void submitFromPrompt() {
        if (model.getCurrentItems().isEmpty()) {
            cashierView.showError(EMPTY_ORDER);
            return;
        }
        Boolean rush = promptForRush();
        if (rush != null) {
            handleSubmitOrder(rush.booleanValue());
        }
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
     * Asks the cashier "rush order?" until y, yes, n, or no is entered, in
     * any case and ignoring spaces around it.
     *
     * @return true for yes, false for no, or null if the prompt gave up
     */
    private Boolean promptForRush() {
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            String answer = cashierView.promptRush().trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return Boolean.TRUE;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return Boolean.FALSE;
            }
            cashierView.showError(NOT_YES_NO);
        }
        cashierView.showError(TOO_MANY_TRIES);
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
