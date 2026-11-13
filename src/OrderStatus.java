/**
 * The states an order can be in. Used by the model and displayed (read-only)
 * by the views.
 */
public enum OrderStatus {
    /** The order is in the kitchen queue, waiting to be made. */
    WAITING,
    /** The cook has finished the order; it is ready for pickup. */
    READY,
    /** No order exists for the ticket number that was asked about. */
    NOT_FOUND,
    /**
     * The order was cancelled before it was made. It stays in the order
     * history and the kitchen line skips it.
     */
    CANCELLED
}
