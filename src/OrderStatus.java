/**
 * The states an order can be in. Used by the model and displayed (read-only)
 * by the views.
 * <p>
 * CANCELLED is intentionally absent: it is the Deliverable 4 change feature
 * and will be added then.
 */
public enum OrderStatus {
    /** The order is in the kitchen queue, waiting to be made. */
    WAITING,
    /** The cook has finished the order; it is ready for pickup. */
    READY,
    /** No order exists for the ticket number that was asked about. */
    NOT_FOUND
}
