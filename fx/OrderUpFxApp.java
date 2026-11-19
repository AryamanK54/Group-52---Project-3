import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Starts the JavaFX version of Order Up!. It builds the same model and
 * controller as the console version, but with the three JavaFX screens as
 * the views. Buttons along the top switch between the Cashier, Kitchen, and
 * Customer screens; each screen only has the actions for that role.
 *
 * @author Aryaman Kapoor
 * @version 2026.11.18
 */
public class OrderUpFxApp extends Application {

    private final BorderPane root = new BorderPane();
    private final Label roleLabel = new Label();

    @Override
    public void start(Stage stage) {
        OrderUpModel model = new OrderUpModel();
        FxCashierView cashier = new FxCashierView();
        FxKitchenView kitchen = new FxKitchenView();
        FxCustomerStatusView customer = new FxCustomerStatusView();
        // The console main menu is only used by OrderUpController.run(),
        // which the JavaFX version never calls.
        MainMenuView unusedMenu = new MainMenuView(new Scanner(""),
                new PrintStream(OutputStream.nullOutputStream()));
        OrderUpController controller = new OrderUpController(model, cashier,
                kitchen, customer, unusedMenu);

        cashier.connect(controller);
        kitchen.connect(controller);
        customer.connect(controller);

        Button cashierButton = new Button("Cashier");
        Button kitchenButton = new Button("Kitchen");
        Button customerButton = new Button("Customer");
        cashierButton.setOnAction(e -> show("Cashier", cashier.getPane()));
        kitchenButton.setOnAction(e -> {
            // Refresh the line, since orders may have changed elsewhere.
            controller.handleShowNext();
            show("Kitchen", kitchen.getPane());
        });
        customerButton.setOnAction(e -> show("Customer", customer.getPane()));

        roleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, roleLabel, spacer, cashierButton,
                kitchenButton, customerButton);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10, 12, 10, 12));
        top.setStyle("-fx-background-color: #e8eaf6;");
        root.setTop(top);

        show("Cashier", cashier.getPane());
        stage.setTitle("Order Up!");
        stage.setScene(new Scene(root, 620, 600));
        stage.show();
    }

    private void show(String role, Pane screen) {
        roleLabel.setText("Order Up!  -  " + role);
        root.setCenter(screen);
    }

    /**
     * Launches the JavaFX window.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        launch(args);
    }
}
