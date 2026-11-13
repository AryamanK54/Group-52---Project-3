import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Console view for the cashier: shows the menu and confirmations and prompts
 * for choices. Display and prompting only.
 * <p>
 * The prompt methods return exactly what the cashier typed (trimmed). They do
 * not parse or validate it: turning text into numbers and deciding whether it
 * is acceptable is the controller's job.
 */
public class CashierView {

    private final Scanner in;
    private final PrintStream out;

    /**
     * @param in  the shared input source (one Scanner for the whole program)
     * @param out where to print
     */
    public CashierView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Prints the menu as a numbered list (starting at 1). Out-of-stock items
     * are marked.
     *
     * @param items the menu items to display
     */
    public void showMenu(List<MenuItem> items) {
        out.println();
        out.println("--- Menu ---");
        for (int i = 0; i < items.size(); i++) {
            MenuItem m = items.get(i);
            out.println("  " + (i + 1) + ") " + m.getName()
                    + "  $" + String.format("%.2f", m.getPrice())
                    + (m.isInStock() ? "" : "  (OUT OF STOCK)"));
        }
    }

    /** @return the raw text the cashier typed as a menu item number; never null */
    public String promptItemChoice() {
        return readLine("Item number: ");
    }

    /** @return the raw text the cashier typed as a quantity; never null */
    public String promptQuantity() {
        return readLine("Quantity: ");
    }

    /** Confirms that a fresh order was started. */
    public void showNewOrderStarted() {
        out.println("New order started.");
    }

    /**
     * Confirms an item was added to the current order.
     *
     * @param item the item added
     * @param qty  the quantity added
     */
    public void showItemAdded(MenuItem item, int qty) {
        out.println("Added " + qty + " x " + item.getName() + ".");
    }

    /**
     * Confirms an order was submitted.
     *
     * @param ticket the ticket number given to the order
     */
    public void showOrderPlaced(int ticket) {
        out.println("Order placed! Ticket #" + ticket);
    }

    /**
     * Shows an error message.
     *
     * @param msg the text to show
     */
    public void showError(String msg) {
        out.println("Error: " + msg);
    }

    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }
}
