import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MenuItem}.
 */
public class MenuItemTest {

    private static final double DELTA = 0.0001;

    // ---------- constructors ----------

    @Test
    public void twoArgConstructorDefaultsToInStock() {
        MenuItem item = new MenuItem("Burger", 8.99);
        assertEquals("Burger", item.getName());
        assertEquals(8.99, item.getPrice(), DELTA);
        assertTrue(item.isInStock());
    }

    @Test
    public void threeArgConstructorInStockTrue() {
        MenuItem item = new MenuItem("Fries", 3.50, true);
        assertTrue(item.isInStock());
    }

    @Test
    public void threeArgConstructorInStockFalse() {
        MenuItem item = new MenuItem("Fries", 3.50, false);
        assertFalse(item.isInStock());
        assertEquals("Fries", item.getName());
        assertEquals(3.50, item.getPrice(), DELTA);
    }

    @Test
    public void zeroPriceIsAllowed() {
        MenuItem item = new MenuItem("Water", 0.0);
        assertEquals(0.0, item.getPrice(), DELTA);
    }

    @Test
    public void nameWithSurroundingWhitespaceIsKeptAsIs() {
        MenuItem item = new MenuItem("  Soda  ", 1.0);
        assertEquals("  Soda  ", item.getName());
    }

    // ---------- constructor validation ----------

    @Test
    public void nullNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(null, 5.0));
    }

    @Test
    public void emptyNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("", 5.0));
    }

    @Test
    public void blankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("   \t", 5.0));
    }

    @Test
    public void negativePriceThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Burger", -0.01));
    }

    @Test
    public void nanPriceThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Burger", Double.NaN));
    }

    @Test
    public void negativeInfinityPriceThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new MenuItem("Burger", Double.NEGATIVE_INFINITY));
    }

    @Test
    public void validationAppliesToThreeArgConstructorToo() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(null, 1.0, true));
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(" ", 1.0, false));
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("A", -1.0, true));
    }

    @Test
    public void exceptionMessagesAreDescriptive() {
        IllegalArgumentException nameEx = assertThrows(IllegalArgumentException.class,
            () -> new MenuItem("", 1.0));
        assertTrue(nameEx.getMessage().toLowerCase().contains("name"));

        IllegalArgumentException priceEx = assertThrows(IllegalArgumentException.class,
            () -> new MenuItem("A", -1.0));
        assertTrue(priceEx.getMessage().toLowerCase().contains("price"));
    }

    // ---------- setInStock ----------

    @Test
    public void setInStockToFalse() {
        MenuItem item = new MenuItem("Burger", 8.99);
        item.setInStock(false);
        assertFalse(item.isInStock());
    }

    @Test
    public void setInStockBackToTrue() {
        MenuItem item = new MenuItem("Burger", 8.99, false);
        item.setInStock(true);
        assertTrue(item.isInStock());
    }

    @Test
    public void setInStockDoesNotChangeNameOrPrice() {
        MenuItem item = new MenuItem("Burger", 8.99);
        item.setInStock(false);
        assertEquals("Burger", item.getName());
        assertEquals(8.99, item.getPrice(), DELTA);
    }

    // ---------- toString ----------

    @Test
    public void toStringFormatsTwoDecimals() {
        assertEquals("Burger ($8.99)", new MenuItem("Burger", 8.99).toString());
    }

    @Test
    public void toStringPadsWholeNumbers() {
        assertEquals("Water ($2.00)", new MenuItem("Water", 2).toString());
    }

    @Test
    public void toStringZeroPrice() {
        assertEquals("Free ($0.00)", new MenuItem("Free", 0).toString());
    }

    @Test
    public void toStringRoundsToTwoDecimals() {
        assertEquals("Fries ($3.57)", new MenuItem("Fries", 3.567).toString());
    }

    @Test
    public void toStringIgnoresStockFlag() {
        assertEquals("Burger ($8.99)", new MenuItem("Burger", 8.99, false).toString());
    }
}
