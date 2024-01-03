/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.sessiond.impl.util;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@link RotatableSessionContainerList} - An implementation of <code>CopyOnWriteArrayList</code> supporting {@link #rotate(Object)} method.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class RotatableSessionContainerList implements List<SessionContainer>, RandomAccess, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 8673264195747942595L;

    /**
     * The lock protecting all mutators. (We have a mild preference
     * for builtin monitors over ReentrantLock when either will do.)
     */
    final transient Object lock = new Object();

    /** The array, accessed only via getArray/setArray. */
    private final transient AtomicReference<SessionContainer[]> arrayReference = new AtomicReference<SessionContainer[]>();

    /**
     * Gets the array. Non-private so as to also be accessible
     * from CopyOnWriteArraySet class.
     */
    final SessionContainer[] getArray() {
        return arrayReference.get();
    }

    /**
     * Sets the array.
     */
    final void setArray(SessionContainer[] a) {
        arrayReference.set(a);
    }

    /**
     * Creates an empty list.
     */
    public RotatableSessionContainerList() {
        super();
        setArray(new SessionContainer[0]);
    }

    /**
     * Creates a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection of initially held elements
     * @throws NullPointerException if the specified collection is null
     */
    public RotatableSessionContainerList(Collection<SessionContainer> c) {
        super();
        SessionContainer[] es;
        if (c.getClass() == RotatableSessionContainerList.class) {
            es = ((RotatableSessionContainerList) c).getArray();
        } else {
            es = c.toArray(new SessionContainer[c.size()]);
        }
        setArray(es);
    }

    /**
     * Creates a list holding a copy of the given array.
     *
     * @param toCopyIn the array (a copy of this array is used as the
     *            internal array)
     * @throws NullPointerException if the specified array is null
     */
    public RotatableSessionContainerList(SessionContainer[] toCopyIn) {
        super();
        SessionContainer[] es = new SessionContainer[toCopyIn.length];
        System.arraycopy(toCopyIn, 0, es, 0, toCopyIn.length);
        setArray(es);
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return getArray().length;
    }

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * static version of indexOf, to allow repeated calls without
     * needing to re-acquire array each time.
     *
     * @param o element to search for
     * @param es the array
     * @param from first index to search
     * @param to one past last index to search
     * @return index of element, or -1 if absent
     */
    private static int indexOfRange(Object o, Object[] es, int from, int to) {
        if (o == null) {
            for (int i = from; i < to; i++) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = from; i < to; i++) {
                if (o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * static version of lastIndexOf.
     *
     * @param o element to search for
     * @param es the array
     * @param from index of first element of range, last element to search
     * @param to one past last element of range, first element to search
     * @return index of element, or -1 if absent
     */
    private static int lastIndexOfRange(Object o, Object[] es, int from, int to) {
        if (o == null) {
            for (int i = to - 1; i >= from; i--) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = to - 1; i >= from; i--) {
                if (o.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Copies the specified array, truncating or padding with <code>null</code>s (if necessary) so the copy has the specified length.
     *
     * @param es The array to be copied
     * @param newLength The length of the copy to be returned
     * @return A copy of the given array, truncated or padded with <code>null</code>s to obtain the specified length
     */
    private static SessionContainer[] copyOf(SessionContainer[] es, int newLength) {
        SessionContainer[] copy = new SessionContainer[newLength];
        System.arraycopy(es, 0, copy, 0, Math.min(es.length, newLength));
        return copy;
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(Object o) {
        Object[] es = getArray();
        return indexOfRange(o, es, 0, es.length);
    }

    /**
     * Returns the index of the first occurrence of the specified element in
     * this list, searching forwards from {@code index}, or returns -1 if
     * the element is not found.
     * More formally, returns the lowest index {@code i} such that
     * {@code i >= index && Objects.equals(get(i), e)},
     * or -1 if there is no such index.
     *
     * @param e element to search for
     * @param index index to start searching from
     * @return the index of the first occurrence of the element in
     *         this list at position {@code index} or later in the list;
     *         {@code -1} if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public int indexOf(SessionContainer e, int index) {
        Object[] es = getArray();
        return indexOfRange(e, es, index, es.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object o) {
        Object[] es = getArray();
        return lastIndexOfRange(o, es, 0, es.length);
    }

    /**
     * Returns the index of the last occurrence of the specified element in
     * this list, searching backwards from {@code index}, or returns -1 if
     * the element is not found.
     * More formally, returns the highest index {@code i} such that
     * {@code i <= index && Objects.equals(get(i), e)},
     * or -1 if there is no such index.
     *
     * @param e element to search for
     * @param index index to start searching backwards from
     * @return the index of the last occurrence of the element at position
     *         less than or equal to {@code index} in this list;
     *         -1 if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is greater
     *             than or equal to the current size of this list
     */
    public int lastIndexOf(SessionContainer e, int index) {
        Object[] es = getArray();
        return lastIndexOfRange(e, es, 0, index + 1);
    }

    /**
     * Returns a shallow copy of this list. (The elements themselves
     * are not copied.)
     *
     * @return a clone of this list
     */
    @Override
    public Object clone() {
        try {
            @SuppressWarnings("unchecked") RotatableSessionContainerList clone = (RotatableSessionContainerList) super.clone();
            clone.resetLock();
            // Unlike in readObject, here we cannot visibility-piggyback on the
            // volatile write in setArray().
            VarHandle.releaseFence();
            return clone;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list. (In other words, this method must allocate
     * a new array). The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all the elements in this list
     */
    @Override
    public Object[] toArray() {
        return getArray().clone();
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array. If the list fits
     * in the specified array, it is returned therein. Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If this list fits in the specified array with room to spare
     * (i.e., the array has more elements than this list), the element in
     * the array immediately following the end of the list is set to
     * {@code null}. (This is useful in determining the length of this
     * list <i>only</i> if the caller knows that this list does not contain
     * any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the list are to
     *            be stored, if it is big enough; otherwise, a new array of the
     *            same runtime type is allocated for this purpose.
     * @return an array containing all the elements in this list
     * @throws ArrayStoreException if the runtime type of the specified array
     *             is not a supertype of the runtime type of every element in
     *             this list
     * @throws NullPointerException if the specified array is null
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        Object[] es = getArray();
        int len = es.length;
        if (a.length < len) {
            return (T[]) Arrays.copyOf(es, len, a.getClass());
        } else {
            System.arraycopy(es, 0, a, 0, len);
            if (a.length > len) {
                a[len] = null;
            }
            return a;
        }
    }

    // Positional Access Operations

    static String outOfBounds(int index, int size) {
        return "Index: " + index + ", Size: " + size;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public SessionContainer get(int index) {
        return getArray()[index];
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public SessionContainer set(int index, SessionContainer element) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            SessionContainer oldValue = es[index];

            if (oldValue != element) {
                es = es.clone();
                es[index] = element;
            }
            // Ensure volatile write semantics even when oldvalue == element
            setArray(es);
            return oldValue;
        }
    }

    /**
     * Prepends the specified element to the start of this list.
     *
     * @param e element to be prepended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    public boolean offer(SessionContainer e) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            SessionContainer[] newElements = new SessionContainer[len + 1];
            System.arraycopy(es, 0, newElements, 1, len);
            newElements[0] = e;
            setArray(newElements);
            return true;
        }
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    @Override
    public boolean add(SessionContainer e) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            es = copyOf(es, len + 1);
            es[len] = e;
            setArray(es);
            return true;
        }
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void add(int index, SessionContainer element) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException(outOfBounds(index, len));
            }
            SessionContainer[] newElements;
            int numMoved = len - index;
            if (numMoved == 0) {
                newElements = copyOf(es, len + 1);
            } else {
                newElements = new SessionContainer[len + 1];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index, newElements, index + 1, numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        }
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices). Returns the element that was removed from the list.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public SessionContainer remove(int index) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            SessionContainer oldValue = es[index];
            int numMoved = len - index - 1;
            SessionContainer[] newElements;
            if (numMoved == 0) {
                newElements = copyOf(es, len - 1);
            } else {
                newElements = new SessionContainer[len - 1];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index + 1, newElements, index, numMoved);
            }
            setArray(newElements);
            return oldValue;
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present. If this list does not contain the element, it is
     * unchanged. More formally, removes the element with the lowest index
     * {@code i} such that {@code Objects.equals(o, get(i))}
     * (if such an element exists). Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        SessionContainer[] snapshot = getArray();
        int index = indexOfRange(o, snapshot, 0, snapshot.length);
        return index >= 0 && remove(o, snapshot, index);
    }

    /**
     * A version of remove(Object) using the strong hint that given
     * recent snapshot contains o at the given index.
     */
    private boolean remove(Object o, Object[] snapshot, int index) {
        synchronized (lock) {
            SessionContainer[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                findIndex: {
                    int prefix = Math.min(index, len);
                    for (int i = 0; i < prefix; i++) {
                        if (current[i] != snapshot[i] && Objects.equals(o, current[i])) {
                            index = i;
                            break findIndex;
                        }
                    }
                    if (index >= len) {
                        return false;
                    }
                    if (current[index] == o) {
                        break findIndex;
                    }
                    index = indexOfRange(o, current, index, len);
                    if (index < 0) {
                        return false;
                    }
                }
            }
            SessionContainer[] newElements = new SessionContainer[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            setArray(newElements);
            return true;
        }
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     * @throws IndexOutOfBoundsException if fromIndex or toIndex out of range
     *             ({@code fromIndex < 0 || toIndex > size() || toIndex < fromIndex})
     */
    void removeRange(int fromIndex, int toIndex) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;

            if (fromIndex < 0 || toIndex > len || toIndex < fromIndex) {
                throw new IndexOutOfBoundsException();
            }
            int newlen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0) {
                setArray(copyOf(es, newlen));
            } else {
                SessionContainer[] newElements = new SessionContainer[newlen];
                System.arraycopy(es, 0, newElements, 0, fromIndex);
                System.arraycopy(es, toIndex, newElements, fromIndex, numMoved);
                setArray(newElements);
            }
        }
    }

    /**
     * Appends the element, if not present.
     *
     * @param e element to be added to this list, if absent
     * @return {@code true} if the element was added
     */
    public boolean addIfAbsent(SessionContainer e) {
        SessionContainer[] snapshot = getArray();
        return indexOfRange(e, snapshot, 0, snapshot.length) < 0 && addIfAbsent(e, snapshot);
    }

    /**
     * A version of addIfAbsent using the strong hint that given
     * recent snapshot does not contain e.
     */
    private boolean addIfAbsent(SessionContainer e, Object[] snapshot) {
        synchronized (lock) {
            SessionContainer[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++) {
                    if (current[i] != snapshot[i] && Objects.equals(e, current[i])) {
                        return false;
                    }
                }
                if (indexOfRange(e, current, common, len) >= 0) {
                    return false;
                }
            }
            SessionContainer[] newElements = copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        }
    }

    /**
     * Returns {@code true} if this list contains all of the elements of the
     * specified collection.
     *
     * @param c collection to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements of the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        Object[] es = getArray();
        int len = es.length;
        for (Object e : c) {
            if (indexOfRange(e, es, 0, len) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes from this list all of its elements that are contained in
     * the specified collection. This is a particularly expensive operation
     * in this class because of the need for an internal temporary array.
     *
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *             is incompatible with the specified collection
     *             (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *             specified collection does not permit null elements
     *             (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *             or if the specified collection is null
     * @see #remove(Object)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(c::contains);
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection. In other words, removes from this list all of
     * its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *             is incompatible with the specified collection
     *             (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *             specified collection does not permit null elements
     *             (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *             or if the specified collection is null
     * @see #remove(Object)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }

    /**
     * Appends all of the elements in the specified collection that
     * are not already contained in this list, to the end of
     * this list, in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return the number of elements added
     * @throws NullPointerException if the specified collection is null
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection<SessionContainer> c) {
        SessionContainer[] cs = c.toArray(new SessionContainer[c.size()]);
        if (c.getClass() != ArrayList.class) {
            cs = cs.clone();
        }
        if (cs.length == 0) {
            return 0;
        }
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            int added = 0;
            // uniquify and compact elements in cs
            for (int i = 0; i < cs.length; ++i) {
                SessionContainer e = cs[i];
                if (indexOfRange(e, es, 0, len) < 0 && indexOfRange(e, cs, 0, added) < 0) {
                    cs[added++] = e;
                }
            }
            if (added > 0) {
                SessionContainer[] newElements = copyOf(es, len + added);
                System.arraycopy(cs, 0, newElements, len, added);
                setArray(newElements);
            }
            return added;
        }
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    @Override
    public void clear() {
        synchronized (lock) {
            setArray(new SessionContainer[0]);
        }
    }

    /**
     * Appends all of the elements in the specified collection to the end
     * of this list, in the order that they are returned by the specified
     * collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends SessionContainer> c) {
        SessionContainer[] cs = (c.getClass() == RotatableSessionContainerList.class) ? ((RotatableSessionContainerList) c).getArray() : c.toArray(new SessionContainer[c.size()]);
        if (cs.length == 0) {
            return false;
        }
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            SessionContainer[] newElements;
            if (len == 0 && (c.getClass() == RotatableSessionContainerList.class || c.getClass() == ArrayList.class)) {
                newElements = cs;
            } else {
                newElements = copyOf(es, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
            }
            setArray(newElements);
            return true;
        }
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position. Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices). The new elements will appear
     * in this list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *            from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     * @see #add(int,Object)
     */
    @Override
    public boolean addAll(int index, Collection<? extends SessionContainer> c) {
        Object[] cs = c.toArray();
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            if (index > len || index < 0) {
                throw new IndexOutOfBoundsException(outOfBounds(index, len));
            }
            if (cs.length == 0) {
                return false;
            }
            int numMoved = len - index;
            SessionContainer[] newElements;
            if (numMoved == 0) {
                newElements = copyOf(es, len + cs.length);
            } else {
                newElements = new SessionContainer[len + cs.length];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index, newElements, index + cs.length, numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        }
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void forEach(Consumer<? super SessionContainer> action) {
        Objects.requireNonNull(action);
        for (SessionContainer x : getArray()) {
            action.accept(x);
        }
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean removeIf(Predicate<? super SessionContainer> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }

    // A tiny bit set implementation

    private static long[] nBits(int n) {
        return new long[((n - 1) >> 6) + 1];
    }

    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }

    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & (1L << i)) == 0;
    }

    private boolean bulkRemove(Predicate<? super SessionContainer> filter) {
        synchronized (lock) {
            return bulkRemove(filter, 0, getArray().length);
        }
    }

    boolean bulkRemove(Predicate<? super SessionContainer> filter, int i, int end) {
        // assert Thread.holdsLock(lock);
        final SessionContainer[] es = getArray();
        // Optimize for initial run of survivors
        for (; i < end && !filter.test(es[i]); i++) {
            ;
        }
        if (i < end) {
            final int beg = i;
            final long[] deathRow = nBits(end - beg);
            int deleted = 1;
            deathRow[0] = 1L;   // set bit 0
            for (i = beg + 1; i < end; i++) {
                if (filter.test(es[i])) {
                    setBit(deathRow, i - beg);
                    deleted++;
                }
            }
            // Did filter reentrantly modify the list?
            if (es != getArray()) {
                throw new ConcurrentModificationException();
            }
            final SessionContainer[] newElts = copyOf(es, es.length - deleted);
            int w = beg;
            for (i = beg; i < end; i++) {
                if (isClear(deathRow, i - beg)) {
                    newElts[w++] = es[i];
                }
            }
            System.arraycopy(es, i, newElts, w, es.length - i);
            setArray(newElts);
            return true;
        } else {
            if (es != getArray()) {
                throw new ConcurrentModificationException();
            }
            return false;
        }
    }

    @Override
    public void replaceAll(UnaryOperator<SessionContainer> operator) {
        synchronized (lock) {
            replaceAllRange(operator, 0, getArray().length);
        }
    }

    void replaceAllRange(UnaryOperator<SessionContainer> operator, int i, int end) {
        // assert Thread.holdsLock(lock);
        Objects.requireNonNull(operator);
        final SessionContainer[] es = getArray().clone();
        for (; i < end; i++) {
            es[i] = operator.apply(es[i]);
        }
        setArray(es);
    }

    @Override
    public void sort(Comparator<? super SessionContainer> c) {
        synchronized (lock) {
            sortRange(c, 0, getArray().length);
        }
    }

    @SuppressWarnings("unchecked")
    void sortRange(Comparator<? super SessionContainer> c, int i, int end) {
        // assert Thread.holdsLock(lock);
        final SessionContainer[] es = getArray().clone();
        Arrays.sort(es, i, end, (Comparator<Object>) c);
        setArray(es);
    }

    /**
     * Returns a string representation of this list. The string
     * representation consists of the string representations of the list's
     * elements in the order they are returned by its iterator, enclosed in
     * square brackets ({@code "[]"}). Adjacent elements are separated by
     * the characters {@code ", "} (comma and space). Elements are
     * converted to strings as by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return Arrays.toString(getArray());
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns {@code true} if the specified object is the same object
     * as this object, or if it is also a {@link List} and the sequence
     * of elements returned by an {@linkplain List#iterator() iterator}
     * over the specified list is the same as the sequence returned by
     * an iterator over this list. The two sequences are considered to
     * be the same if they have the same length and corresponding
     * elements at the same position in the sequence are <em>equal</em>.
     * Two elements {@code e1} and {@code e2} are considered
     * <em>equal</em> if {@code Objects.equals(e1, e2)}.
     *
     * @param o the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }

        List<?> list = (List<?>) o;
        Iterator<?> it = list.iterator();
        for (Object element : getArray()) {
            if (!it.hasNext() || !Objects.equals(element, it.next())) {
                return false;
            }
        }
        return !it.hasNext();
    }

    private static int hashCodeOfRange(Object[] es, int from, int to) {
        int hashCode = 1;
        for (int i = from; i < to; i++) {
            Object x = es[i];
            hashCode = 31 * hashCode + (x == null ? 0 : x.hashCode());
        }
        return hashCode;
    }

    /**
     * Returns the hash code value for this list.
     *
     * <p>This implementation uses the definition in {@link List#hashCode}.
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        Object[] es = getArray();
        return hashCodeOfRange(es, 0, es.length);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove} method.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public Iterator<SessionContainer> iterator() {
        return new COWIterator(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     */
    @Override
    public ListIterator<SessionContainer> listIterator() {
        return new COWIterator(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public ListIterator<SessionContainer> listIterator(int index) {
        SessionContainer[] es = getArray();
        int len = es.length;
        if (index < 0 || index > len) {
            throw new IndexOutOfBoundsException(outOfBounds(index, len));
        }

        return new COWIterator(es, index);
    }

    /**
     * Returns a {@link Spliterator} over the elements in this list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#IMMUTABLE},
     * {@link Spliterator#ORDERED}, {@link Spliterator#SIZED}, and
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>The spliterator provides a snapshot of the state of the list
     * when the spliterator was constructed. No synchronization is needed while
     * operating on the spliterator.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<SessionContainer> spliterator() {
        return Spliterators.spliterator(getArray(), Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    static final class COWIterator implements ListIterator<SessionContainer> {

        /** Snapshot of the array */
        private final SessionContainer[] snapshot;
        /** Index of element to be returned by subsequent call to next. */
        private int cursor;

        COWIterator(SessionContainer[] es, int initialCursor) {
            cursor = initialCursor;
            snapshot = es;
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public SessionContainer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return snapshot[cursor++];
        }

        @Override
        @SuppressWarnings("unchecked")
        public SessionContainer previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return snapshot[--cursor];
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code remove}
         *             is not supported by this iterator.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code set}
         *             is not supported by this iterator.
         */
        @Override
        public void set(SessionContainer e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always; {@code add}
         *             is not supported by this iterator.
         */
        @Override
        public void add(SessionContainer e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super SessionContainer> action) {
            Objects.requireNonNull(action);
            final int size = snapshot.length;
            int i = cursor;
            cursor = size;
            for (; i < size; i++) {
                action.accept(snapshot[i]);
            }
        }
    }

    /**
     * Returns a view of the portion of this list between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * The returned list is backed by this list, so changes in the
     * returned list are reflected in this list.
     *
     * <p>The semantics of the list returned by this method become
     * undefined if the backing list (i.e., this list) is modified in
     * any way other than via the returned list.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public List<SessionContainer> subList(int fromIndex, int toIndex) {
        synchronized (lock) {
            SessionContainer[] es = getArray();
            int len = es.length;
            int size = toIndex - fromIndex;
            if (fromIndex < 0 || toIndex > len || size < 0) {
                throw new IndexOutOfBoundsException();
            }
            return new COWSubList(es, fromIndex, size);
        }
    }

    /**
     * Rotates this list:
     * <ul>
     * <li>Removes (& returns) the last element in this list</li>
     * <li>Adds specified new element to the beginning of this list</li>
     * <li>Rotates remaining elements by 1</li>
     * </ul>
     *
     * @param newElement The new element, which is supposed to be set to the beginning of this list
     * @return The removed last element
     * @throw IndexOutOfBoundsException If this list is empty
     */
    public SessionContainer rotate(SessionContainer newElement) {
        synchronized (lock) {
            SessionContainer[] elements = getArray();
            int len = elements.length;
            int lastIndex = len - 1;
            if (lastIndex < 0) {
                // Cannot rotate an empty list
                throw new IndexOutOfBoundsException();
            }

            @SuppressWarnings("unchecked") SessionContainer oldValue = elements[lastIndex];
            SessionContainer[] newElements = new SessionContainer[len];
            if (lastIndex > 0) {
                System.arraycopy(elements, 0, newElements, 1, lastIndex);
            }
            newElements[0] = newElement;
            setArray(newElements);
            return oldValue;
        }
    }

    /**
     * Sublist for CopyOnWriteArrayList.
     */
    private class COWSubList implements List<SessionContainer>, RandomAccess {

        private final int offset;
        private int size;
        private SessionContainer[] expectedArray;

        COWSubList(SessionContainer[] es, int offset, int size) {
            // assert Thread.holdsLock(lock);
            expectedArray = es;
            this.offset = offset;
            this.size = size;
        }

        private void checkForComodification() {
            // assert Thread.holdsLock(lock);
            if (getArray() != expectedArray) {
                throw new ConcurrentModificationException();
            }
        }

        private SessionContainer[] getArrayChecked() {
            // assert Thread.holdsLock(lock);
            SessionContainer[] a = getArray();
            if (a != expectedArray) {
                throw new ConcurrentModificationException();
            }
            return a;
        }

        private void rangeCheck(int index) {
            // assert Thread.holdsLock(lock);
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(outOfBounds(index, size));
            }
        }

        private void rangeCheckForAdd(int index) {
            // assert Thread.holdsLock(lock);
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException(outOfBounds(index, size));
            }
        }

        @Override
        public Object[] toArray() {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            return Arrays.copyOfRange(es, offset, offset + size);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            if (a.length < size) {
                return (T[]) Arrays.copyOfRange(es, offset, offset + size, a.getClass());
            } else {
                System.arraycopy(es, offset, a, 0, size);
                if (a.length > size) {
                    a[size] = null;
                }
                return a;
            }
        }

        @Override
        public int indexOf(Object o) {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            int i = indexOfRange(o, es, offset, offset + size);
            return (i == -1) ? -1 : i - offset;
        }

        @Override
        public int lastIndexOf(Object o) {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            int i = lastIndexOfRange(o, es, offset, offset + size);
            return (i == -1) ? -1 : i - offset;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            for (Object o : c) {
                if (indexOfRange(o, es, offset, offset + size) < 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public String toString() {
            return Arrays.toString(toArray());
        }

        @Override
        public int hashCode() {
            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }
            return hashCodeOfRange(es, offset, offset + size);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof List)) {
                return false;
            }
            Iterator<?> it = ((List<?>) o).iterator();

            final Object[] es;
            final int offset;
            final int size;
            synchronized (lock) {
                es = getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            for (int i = offset, end = offset + size; i < end; i++) {
                if (!it.hasNext() || !Objects.equals(es[i], it.next())) {
                    return false;
                }
            }
            return !it.hasNext();
        }

        @Override
        public SessionContainer set(int index, SessionContainer element) {
            synchronized (lock) {
                rangeCheck(index);
                checkForComodification();
                SessionContainer x = RotatableSessionContainerList.this.set(offset + index, element);
                expectedArray = getArray();
                return x;
            }
        }

        @Override
        public SessionContainer get(int index) {
            synchronized (lock) {
                rangeCheck(index);
                checkForComodification();
                return RotatableSessionContainerList.this.get(offset + index);
            }
        }

        @Override
        public int size() {
            synchronized (lock) {
                checkForComodification();
                return size;
            }
        }

        @Override
        public boolean add(SessionContainer element) {
            synchronized (lock) {
                checkForComodification();
                RotatableSessionContainerList.this.add(offset + size, element);
                expectedArray = getArray();
                size++;
            }
            return true;
        }

        @Override
        public void add(int index, SessionContainer element) {
            synchronized (lock) {
                checkForComodification();
                rangeCheckForAdd(index);
                RotatableSessionContainerList.this.add(offset + index, element);
                expectedArray = getArray();
                size++;
            }
        }

        @Override
        public boolean addAll(Collection<? extends SessionContainer> c) {
            synchronized (lock) {
                final SessionContainer[] oldArray = getArrayChecked();
                boolean modified = RotatableSessionContainerList.this.addAll(offset + size, c);
                size += (expectedArray = getArray()).length - oldArray.length;
                return modified;
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends SessionContainer> c) {
            synchronized (lock) {
                rangeCheckForAdd(index);
                final SessionContainer[] oldArray = getArrayChecked();
                boolean modified = RotatableSessionContainerList.this.addAll(offset + index, c);
                size += (expectedArray = getArray()).length - oldArray.length;
                return modified;
            }
        }

        @Override
        public void clear() {
            synchronized (lock) {
                checkForComodification();
                removeRange(offset, offset + size);
                expectedArray = getArray();
                size = 0;
            }
        }

        @Override
        public SessionContainer remove(int index) {
            synchronized (lock) {
                rangeCheck(index);
                checkForComodification();
                SessionContainer result = RotatableSessionContainerList.this.remove(offset + index);
                expectedArray = getArray();
                size--;
                return result;
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized (lock) {
                checkForComodification();
                int index = indexOf(o);
                if (index == -1) {
                    return false;
                }
                remove(index);
                return true;
            }
        }

        @Override
        public Iterator<SessionContainer> iterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<SessionContainer> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<SessionContainer> listIterator(int index) {
            synchronized (lock) {
                checkForComodification();
                rangeCheckForAdd(index);
                return new COWSubListIterator(RotatableSessionContainerList.this, index, offset, size);
            }
        }

        @Override
        public List<SessionContainer> subList(int fromIndex, int toIndex) {
            synchronized (lock) {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
                    throw new IndexOutOfBoundsException();
                }
                return new COWSubList(expectedArray, fromIndex + offset, toIndex - fromIndex);
            }
        }

        @Override
        public void forEach(Consumer<? super SessionContainer> action) {
            Objects.requireNonNull(action);
            int i, end;
            final SessionContainer[] es;
            synchronized (lock) {
                es = getArrayChecked();
                i = offset;
                end = i + size;
            }
            for (; i < end; i++) {
                action.accept(es[i]);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<SessionContainer> operator) {
            synchronized (lock) {
                checkForComodification();
                replaceAllRange(operator, offset, offset + size);
                expectedArray = getArray();
            }
        }

        @Override
        public void sort(Comparator<? super SessionContainer> c) {
            synchronized (lock) {
                checkForComodification();
                sortRange(c, offset, offset + size);
                expectedArray = getArray();
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return bulkRemove(e -> c.contains(e));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return bulkRemove(e -> !c.contains(e));
        }

        @Override
        public boolean removeIf(Predicate<? super SessionContainer> filter) {
            Objects.requireNonNull(filter);
            return bulkRemove(filter);
        }

        private boolean bulkRemove(Predicate<? super SessionContainer> filter) {
            synchronized (lock) {
                final SessionContainer[] oldArray = getArrayChecked();
                boolean modified = RotatableSessionContainerList.this.bulkRemove(filter, offset, offset + size);
                size += (expectedArray = getArray()).length - oldArray.length;
                return modified;
            }
        }

        @Override
        public Spliterator<SessionContainer> spliterator() {
            synchronized (lock) {
                return Spliterators.spliterator(getArrayChecked(), offset, offset + size, Spliterator.IMMUTABLE | Spliterator.ORDERED);
            }
        }

    }

    private static class COWSubListIterator implements ListIterator<SessionContainer> {

        private final ListIterator<SessionContainer> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<SessionContainer> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index + offset);
        }

        @Override
        public boolean hasNext() {
            return nextIndex() < size;
        }

        @Override
        public SessionContainer next() {
            if (hasNext()) {
                return it.next();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        @Override
        public SessionContainer previous() {
            if (hasPrevious()) {
                return it.previous();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        @Override
        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(SessionContainer e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(SessionContainer e) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super SessionContainer> action) {
            Objects.requireNonNull(action);
            while (hasNext()) {
                action.accept(it.next());
            }
        }
    }

    /** Initializes the lock; for use when deserializing or cloning. */
    private void resetLock() {
        @SuppressWarnings("removal") Field lockField = java.security.AccessController.doPrivileged((java.security.PrivilegedAction<Field>) () -> {
            try {
                Field f = RotatableSessionContainerList.class.getDeclaredField("lock");
                f.setAccessible(true);
                return f;
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        });
        try {
            lockField.set(this, new Object());
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

}
