import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * JavaFX screen for the cashier. It extends the console CashierView so the
 * controller can use it unchanged: every show... method the controller calls
 * is overridden to update this screen instead of printing.
 * <p>
 * Display and event forwarding only. The buttons pass what was typed or
 * selected to the controller as text, and the controller decides whether it
 * is valid. The screen keeps no copy of the model: the menu list and the
 * current order are redrawn from whatever the controller last sent.
 *
 * @author Aryaman Kapoor
 * @version 2026.11.18
 */
public class FxCashierView extends CashierView {

    private final ListView<String> menuList = new ListView<>();
    private final TextField qtyField = new TextField("1");
    private final CheckBox rushBox = new CheckBox("Rush order");
    private final TextField cancelField = new TextField();
    private final TextArea orderArea = new TextArea();
    private final Label message = new Label();

    private final Button newButton = new Button("New order");
    private final Button addButton = new Button("Add item");
    private final Button viewButton = new Button("View current order");
    private final Button submitButton = new Button("Submit order");
    private final Button cancelButton = new Button("Cancel order");

    private final VBox pane;

    /**
     * Builds the cashier screen. Call connect before showing it.
     */
    public FxCashierView() {
        // The console prompts are never used by this screen.
        super(new Scanner(""), new PrintStream(OutputStream.nullOutputStream()));

        menuList.setPrefHeight(150);
        qtyField.setPrefColumnCount(4);
        cancelField.setPrefColumnCount(5);
        cancelField.setPromptText("ticket #");
        orderArea.setEditable(false);
        orderArea.setPrefRowCount(7);
        message.setWrapText(true);

        HBox addRow = new HBox(8, new Label("Quantity:"), qtyField, addButton);
        HBox orderRow = new HBox(8, newButton, viewButton);
        HBox submitRow = new HBox(8, rushBox, submitButton);
        HBox cancelRow = new HBox(8, new Label("Cancel ticket:"), cancelField,
                cancelButton);

        pane = new VBox(10,
                bold("Menu (select an item)"), menuList, addRow, orderRow,
                bold("Current order"), orderArea, submitRow, cancelRow,
                message);
        pane.setPadding(new Insets(12));
    }

    /**
     * Sends this screen's button clicks to the controller and asks it for
     * the menu and the (empty) current order.
     *
     * @param controller the controller that handles every action
     */
    public void connect(OrderUpController controller) {
        newButton.setOnAction(e -> {
            clearMessage();
            controller.handleNewOrder();
            controller.handleViewCurrentOrder();
        });
        addButton.setOnAction(e -> {
            clearMessage();
            int index = menuList.getSelectionModel().getSelectedIndex();
            // No selection is sent as item 0, which the controller rejects
            // as "not on the menu".
            String choice = String.valueOf(index + 1);
            controller.handleAddItem(choice, qtyField.getText());
            controller.handleViewCurrentOrder();
        });
        viewButton.setOnAction(e -> {
            clearMessage();
            controller.handleViewCurrentOrder();
        });
        submitButton.setOnAction(e -> {
            clearMessage();
            controller.handleSubmitOrder(rushBox.isSelected());
            controller.handleViewCurrentOrder();
        });
        cancelButton.setOnAction(e -> {
            clearMessage();
            controller.handleCancelOrder(cancelField.getText());
        });
        controller.handleShowMenu();
        controller.handleViewCurrentOrder();
    }

    /** @return the screen to put in the window */
    public Pane getPane() {
        return pane;
    }

    @Override
    public void showMenu(List<MenuItem> items) {
        menuList.getItems().clear();
        for (int i = 0; i < items.size(); i++) {
            MenuItem m = items.get(i);
            menuList.getItems().add((i + 1) + ")  " + m.getName() + "   $"
                    + String.format("%.2f", m.getPrice())
                    + (m.isInStock() ? "" : "   (OUT OF STOCK)"));
        }
    }

    @Override
    public void showNewOrderStarted() {
        info("New order started.");
    }

    @Override
    public void showItemAdded(MenuItem item, int qty) {
        info("Added " + qty + " x " + item.getName() + ".");
    }

    @Override
    public void showOrderPlaced(int ticket) {
        rushBox.setSelected(false);
        info("Order placed! Ticket #" + ticket);
    }

    @Override
    public void showRushOrderPlaced(int ticket) {
        rushBox.setSelected(false);
        info("Rush order placed! Ticket #" + ticket
                + " goes ahead of the regular orders.");
    }

    @Override
    public void showOrderCancelled(int ticket) {
        cancelField.clear();
        info("Order cancelled. Ticket #" + ticket);
    }

    @Override
    public void showCurrentOrder(List<OrderItem> items) {
        if (items.isEmpty()) {
            orderArea.setText("(no items yet)");
            return;
        }
        StringBuilder text = new StringBuilder();
        double total = 0;
        for (OrderItem oi : items) {
            text.append(oi.getQuantity()).append(" x ")
                    .append(oi.getMenuItem().getName()).append("   $")
                    .append(String.format("%.2f", oi.getLineTotal()))
                    .append('\n');
            total += oi.getLineTotal();
        }
        text.append("Total: $").append(String.format("%.2f", total));
        orderArea.setText(text.toString());
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

    private static Label bold(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }
}
