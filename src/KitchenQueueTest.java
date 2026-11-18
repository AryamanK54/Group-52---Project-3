
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


    private static Order rush(int ticket)
    {
        return new Order(ticket, true);
    }


    /**
     * Asserts the exact line: size, front, and 1-based position of every ticket
     * in order.
     */
    private void assertLine(int... tickets)
    {
        assertEquals(tickets.length, queue.size());
        assertEquals(tickets.length == 0, queue.isEmpty());
        if (tickets.length > 0)
        {
            assertEquals(tickets[0], queue.peekFront().getTicketNumber());
        }
        else
        {
            assertNull(queue.peekFront());
        }
        for (int i = 0; i < tickets.length; i++)
        {
            assertEquals(i + 1, queue.positionOf(tickets[i]));
        }
    }

    // ---------- enqueueRush ----------


    /**
     * Tests that a rush order goes ahead of normal orders.
     */
    @Test
    void rushGoesAheadOfNormalOrders()
    {
        Order a = order(1);
        Order b = order(2);
        Order r = rush(3);
        queue.enqueue(a);
        queue.enqueue(b);
        queue.enqueueRush(r);
        assertSame(r, queue.dequeueFront());
        assertSame(a, queue.dequeueFront());
        assertSame(b, queue.dequeueFront());
    }


    /**
     * Tests that a rush order goes behind a remake.
     */
    @Test
    void rushGoesBehindRemake()
    {
        Order a = order(1);
        Order remake = order(2);
        Order r = rush(3);
        queue.enqueue(a);
        queue.addRemakeToFront(remake);
        queue.enqueueRush(r);
        assertSame(remake, queue.dequeueFront());
        assertSame(r, queue.dequeueFront());
        assertSame(a, queue.dequeueFront());
    }


    /**
     * Tests that a rush order goes behind earlier rush orders.
     */
    @Test
    void rushGoesBehindEarlierRushOrders()
    {
        Order r1 = rush(1);
        Order r2 = rush(2);
        Order r3 = rush(3);
        Order n = order(4);
        queue.enqueue(n);
        queue.enqueueRush(r1);
        queue.enqueueRush(r2);
        queue.enqueueRush(r3);
        assertSame(r1, queue.dequeueFront());
        assertSame(r2, queue.dequeueFront());
        assertSame(r3, queue.dequeueFront());
        assertSame(n, queue.dequeueFront());
    }


    /**
     * Tests the full layout [remakes][rush][normal], including a remake added
     * after rush orders.
     */
    @Test
    void lineIsRemakesThenRushThenNormal()
    {
        queue.enqueue(order(1));
        queue.enqueueRush(rush(2));
        queue.addRemakeToFront(order(3));
        queue.enqueueRush(rush(4));
        queue.enqueue(order(5));
        queue.addRemakeToFront(order(6));
        assertLine(6, 3, 2, 4, 1, 5);
    }


    /**
     * Tests that a rush order into an empty queue becomes the only order.
     */
    @Test
    void rushIntoEmptyQueueBecomesTheOnlyOrder()
    {
        Order r = rush(1);
        queue.enqueueRush(r);
        assertLine(1);
        assertSame(r, queue.dequeueFront());
        assertTrue(queue.isEmpty());
        assertTrue(r.isRush());
        assertFalse(order(2).isRush());
    }


    /**
     * Tests that after the last rush order is served, the next rush order still
     * goes ahead of the normal orders.
     */
    @Test
    void rushAfterRushRegionDrainedStillGoesToFront()
    {
        queue.enqueue(order(1));
        queue.enqueueRush(rush(2));
        queue.dequeueFront();
        queue.enqueueRush(rush(3));
        assertLine(3, 1);
    }


    /**
     * Tests that null or non-WAITING orders are rejected by enqueueRush and
     * leave the queue unchanged.
     */
    @Test
    void rushNullOrNonWaitingThrowsAndLeavesQueueUnchanged()
    {
        queue.enqueue(order(1));
        Order ready = rush(2);
        ready.setStatus(OrderStatus.READY);
        Order cancelled = rush(3);
        cancelled.setStatus(OrderStatus.CANCELLED);
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.enqueueRush(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.enqueueRush(ready));
        assertThrows(
            IllegalArgumentException.class,
            () -> queue.enqueueRush(cancelled));
        assertLine(1);
    }

    // ---------- cancelled rush orders ----------


    /**
     * Tests that a cancelled rush order is skipped by peekFront, size,
     * positionOf, and dequeueFront.
     */
    @Test
    void cancelledRushOrderIsSkipped()
    {
        Order n = order(1);
        Order r1 = rush(2);
        Order r2 = rush(3);
        queue.enqueue(n);
        queue.enqueueRush(r1);
        queue.enqueueRush(r2);
        r1.setStatus(OrderStatus.CANCELLED);
        assertLine(3, 1);
        assertEquals(-1, queue.positionOf(2));
        assertSame(r2, queue.dequeueFront());
        assertSame(n, queue.dequeueFront());
        assertTrue(queue.isEmpty());
    }


    /**
     * Tests that cancelling the last rush order (the region boundary) does not
     * break later rush insertion: a new rush order still goes ahead of normal
     * orders.
     */
    @Test
    void rushAfterCancelledBoundaryRushStillGoesAheadOfNormal()
    {
        Order n = order(1);
        Order r1 = rush(2);
        Order r2 = rush(3);
        queue.enqueue(n);
        queue.enqueueRush(r1);
        r1.setStatus(OrderStatus.CANCELLED);
        queue.enqueueRush(r2);
        assertLine(3, 1);
        assertSame(r2, queue.dequeueFront());
        assertSame(n, queue.dequeueFront());
    }


    /**
     * Tests that dequeueFront discards a cancelled rush order at the front and
     * returns the next active order.
     */
    @Test
    void cancelledRushAtFrontIsDiscardedByDequeue()
    {
        Order n = order(1);
        Order r = rush(2);
        queue.enqueue(n);
        queue.enqueueRush(r);
        r.setStatus(OrderStatus.CANCELLED);
        assertSame(n, queue.dequeueFront());
        queue.enqueueRush(rush(3));
        assertLine(3);
    }


    /**
     * Tests that a queue holding only a cancelled rush order acts empty.
     */
    @Test
    void queueWithOnlyCancelledRushActsEmpty()
    {
        Order r = rush(1);
        queue.enqueueRush(r);
        r.setStatus(OrderStatus.CANCELLED);
        assertLine();
        assertThrows(NoSuchElementException.class, () -> queue.dequeueFront());
    }

    // ---------- interleaved sequences ----------


    /**
     * Tests an interleaved sequence of enqueue, rush, remake, and dequeue
     * calls, checking the whole line after every step.
     */
    @Test
    void interleavedEnqueueRushRemakeAndDequeueKeepCorrectOrder()
    {
        queue.enqueue(order(1));
        queue.enqueue(order(2));
        assertLine(1, 2);
        queue.enqueueRush(rush(3));
        assertLine(3, 1, 2);
        queue.enqueue(order(4));
        assertLine(3, 1, 2, 4);
        queue.enqueueRush(rush(5));
        assertLine(3, 5, 1, 2, 4);
        queue.addRemakeToFront(order(6));
        assertLine(6, 3, 5, 1, 2, 4);
        assertEquals(6, queue.dequeueFront().getTicketNumber());
        assertLine(3, 5, 1, 2, 4);
        queue.enqueueRush(rush(7));
        assertLine(3, 5, 7, 1, 2, 4);
        assertEquals(3, queue.dequeueFront().getTicketNumber());
        assertEquals(5, queue.dequeueFront().getTicketNumber());
        assertEquals(7, queue.dequeueFront().getTicketNumber());
        assertLine(1, 2, 4);
        queue.enqueueRush(rush(8));
        assertLine(8, 1, 2, 4);
        queue.enqueue(order(9));
        assertLine(8, 1, 2, 4, 9);
        queue.enqueueRush(rush(10));
        assertLine(8, 10, 1, 2, 4, 9);
        assertEquals(8, queue.dequeueFront().getTicketNumber());
        assertEquals(10, queue.dequeueFront().getTicketNumber());
        assertLine(1, 2, 4, 9);
        queue.addRemakeToFront(order(11));
        assertLine(11, 1, 2, 4, 9);
        queue.enqueueRush(rush(12));
        assertLine(11, 12, 1, 2, 4, 9);
        while (!queue.isEmpty())
        {
            queue.dequeueFront();
        }
        assertLine();
    }


    /**
     * Tests an interleaved sequence that includes cancellations of rush and
     * normal orders, checking the line after every step.
     */
    @Test
    void interleavedSequenceWithCancellationsKeepsCorrectOrder()
    {
        Order n1 = order(1);
        Order r2 = rush(2);
        Order r3 = rush(3);
        Order n4 = order(4);
        queue.enqueue(n1);
        queue.enqueueRush(r2);
        queue.enqueueRush(r3);
        queue.enqueue(n4);
        assertLine(2, 3, 1, 4);
        r3.setStatus(OrderStatus.CANCELLED);
        assertLine(2, 1, 4);
        queue.enqueueRush(rush(5));
        assertLine(2, 5, 1, 4);
        n1.setStatus(OrderStatus.CANCELLED);
        assertLine(2, 5, 4);
        assertEquals(2, queue.dequeueFront().getTicketNumber());
        assertLine(5, 4);
        assertEquals(5, queue.dequeueFront().getTicketNumber());
        assertLine(4);
        queue.enqueueRush(rush(6));
        assertLine(6, 4);
        assertEquals(6, queue.dequeueFront().getTicketNumber());
        assertEquals(4, queue.dequeueFront().getTicketNumber());
        assertLine();
    }
}
