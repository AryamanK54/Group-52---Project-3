package orderup.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * // -------------------------------------------------------------------------
 * /** Unit tests for the {@link KitchenQueue} class.
 * 
 * @author Vanessa Flores
 * @version Nov 12, 2026
 */
class KitchenQueueTest
{

    private KitchenQueue queue;

    /**
     * Initializes a fresh empty KitchenQueue before each test.
     */
    @BeforeEach
    void setUp()
    {
        queue = new KitchenQueue();
    }


    private static Order order(int ticket)
    {
        return new Order(ticket);
    }

    // ---------- enqueue / dequeueFront ----------


    /**
     * Tests that dequeueFront removes and returns orders in first-in, first-out
     * (FIFO) order.
     */
    @Test
    void dequeueReturnsOrdersInFifoOrder()
    {
        Order a = order(1);
        Order b = order(2);
        Order c = order(3);
        queue.enqueue(a);
        queue.enqueue(b);
        queue.enqueue(c);
        assertSame(a, queue.dequeueFront());
        assertSame(b, queue.dequeueFront());
        assertSame(c, queue.dequeueFront());
    }


    /**
     * Tests that size and isEmpty accurately reflect the number of active
     * orders in the queue.
     */
    @Test
    void sizeAndIsEmptyTrackContents()
    {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        queue.enqueue(order(1));
        queue.enqueue(order(2));
        assertFalse(queue.isEmpty());
        assertEquals(2, queue.size());
        queue.dequeueFront();
        assertEquals(1, queue.size());
    }


    /**
     * Tests that calling dequeueFront on an empty queue throws a
     * NoSuchElementException
     */
    @Test
    void dequeueOnEmptyQueueThrows()
    {
        assertThrows(NoSuchElementException.class, () -> queue.dequeueFront());
    }


    /**
     * Tests that enqueueing a null order throws an IllegalArgumentException and
     * leaves the queue contents unchanged.
     */
    @Test
    void enqueueNullThrowsAndLeavesQueueUnchanged()
    {
        queue.enqueue(order(1));
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
        assertEquals(1, queue.size());
    }


    /**
     * Tests that enqueueing orders that do not have WAITING status throws an
     * IllegalArgumentException and leaves the queue empty.
     */
    @Test
    void enqueueNonWaitingOrderThrowsAndLeavesQueueUnchanged()
    {
        Order ready = order(1);
        ready.setStatus(OrderStatus.READY);
        Order cancelled = order(2);
        cancelled.setStatus(OrderStatus.CANCELLED);
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.enqueue(ready));
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.enqueue(cancelled));
        assertTrue(queue.isEmpty());
    }

    // ---------- peekFront ----------


    /**
     * Tests that peekFront returns the front order without removing it from the
     * queue.
     */
    @Test
    void peekFrontReturnsFrontWithoutRemoving()
    {
        Order a = order(1);
        queue.enqueue(a);
        queue.enqueue(order(2));
        assertSame(a, queue.peekFront());
        assertEquals(2, queue.size());
    }


    /**
     * Tests that peekFront returns null when the queue is empty.
     */
    @Test
    void peekFrontOnEmptyQueueReturnsNull()
    {
        assertNull(queue.peekFront());
    }

    // ---------- addRemakeToFront ----------


    /**
     * Tests that addRemakeToFront places a remade order ahead of all existing
     * waiting orders.
     */
    @Test
    void remakeGoesToFrontAheadOfWaitingOrders()
    {
        Order a = order(1);
        Order b = order(2);
        Order remake = order(3);
        queue.enqueue(a);
        queue.enqueue(b);
        queue.addRemakeToFront(remake);
        assertSame(remake, queue.peekFront());
        assertSame(remake, queue.dequeueFront());
        assertSame(a, queue.dequeueFront());
        assertSame(b, queue.dequeueFront());
    }


    /**
     * Tests that adding a remake to any empty queue makes it the sole waiting
     * order.
     */
    @Test
    void remakeIntoEmptyQueueBecomesTheOnlyOrder()
    {
        Order remake = order(1);
        queue.addRemakeToFront(remake);
        assertEquals(1, queue.size());
        assertSame(remake, queue.peekFront());
    }


    /**
     * Tests that passing null or a non-WAITING order to addRemakeToFront throws
     * an IllegalArgumentExeption and leaves the queue unchanged.
     */
    @Test
    void remakeNullOrNonWaitingThrowsAndLeavesQueueUnchanged()
    {
        queue.enqueue(order(1));
        Order ready = order(2);
        ready.setStatus(OrderStatus.READY);
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.addRemakeToFront(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.addRemakeToFront(ready));
        assertEquals(1, queue.size());
        assertEquals(1, queue.peekFront().getTicketNumber());
    }

    // ---------- positionOf ----------


    /**
     * Tests that positionOf returns the 1-based position of active orders from
     * the front of the queue.
     */
    @Test
    void positionOfIsOneBasedFromTheFront()
    {
        queue.enqueue(order(10));
        queue.enqueue(order(20));
        queue.enqueue(order(30));
        assertEquals(1, queue.positionOf(10));
        assertEquals(2, queue.positionOf(20));
        assertEquals(3, queue.positionOf(30));
    }


    /**
     * Tests that positionOf returns -1 for ticket numbers not present in the
     * queue.
     */
    @Test
    void positionOfUnknownTicketIsMinusOne()
    {
        queue.enqueue(order(1));
        assertEquals(-1, queue.positionOf(99));
        assertEquals(-1, new KitchenQueue().positionOf(1));
    }


    /**
     * Tests that remaining orders shift forward in position after the front
     * order is dequeued.
     */
    @Test
    void positionsShiftAfterDequeue()
    {
        queue.enqueue(order(1));
        queue.enqueue(order(2));
        queue.dequeueFront();
        assertEquals(-1, queue.positionOf(1));
        assertEquals(1, queue.positionOf(2));
    }

    // ---------- cancelled orders are flagged, not removed ----------


    /**
     * Tests that cancelled orders are ignored by peekFront, size, and
     * positionOf.
     */
    @Test
    void cancelledOrdersAreIgnoredByPeekSizeAndPosition()
    {
        Order a = order(1);
        Order b = order(2);
        Order c = order(3);
        queue.enqueue(a);
        queue.enqueue(b);
        queue.enqueue(c);
        a.setStatus(OrderStatus.CANCELLED);
        assertSame(b, queue.peekFront());
        assertEquals(2, queue.size());
        assertEquals(-1, queue.positionOf(1));
        assertEquals(1, queue.positionOf(2));
        assertEquals(2, queue.positionOf(3));
    }


    /**
     * Tests that dequeueFront skips over cancelled orders in the middle of
     * queue.
     */
    @Test
    void cancelledOrderInTheMiddleIsSkippedByDequeue()
    {
        Order a = order(1);
        Order b = order(2);
        Order c = order(3);
        queue.enqueue(a);
        queue.enqueue(b);
        queue.enqueue(c);
        b.setStatus(OrderStatus.CANCELLED);
        assertSame(a, queue.dequeueFront());
        assertSame(c, queue.dequeueFront());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests that a queue containing only cancelled orders behaves as empty
     * across isEmpty, size, peekFront, and dequeueFront.
     */
    @Test
    void queueWithOnlyCancelledOrdersActsEmpty()
    {
        Order a = order(1);
        queue.enqueue(a);
        a.setStatus(OrderStatus.CANCELLED);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peekFront());
        assertThrows(NoSuchElementException.class, () -> queue.dequeueFront());
    }
}
