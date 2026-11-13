/**
 * One entry on the fixed menu: a name, a price, and whether it is in stock.
 * Name and price never change after construction; stock can run out.
 */
public class MenuItem {

    private final String name;
    private final double price;
    private boolean inStock;

    /**
     * Creates an in-stock menu item.
     *
     * @param name  the item's name; must not be null or blank
     * @param price the item's price; must not be negative
     * @throws IllegalArgumentException if the name is null/blank or the price is negative
     */
    public MenuItem(String name, double price) {
        this(name, price, true);
    }

    /**
     * Creates a menu item with an explicit stock flag.
     *
     * @param name    the item's name; must not be null or blank
     * @param price   the item's price; must not be negative
     * @param inStock whether the item can currently be ordered
     * @throws IllegalArgumentException if the name is null/blank or the price is negative
     */
    public MenuItem(String name, double price, boolean inStock) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Menu item name must not be null or blank");
        }
        if (Double.isNaN(price) || price < 0) {
            throw new IllegalArgumentException("Menu item price must not be negative");
        }
        this.name = name;
        this.price = price;
        this.inStock = inStock;
    }

    /** @return the item's name */
    public String getName() {
        return name;
    }

    /** @return the item's price */
    public double getPrice() {
        return price;
    }

    /** @return true if the item can currently be ordered */
    public boolean isInStock() {
        return inStock;
    }

    /**
     * Marks the item as in or out of stock.
     *
     * @param inStock the new stock flag
     */
    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    @Override
    public String toString() {
        return name + " ($" + String.format("%.2f", price) + ")";
    }
}
