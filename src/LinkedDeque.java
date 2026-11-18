import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A generic doubly linked deque. Besides the usual end operations it tracks a
 * "priority boundary": the last node of a region at the front of the deque.
 * This allows constant-time insertion at the end of that region.
 *
 * @param <T>
 *            the element type
 */
public class LinkedDeque<T>
    implements Iterable<T>
{

    private static class Node<E>
    {
        private E data;
        @SuppressWarnings("unused")
        private Node<E> prev;
        private Node<E> next;

        Node(E data)
        {
            this.data = data;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private Node<T> boundary; // last node of the priority region, or null
    private int size;

    /**
     * Adds an element to the front.
     *
     * @param item
     *            the element to add
     * @throws IllegalArgumentException
     *             if item is null
     */
    public void addFirst(T item)
    {
        requireNonNull(item);
        Node<T> n = new Node<>(item);
        n.next = head;
        if (head == null)
        {
            tail = n;
        }
        else
        {
            head.prev = n;
        }
        head = n;
        size++;
    }


    /**
     * Adds an element to the back.
     *
     * @param item
     *            the element to add
     * @throws IllegalArgumentException
     *             if item is null
     */
    public void addLast(T item)
    {
        requireNonNull(item);
        Node<T> n = new Node<>(item);
        n.prev = tail;
        if (tail == null)
        {
            head = n;
        }
        else
        {
            tail.next = n;
        }
        tail = n;
        size++;
    }


    /**
     * Adds an element to the front. If the priority region was empty, the new
     * element becomes the whole region; otherwise the boundary is unchanged.
     *
     * @param item
     *            the element to add
     */
    public void addFirstPriority(T item)
    {
        addFirst(item);
        if (boundary == null)
        {
            boundary = head;
        }
    }


    /**
     * Adds an element immediately after the priority region (or at the front if
     * the region is empty) and extends the region to include it. Repeated calls
     * therefore keep their insertion order.
     *
     * @param item
     *            the element to add
     */
    public void addAfterPriority(T item)
    {
        if (boundary == null)
        {
            addFirstPriority(item);
            return;
        }
        requireNonNull(item);
        Node<T> n = new Node<>(item);
        n.prev = boundary;
        n.next = boundary.next;
        if (boundary.next == null)
        {
            tail = n;
        }
        else
        {
            boundary.next.prev = n;
        }
        boundary.next = n;
        boundary = n;
        size++;
    }


    /**
     * Removes and returns the first element.
     *
     * @return the first element, or null if the deque is empty
     */
    public T pollFirst()
    {
        if (head == null)
        {
            return null;
        }
        Node<T> removed = head;
        head = removed.next;
        if (head == null)
        {
            tail = null;
        }
        else
        {
            head.prev = null;
        }
        if (removed == boundary)
        {
            boundary = null; // everything before it is gone, so the region is
                             // empty
        }
        size--;
        return removed.data;
    }


    /**
     * @return the first element without removing it, or null if empty
     */
    public T peekFirst()
    {
        return head == null ? null : head.data;
    }


    /**
     * @return the number of elements
     */
    public int size()
    {
        return size;
    }


    /**
     * @return true if the deque has no elements
     */
    public boolean isEmpty()
    {
        return size == 0;
    }


    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext()
            {
                return current != null;
            }


            @Override
            public T next()
            {
                if (current == null)
                {
                    throw new NoSuchElementException();
                }
                T value = current.data;
                current = current.next;
                return value;
            }
        };
    }


    private static void requireNonNull(Object o)
    {
        if (o == null)
        {
            throw new IllegalArgumentException("item must not be null");
        }
    }
}
