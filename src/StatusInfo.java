/**
 * What OrderUpModel.getStatus(ticket) returns: the ticket asked about, that
 * order's status, and its 1-based place in the kitchen line. Immutable.
 * <p>
 * The position is only meaningful while the order is WAITING; for READY,
 * CANCELLED, and NOT_FOUND it is NOT_IN_LINE (-1).
 */
public class StatusInfo {

    /** Position value used when the order is not in the kitchen line. */
    public static final int NOT_IN_LINE = -1;

    private final int ticket;
    private final OrderStatus status;
    private final int position;

    /**
     * @param ticket   the ticket number that was looked up
     * @param status   the order's status; must not be null
     * @param position 1-based place in line, or NOT_IN_LINE if not waiting
     * @throws IllegalArgumentException if status is null
     */
    public StatusInfo(int ticket, OrderStatus status, int position) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.ticket = ticket;
        this.status = status;
        this.position = position;
    }

    /**
     * @param ticket the ticket number that was looked up
     * @return the "not found" result: NOT_FOUND with no position
     */
    public static StatusInfo notFound(int ticket) {
        return new StatusInfo(ticket, OrderStatus.NOT_FOUND, NOT_IN_LINE);
    }

    /** @return the ticket number that was looked up */
    public int getTicket() {
        return ticket;
    }

    /** @return the order's status */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return 1-based place in line, or NOT_IN_LINE (-1) if not waiting */
    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "#" + ticket + " " + status
                + (position == NOT_IN_LINE ? "" : " (position " + position + ")");
    }
}
