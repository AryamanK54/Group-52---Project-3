import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * JavaFX screen for customers checking on an order. It extends the console
 * CustomerStatusView so the controller can use it unchanged; each show...
 * method updates this screen instead of printing.
 * <p>
 * Display and event forwarding only: the typed ticket number goes to the
 * controller as text, and the controller decides whether it is a number.
 *
 * @author Aryaman Kapoor
 * @version 2026.11.18
 */
public class FxCustomerStatusView extends CustomerStatusView {

    private final TextField ticketField = new TextField();
    private final Button checkButton = new Button("Check status");
    private final Label result = new Label();

    private final VBox pane;

    /**
     * Builds the customer screen. Call connect before showing it.
     */
    public FxCustomerStatusView() {
        // The console prompt is never used by this screen.
        super(new Scanner(""), new PrintStream(OutputStream.nullOutputStream()));

        ticketField.setPrefColumnCount(6);
        ticketField.setPromptText("ticket #");
        result.setWrapText(true);
        result.setStyle("-fx-font-size: 16px;");

        HBox row = new HBox(8, new Label("Your ticket number:"), ticketField,
                checkButton);
        pane = new VBox(14, row, result);
        pane.setPadding(new Insets(12));
    }

    /**
     * Sends this screen's button click (and Enter in the ticket box) to the
     * controller.
     *
     * @param controller the controller that handles every action
     */
    public void connect(OrderUpController controller) {
        checkButton.setOnAction(e ->
                controller.handleStatusLookup(ticketField.getText()));
        ticketField.setOnAction(e ->
                controller.handleStatusLookup(ticketField.getText()));
    }

    /** @return the screen to put in the window */
    public Pane getPane() {
        return pane;
    }

    @Override
    public void showStatus(int ticket, OrderStatus status, int position) {
        switch (status) {
            case WAITING:
                show("Order #" + ticket + " is waiting (position " + position
                        + " in line).", "#0d47a1");
                break;
            case READY:
                show("Order #" + ticket + " is READY for pickup!", "#1b5e20");
                break;
            case CANCELLED:
                show("Order #" + ticket + " was cancelled.", "#6d4c41");
                break;
            default:
                showNotFound(ticket);
                break;
        }
    }

    @Override
    public void showNotFound(int ticket) {
        show("Order #" + ticket + " was not found.", "#444444");
    }

    @Override
    public void showError(String msg) {
        show("Error: " + msg, "#b00020");
    }

    private void show(String text, String color) {
        result.setStyle("-fx-font-size: 16px; -fx-text-fill: " + color + ";");
        result.setText(text);
    }
}
