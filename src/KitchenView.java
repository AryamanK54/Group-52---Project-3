import java.io.PrintStream;

/**
 * Console view for the cook: shows the order at the front of the line and how
 * many orders are still waiting. Display only; there is nothing to prompt for
 * because the cook's actions come from the main menu.
 */
public class KitchenView {

    private final PrintStream out;

    /**
     * @param out where to print
     */
    public KitchenView(PrintStream out) {
        this.out = out;
    }

    /**
     * Shows the order the cook should make next. If given null (nothing to
     * show) it shows the empty-line message instead of crashing.
     *
     * @param o the order at the front of the line, or null
     */
    public void showNextOrder(Order o) {
        if (o == null) {
            showEmptyLine();
            return;
        }
        out.println();
        out.println("--- Make this next: Ticket #" + o.getTicketNumber() + " ---");
        for (OrderItem oi : o.getItems()) {
            out.println("  " + oi);
        }
    }

    /**
     * Confirms an order was marked done.
     *
     * @param o the order that was completed
     */
    public void showOrderCompleted(Order o) {
        out.println("Ticket #" + o.getTicketNumber() + " marked READY.");
    }

    /**
     * Shows how many orders are still in line.
     *
     * @param n the number of waiting orders
     */
    public void showWaitingCount(int n) {
        out.println("Orders waiting: " + n);
    }

    /** Tells the cook there is nothing in the line. */
    public void showEmptyLine() {
        out.println("No orders waiting.");
    }

    /**
     * Shows an error message.
     *
     * @param msg the text to show
     */
    public void showError(String msg) {
        out.println("Error: " + msg);
    }
}
