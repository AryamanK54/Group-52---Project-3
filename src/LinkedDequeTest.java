import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link LinkedDeque} class.
 */
class LinkedDequeTest
{

    private LinkedDeque<Integer> deque;

    /**
     * Initializes a fresh empty deque before each test.
     */
    @BeforeEach
    void setUp()
    {
        deque = new LinkedDeque<>();
    }


    private List<Integer> contents()
    {
        List<Integer> result = new ArrayList<>();
        for (Integer i : deque)
        {
            result.add(i);
        }
        return result;
    }

    // ---------- empty deque ----------


    /**
     * Tests the state of a new deque.
     */
    @Test
    void newDequeIsEmpty()
    {
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
        assertNull(deque.peekFirst());
        assertNull(deque.pollFirst());
        assertFalse(deque.iterator().hasNext());
    }

    // ---------- addFirst / addLast / pollFirst / peekFirst ----------


    /**
     * Tests that addLast followed by pollFirst is first-in, first-out.
     */
    @Test
    void addLastThenPollFirstIsFifo()
    {
        deque.addLast(1);
        deque.addLast(2);
        deque.addLast(3);
        assertEquals(1, deque.pollFirst());
        assertEquals(2, deque.pollFirst());
        assertEquals(3, deque.pollFirst());
        assertNull(deque.pollFirst());
    }


    /**
     * Tests that addFirst puts each new element ahead of the previous ones.
     */
    @Test
    void addFirstReversesInsertionOrder()
    {
        deque.addFirst(1);
        deque.addFirst(2);
        deque.addFirst(3);
        assertEquals(List.of(3, 2, 1), contents());
    }


    /**
     * Tests that addFirst and addLast can be mixed.
     */
    @Test
    void addFirstAndAddLastMix()
    {
        deque.addLast(2);
        deque.addFirst(1);
        deque.addLast(3);
        deque.addFirst(0);
        assertEquals(List.of(0, 1, 2, 3), contents());
        assertEquals(4, deque.size());
    }


    /**
     * Tests that peekFirst does not remove the element.
     */
    @Test
    void peekFirstDoesNotRemove()
    {
        deque.addLast(1);
        deque.addLast(2);
        assertEquals(1, deque.peekFirst());
        assertEquals(1, deque.peekFirst());
        assertEquals(2, deque.size());
    }


    /**
     * Tests that size and isEmpty track adds and polls.
     */
    @Test
    void sizeAndIsEmptyTrackContents()
    {
        deque.addLast(1);
        deque.addFirst(2);
        assertFalse(deque.isEmpty());
        assertEquals(2, deque.size());
        deque.pollFirst();
        assertEquals(1, deque.size());
        deque.pollFirst();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }


    /**
     * Tests that the deque is fully usable again after being emptied (head and
     * tail were reset).
     */
    @Test
    void dequeIsReusableAfterBeingEmptied()
    {
        deque.addLast(1);
        deque.pollFirst();
        deque.addLast(2);
        deque.addFirst(1);
        assertEquals(List.of(1, 2), contents());
        deque.pollFirst();
        deque.pollFirst();
        deque.addFirst(3);
        assertEquals(List.of(3), contents());
    }


