import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console view for the top-level menu that lets the user pick what to do.
 * Display and prompting only: it never decides what a choice means. The
 * controller interprets the returned text.
 */
public class MainMenuView {

    private final Scanner in;
    private final PrintStream out;

    /**
     * @param in  the shared input source (one Scanner for the whole program)
     * @param out where to print
     */
    public MainMenuView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Prints the list of actions, numbered 1-6, with 0 to quit. */
    public void showMainMenu() {
        out.println();
        out.println("=== Order Up! ===");
        out.println("Cashier");
        out.println("  1) Start new order");
        out.println("  2) Add item to order");
        out.println("  3) Submit order");
        out.println("Kitchen");
        out.println("  4) Show next order");
        out.println("  5) Mark next order done");
        out.println("Customer");
        out.println("  6) Check order status");
        out.println("  0) Quit");
    }

    /**
     * Asks which action to take and returns exactly what was typed, trimmed.
     * If the input stream has ended, returns "0" (quit) so the main loop
     * cannot spin forever.
     *
     * @return the raw choice text; never null
     */
    public String promptChoice() {
        out.print("Choose an option: ");
        out.flush();
        if (!in.hasNextLine()) {
            return "0";
        }
        return in.nextLine().trim();
    }

    /** Shows a goodbye message. */
    public void showGoodbye() {
        out.println("Goodbye!");
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
