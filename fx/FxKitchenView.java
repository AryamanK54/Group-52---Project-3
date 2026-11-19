import java.io.OutputStream;
import java.io.PrintStream;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * JavaFX screen for the cook. It extends the console KitchenView so the
 * controller can use it unchanged; each show... method updates this screen
 * instead of printing.
 * <p>
 * Display and event forwarding only: the buttons call the controller, and
 * the screen shows whatever order and count the controller sends back.
 *
 * @author Aryaman Kapoor
 * @version 2026.11.18
 */
public class FxKitchenView extends KitchenView {

    private final Label nextLabel = new Label();
    private final TextArea itemsArea = new TextArea();
    private final Label countLabel = new Label();
    private final TextField remakeField = new TextField();
    private final Label message = new Label();

    private final Button showButton = new Button("Show next order");
    private final Button completeButton = new Button("Complete next order");
    private final Button remakeButton = new Button("Remake order");

    private final VBox pane;

    /**
     * Builds the kitchen screen. Call connect before showing it.
     */
    public FxKitchenView() {
        super(new PrintStream(OutputStream.nullOutputStream()));

        nextLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        itemsArea.setEditable(false);
        itemsArea.setPrefRowCount(8);
        remakeField.setPrefColumnCount(5);
        remakeField.setPromptText("ticket #");
        message.setWrapText(true);

        HBox actionRow = new HBox(8, showButton, completeButton);
        HBox remakeRow = new HBox(8, new Label("Remake ticket:"), remakeField,
                remakeButton);

        pane = new VBox(10, nextLabel, itemsArea, countLabel, actionRow,
                remakeRow, message);
        pane.setPadding(new Insets(12));
    }

    /**
     * Sends this screen's button clicks to the controller.
     *
     * @param controller the controller that handles every action
     */
    public void connect(OrderUpController controller) {
        showButton.setOnAction(e -> {
            clearMessage();
            controller.handleShowNext();
        });
        completeButton.setOnAction(e -> {
            clearMessage();
            controller.handleCompleteNext();
        });
        remakeButton.setOnAction(e -> {
            clearMessage();
            controller.handleRemakeOrder(remakeField.getText());
        });
        controller.handleShowNext();
    }

    /** @return the screen to put in the window */
    public Pane getPane() {
        return pane;
    }

    @Override
    public void showNextOrder(Order o) {
        if (o == null) {
            showEmptyLine();
            return;
        }
        nextLabel.setText("Make this next: Ticket #" + o.getTicketNumber()
                + (o.isRush() ? "   (RUSH)" : ""));
        StringBuilder text = new StringBuilder();
        for (OrderItem oi : o.getItems()) {
            text.append(oi.getQuantity()).append(" x ")
                    .append(oi.getMenuItem().getName()).append('\n');
        }
        itemsArea.setText(text.toString());
    }

    @Override
    public void showOrderCompleted(Order o) {
        info("Ticket #" + o.getTicketNumber() + " marked READY.");
    }

    @Override
    public void showWaitingCount(int n) {
        countLabel.setText("Orders waiting: " + n);
    }

    @Override
    public void showEmptyLine() {
        nextLabel.setText("No orders waiting.");
        itemsArea.clear();
        countLabel.setText("Orders waiting: 0");
    }

    @Override
    public void showError(String msg) {
        message.setStyle("-fx-text-fill: #b00020;");
        message.setText("Error: " + msg);
    }

    private void info(String msg) {
        message.setStyle("-fx-text-fill: #1b5e20;");
        message.setText(msg);
    }

    private void clearMessage() {
        message.setText("");
    }
}
