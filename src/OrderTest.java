import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Order}.
 */
public class OrderTest {

    private static final double DELTA = 0.0001;

    private Order order;
    private MenuItem burger;
    private MenuItem fries;

    @BeforeEach
    public void setUp() {
        order = new Order(7);
        burger = new MenuItem("Burger", 8.50);
        fries = new MenuItem("Fries", 3.25);
    }

    // ---------- constructor / initial state ----------

    @Test
    public void newOrderHasTicketNumber() {
        assertEquals(7, order.getTicketNumber());
    }

    @Test
    public void newOrderIsEmpty() {
        assertTrue(order.isEmpty());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    public void newOrderStartsWaiting() {
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void newOrderTotalIsZero() {
        assertEquals(0.0, order.getTotal(), DELTA);
    }

    @Test
    public void ticketNumberIsNotValidated() {
        assertEquals(0, new Order(0).getTicketNumber());
        assertEquals(-5, new Order(-5).getTicketNumber());
    }

    // ---------- addItem ----------

    @Test
    public void addItemMakesOrderNonEmpty() {
        order.addItem(burger, 1);
        assertFalse(order.isEmpty());
        assertEquals(1, order.getItems().size());
    }

    @Test
    public void addItemStoresInOrderAdded() {
        order.addItem(burger, 1);
        order.addItem(fries, 2);
        List<OrderItem> items = order.getItems();
        assertEquals(2, items.size());
        assertEquals(8.50, items.get(0).getLineTotal(), DELTA);
        assertEquals(6.50, items.get(1).getLineTotal(), DELTA);
    }

    @Test
    public void addingSameMenuItemTwiceCreatesTwoLines() {
        order.addItem(burger, 1);
        order.addItem(burger, 1);
        assertEquals(2, order.getItems().size());
    }

    @Test
    public void addItemNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> order.addItem(null, 1));
        assertTrue(order.isEmpty());
    }

    @Test
    public void addItemZeroQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> order.addItem(burger, 0));
        assertTrue(order.isEmpty());
    }

    @Test
    public void addItemNegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> order.addItem(burger, -3));
        assertTrue(order.isEmpty());
    }

    @Test
    public void addItemQuantityOneIsAllowed() {
        order.addItem(burger, 1);
        assertEquals(1, order.getItems().size());
    }

    @Test
    public void failedAddDoesNotDisturbExistingItems() {
        order.addItem(burger, 1);
        assertThrows(IllegalArgumentException.class, () -> order.addItem(fries, 0));
        assertEquals(1, order.getItems().size());
    }

    // ---------- getItems (read-only view) ----------

    @Test
    public void getItemsIsUnmodifiable() {
        order.addItem(burger, 1);
        List<OrderItem> items = order.getItems();
        assertThrows(UnsupportedOperationException.class, () -> items.add(null));
        assertThrows(UnsupportedOperationException.class, () -> items.remove(0));
        assertThrows(UnsupportedOperationException.class, items::clear);
    }

    @Test
    public void getItemsReflectsLaterAdds() {
        List<OrderItem> view = order.getItems();
        assertEquals(0, view.size());
        order.addItem(burger, 1);
        assertEquals(1, view.size());
    }

    // ---------- status ----------

    @Test
    public void setStatusToReady() {
        order.setStatus(OrderStatus.READY);
        assertEquals(OrderStatus.READY, order.getStatus());
    }

    @Test
    public void setStatusBackToWaiting() {
        order.setStatus(OrderStatus.READY);
        order.setStatus(OrderStatus.WAITING);
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    @Test
    public void setStatusNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> order.setStatus(null));
    }

    @Test
    public void setStatusNullLeavesStatusUnchanged() {
        try {
            order.setStatus(null);
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(OrderStatus.WAITING, order.getStatus());
    }

    // ---------- getTotal ----------

    @Test
    public void totalForSingleLine() {
        order.addItem(burger, 2);
        assertEquals(17.00, order.getTotal(), DELTA);
    }

    @Test
    public void totalForMultipleLines() {
        order.addItem(burger, 2);  // 17.00
        order.addItem(fries, 3);   // 9.75
        assertEquals(26.75, order.getTotal(), DELTA);
    }

    @Test
    public void totalIncludesOutOfStockItems() {
        // Order itself does not check stock.
        MenuItem gone = new MenuItem("Special", 10.00, false);
        order.addItem(gone, 1);
        assertEquals(10.00, order.getTotal(), DELTA);
    }

    @Test
    public void totalWithFreeItem() {
        order.addItem(new MenuItem("Water", 0.0), 4);
        order.addItem(burger, 1);
        assertEquals(8.50, order.getTotal(), DELTA);
    }

    @Test
    public void totalDoesNotChangeWhenStatusChanges() {
        order.addItem(burger, 1);
        order.setStatus(OrderStatus.READY);
        assertEquals(8.50, order.getTotal(), DELTA);
    }

    // ---------- toString ----------

    @Test
    public void toStringEmptyOrder() {
        assertEquals("Order #7 [] [WAITING]", order.toString());
    }

    @Test
    public void toStringShowsTicketAndStatus() {
        order.addItem(burger, 1);
        order.setStatus(OrderStatus.READY);
        String s = order.toString();
        assertTrue(s.startsWith("Order #7 "));
        assertTrue(s.endsWith("[READY]"));
    }

    @Test
    public void toStringIncludesItemsList() {
        order.addItem(burger, 1);
        assertTrue(order.toString().contains(order.getItems().toString()));
    }
}
