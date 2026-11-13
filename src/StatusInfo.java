/**
 * What OrderUpModel.getStatus(ticket) returns: an order's status plus its
 * 1-based place in the kitchen line. Immutable.
 * <p>
 * The position is only meaningful while the order is WAITING; for READY and
 * NOT_FOUND it is NO_POSITION (-1).
 */
public class StatusInfo {

    /** Position value used when the order is not in the kitchen line. */
    public static final int NO_POSITION = -1;

    private final OrderStatus status;
    private final int position;

    /**
     * @param status   the order's status; must not be null
     * @param position 1-based place in line, or NO_POSITION if not waiting
     * @throws IllegalArgumentException if status is null
     */
    public StatusInfo(OrderStatus status, int position) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
        this.position = position;
    }

    /** @return the "not found" result: NOT_FOUND with no position */
    public static StatusInfo notFound() {
        return new StatusInfo(OrderStatus.NOT_FOUND, NO_POSITION);
    }

    /** @return the order's status */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return 1-based place in line, or NO_POSITION (-1) if not waiting */
    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return status + (position == NO_POSITION ? "" : " (position " + position + ")");
    }
}
