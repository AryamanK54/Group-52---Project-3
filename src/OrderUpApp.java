import java.util.Scanner;

/**
 * Starts Order Up!. Builds the model, the three views, and the controller,
 * then hands control to the controller's main loop.
 *
 * @author Syed Rishad
 * @version 2026.11.12
 */
public class OrderUpApp {

    /**
     * Runs the program on the console.
     *
     * @param args
     *            not used
     */
    public static void main(String[] args) {
        // One Scanner for the whole program, shared by the controller and
        // the views that prompt.
        Scanner in = new Scanner(System.in);
        OrderUpModel model = new OrderUpModel();
        CashierView cashierView = new CashierView(in, System.out);
        KitchenView kitchenView = new KitchenView(System.out);
        CustomerStatusView customerView =
            new CustomerStatusView(in, System.out);
        OrderUpController controller = new OrderUpController(model,
            cashierView, kitchenView, customerView, in, System.out);
        controller.run();
    }
}