    /**
     * Tests that null elements are rejected by every add method and leave the
     * deque unchanged.
     */
    @Test
    void nullIsRejectedEverywhere()
    {
        deque.addLast(1);
        assertThrows(
            IllegalArgumentException.class,
            () -> deque.addFirst(null));
        assertThrows(IllegalArgumentException.class, () -> deque.addLast(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> deque.addFirstPriority(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> deque.addAfterPriority(null));
        assertEquals(List.of(1), contents());
        assertEquals(1, deque.size());
    }

    // ---------- iterator ----------


    /**
     * Tests that the iterator visits elements front to back and then throws.
     */
    @Test
    void iteratorVisitsFrontToBackThenThrows()
    {
        deque.addLast(1);
        deque.addLast(2);
        Iterator<Integer> it = deque.iterator();
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
    }


    /**
     * Tests that iterating does not modify the deque.
     */
    @Test
    void iteratingDoesNotModifyDeque()
    {
        deque.addLast(1);
        deque.addLast(2);
        contents();
        contents();
        assertEquals(2, deque.size());
        assertEquals(1, deque.peekFirst());
    }

    // ---------- priority region ----------


    /**
     * Tests that addAfterPriority on an empty deque places the element at the
     * front.
     */
    @Test
    void addAfterPriorityIntoEmptyDeque()
    {
        deque.addAfterPriority(1);
        assertEquals(List.of(1), contents());
        assertEquals(1, deque.size());
    }


    /**
     * Tests that addAfterPriority puts elements ahead of normal elements.
     */
    @Test
    void addAfterPriorityGoesAheadOfNormalElements()
    {
        deque.addLast(1);
        deque.addLast(2);
        deque.addAfterPriority(10);
        assertEquals(List.of(10, 1, 2), contents());
    }


    /**
     * Tests that repeated addAfterPriority calls keep insertion order.
     */
    @Test
    void addAfterPriorityKeepsInsertionOrder()
    {
        deque.addLast(1);
        deque.addAfterPriority(10);
        deque.addAfterPriority(11);
        deque.addAfterPriority(12);
        assertEquals(List.of(10, 11, 12, 1), contents());
    }


    /**
     * Tests that addAfterPriority lands behind a front-priority element.
     */
    @Test
    void addAfterPriorityGoesBehindFirstPriority()
    {
        deque.addLast(1);
        deque.addFirstPriority(20);
        deque.addAfterPriority(10);
        assertEquals(List.of(20, 10, 1), contents());
    }


    /**
     * Tests that a later addFirstPriority goes ahead of the whole region
     * without moving the boundary.
     */
    @Test
    void addFirstPriorityGoesAheadOfRegion()
    {
        deque.addLast(1);
        deque.addAfterPriority(10);
        deque.addFirstPriority(20);
        deque.addAfterPriority(11);
        assertEquals(List.of(20, 10, 11, 1), contents());
    }


    /**
     * Tests that addFirstPriority on a deque with only normal elements starts
     * the region.
     */
    @Test
    void addFirstPriorityStartsRegion()
    {
        deque.addLast(1);
        deque.addFirstPriority(20);
        deque.addAfterPriority(10);
        deque.addLast(2);
        assertEquals(List.of(20, 10, 1, 2), contents());
    }


    /**
     * Tests that addLast still appends at the true end after the boundary node
     * is the tail (the tail pointer must be correct).
     */
    @Test
    void addLastAfterBoundaryIsTail()
    {
        deque.addAfterPriority(1);
        deque.addAfterPriority(2);
        deque.addLast(3);
        deque.addAfterPriority(4);
        assertEquals(List.of(1, 2, 4, 3), contents());
        assertEquals(1, deque.pollFirst());
        assertEquals(2, deque.pollFirst());
        assertEquals(4, deque.pollFirst());
        assertEquals(3, deque.pollFirst());
        assertNull(deque.pollFirst());
    }


    /**
     * Tests that inserting at the boundary when the boundary is the tail
     * updates the tail, so addLast goes after it.
     */
    @Test
    void insertAtBoundaryTailUpdatesTail()
    {
        deque.addAfterPriority(1);
        deque.addAfterPriority(2);
        deque.addLast(3);
        assertEquals(List.of(1, 2, 3), contents());
    }


    /**
     * Tests that polling the boundary node clears the region, so the next
     * addAfterPriority goes to the front rather than after a removed node.
     */
    @Test
    void polledBoundaryResetsRegion()
    {
        deque.addAfterPriority(1);
        deque.addLast(2);
        assertEquals(1, deque.pollFirst());
        deque.addAfterPriority(3);
        assertEquals(List.of(3, 2), contents());
        assertEquals(2, deque.size());
    }


    /**
     * Tests that polling elements before the boundary leaves the region intact.
     */
    @Test
    void pollingBeforeBoundaryKeepsRegion()
    {
        deque.addLast(1);
        deque.addAfterPriority(10);
        deque.addAfterPriority(11);
        deque.pollFirst();
        deque.addAfterPriority(12);
        assertEquals(List.of(11, 12, 1), contents());
    }


    /**
     * Tests that emptying the deque clears the region.
     */
    @Test
    void emptyingDequeResetsRegion()
    {
        deque.addAfterPriority(1);
        deque.pollFirst();
        deque.addLast(2);
        deque.addAfterPriority(3);
        assertEquals(List.of(3, 2), contents());
    }

    // ---------- interleaved sequence ----------


    /**
     * Tests an interleaved mix of every operation, checking the full contents
     * after each step.
     */
    @Test
    void interleavedOperationsKeepCorrectOrder()
    {
        deque.addLast(1);
        assertEquals(List.of(1), contents());
        deque.addAfterPriority(10);
        assertEquals(List.of(10, 1), contents());
        deque.addLast(2);
        assertEquals(List.of(10, 1, 2), contents());
        deque.addAfterPriority(11);
        assertEquals(List.of(10, 11, 1, 2), contents());
        deque.addFirstPriority(20);
        assertEquals(List.of(20, 10, 11, 1, 2), contents());
        assertEquals(20, deque.pollFirst());
        assertEquals(List.of(10, 11, 1, 2), contents());
        deque.addAfterPriority(12);
        assertEquals(List.of(10, 11, 12, 1, 2), contents());
        assertEquals(10, deque.pollFirst());
        assertEquals(11, deque.pollFirst());
        assertEquals(12, deque.pollFirst());
        assertEquals(List.of(1, 2), contents());
        deque.addAfterPriority(13);
        assertEquals(List.of(13, 1, 2), contents());
        deque.addFirst(0);
        assertEquals(List.of(0, 13, 1, 2), contents());
        assertEquals(4, deque.size());
    }
}
