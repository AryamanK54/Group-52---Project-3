import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console view for customers checking on an order. Display and prompting only.
 * The ticket prompt returns the raw text typed; the controller decides whether
 * it is a valid number.
 */
public class CustomerStatusView {

    private final Scanner in;
    private final PrintStream out;

    /**
     * @param in  the shared input source (one Scanner for the whole program)
     * @param out where to print
     */
    public CustomerStatusView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** @return the raw text the customer typed as a ticket number; never null */
    public String promptTicket() {
        out.print("Enter your ticket number: ");
        out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    /**
     * Shows an order's status. A NOT_FOUND status is shown the same way as
     * showNotFound. The position is only shown for WAITING orders.
     *
     * @param ticket   the ticket number asked about
     * @param status   the order's status
     * @param position 1-based place in line, used only when WAITING
     */
    public void showStatus(int ticket, OrderStatus status, int position) {
        switch (status) {
            case WAITING:
                out.println("Order #" + ticket + " is waiting (position " + position + " in line).");
                break;
            case READY:
                out.println("Order #" + ticket + " is READY for pickup!");
                break;
            default:
                showNotFound(ticket);
                break;
        }
    }

    /**
     * Tells the customer no order exists for that ticket number.
     *
     * @param ticket the ticket number asked about
     */
    public void showNotFound(int ticket) {
        out.println("Order #" + ticket + " was not found.");
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
